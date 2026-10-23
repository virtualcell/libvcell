package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.field.FieldDataIdentifierSpec;
import cbit.vcell.field.FieldFunctionArguments;
import cbit.vcell.field.FieldUtilities;
import cbit.vcell.geometry.GeometrySpec;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.NetworkTransformer;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.math.MathException;
import cbit.vcell.messaging.server.SimulationTask;
import cbit.vcell.mongodb.VCMongoMessage;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.simdata.SimulationData;
import cbit.vcell.solver.*;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vcell.libvcell.solvers.LocalFVSolverStandalone;
import org.vcell.libvcell.solvers.LocalMovingBoundarySolverStandalone;
import org.vcell.libvcell.solvers.LocalOdeSolverStandalone;
import org.vcell.sbml.FiniteVolumeRunUtil;
import org.vcell.sbml.vcell.SBMLExporter;
import org.vcell.sbml.vcell.SBMLImporter;
import org.vcell.util.document.ExternalDataIdentifier;
import org.vcell.util.document.KeyValue;
import org.vcell.util.document.User;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class SolverUtils {
    private static final Logger logger = LogManager.getLogger(Entrypoints.class);

    public static void vcmlToFiniteVolumeInput(String vcml_content, String simulation_name, File parentDir, File outputDir) throws XmlParseException, MappingException, SolverException, ExpressionException, MathException {
        GeometrySpec.avoidAWTImageCreation = true;
	    XmlHelper.cloneUsingXML = true;
        VCMongoMessage.enabled = false;

        if (vcml_content.substring(0, 300).contains("<sbml xmlns=\"http://www.sbml.org/sbml")) {
            throw new IllegalArgumentException("expecting VCML content, not SBML");
        }
        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);
        Simulation sim = bioModel.getSimulation(simulation_name);
        if (sim == null) {
            throw new IllegalArgumentException("Simulation not found: " + simulation_name);
        }
        FieldDataIdentifierSpec[] fdiSpecs = getFieldDataIdentifierSpecs(sim, outputDir, parentDir);

        TempSimulation tempSimulation = new TempSimulation(sim, false);
        tempSimulation.setSimulationOwner(sim.getSimulationOwner());
        SimulationJob tempSimulationJob = new SimulationJob(tempSimulation, 0, fdiSpecs);

        renameExistingFieldDataFiles(tempSimulation.getKey(), tempSimulationJob.getJobIndex(), outputDir);

        SimulationTask simTask = new SimulationTask(tempSimulationJob, 0);
        LocalFVSolverStandalone solver = new LocalFVSolverStandalone(simTask, outputDir);
        solver.initialize();
    }

    public static void vcmlToMovingBoundaryInput(String vcml_content, String simulation_name, File outputDir) throws XmlParseException, MappingException, SolverException, ExpressionException, MathException {
        GeometrySpec.avoidAWTImageCreation = true;
	    XmlHelper.cloneUsingXML = true;
        VCMongoMessage.enabled = false;

        if (vcml_content.substring(0, 300).contains("<sbml xmlns=\"http://www.sbml.org/sbml")) {
            throw new IllegalArgumentException("expecting VCML content, not SBML");
        }
        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);
        Simulation sim = bioModel.getSimulation(simulation_name);
        if (sim == null) {
            throw new IllegalArgumentException("Simulation not found: " + simulation_name);
        }
        // Require the simulation to already target the Moving Boundary solver - the input file is
        // generated from MovingBoundary-specific math (spatial processes / front velocities), so this
        // entry point is meaningless for simulations configured for other solvers.
        SolverDescription solverDescription = sim.getSolverTaskDescription().getSolverDescription();
        if (solverDescription != SolverDescription.MovingBoundary) {
            String actual = (solverDescription == null) ? "none" : solverDescription.getDisplayLabel();
            throw new IllegalArgumentException("Simulation '" + simulation_name + "' is configured for the '" + actual + "' solver, expected the Moving Boundary solver");
        }

        TempSimulation tempSimulation = new TempSimulation(sim, false);
        tempSimulation.setSimulationOwner(sim.getSimulationOwner());
        SimulationJob tempSimulationJob = new SimulationJob(tempSimulation, 0, null);

        SimulationTask simTask = new SimulationTask(tempSimulationJob, 0);
        LocalMovingBoundarySolverStandalone solver = new LocalMovingBoundarySolverStandalone(simTask, outputDir);
        solver.writeInputFiles();
    }

    private static void renameExistingFieldDataFiles(KeyValue tempSimKey, int jobId, File outputDir) {
        File[] files = outputDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("SimID_SIMULATIONKEY_JOBINDEX_")) {
                    String newName = file.getName().replace("SIMULATIONKEY",tempSimKey.toString()).replace("JOBINDEX",String.valueOf(jobId));
                    File newFile = new File(outputDir, newName);
                    if (!file.renameTo(newFile)){
                        throw new RuntimeException("Could not rename " + file.getName() + " to " + newFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static FieldDataIdentifierSpec[] getFieldDataIdentifierSpecs(Simulation sim, File outputDir, File parentDir) throws MathException, ExpressionException {
        FieldDataIdentifierSpec[] fdiSpecs = null;
        FieldFunctionArguments[] fieldFuncArgs =  FieldUtilities.getFieldFunctionArguments(sim.getMathDescription());
        if (fieldFuncArgs != null) {
            List<FieldDataIdentifierSpec> fdiSpecList = new ArrayList<>();
            for (FieldFunctionArguments fieldFuncArg : fieldFuncArgs) {
                if (fieldFuncArg != null) {
                    String name = fieldFuncArg.getFieldName();
                    //
                    // First, check if the resampled field data files are already present (e.g. if pyvcell wrote the files directly from image data)
                    //
                    KeyValue random_key = new KeyValue("123456789");
                    ExternalDataIdentifier fakeExtDataId = new ExternalDataIdentifier(random_key, User.tempUser, name);
                    String fieldDataFileName = SimulationData.createCanonicalResampleFileName(fakeExtDataId, fieldFuncArg);
                    fieldDataFileName = fieldDataFileName.replace("SimID_" + random_key + "_0_", "SimID_SIMULATIONKEY_JOBINDEX_");
                    File preexistingFieldDataFile = new File(outputDir, fieldDataFileName);
                    if (preexistingFieldDataFile.exists()) {
                        fdiSpecList.add(new FieldDataIdentifierSpec(fieldFuncArg, fakeExtDataId));
                        continue;
                    }

                    //
                    // If not, check if the field data directory exists as a subdirectory of the parentDir - holding simulation results.
                    //
                    File fieldDataDir = new File(parentDir, name);
                    if (!fieldDataDir.exists()) {
                        throw new IllegalArgumentException("Field data directory does not exist: " + fieldDataDir.getAbsolutePath());
                    }
                    // search fieldDataDir for files with name pattern SimID_<key>_* and extract the key
                    KeyValue key = null;
                    for (File f: fieldDataDir.listFiles()) {
                        String[] filename_parts = f.getName().split("_");
                        if (filename_parts.length < 3) {
                            continue;
                        }
                        if (filename_parts[0].equals("SimID")) {
                            key = new KeyValue(filename_parts[1]);
                            break;
                        }
                    }
                    if (key == null) {
                        throw new IllegalArgumentException("Field data directory does not contain a file with key: " + name);
                    }
                    ExternalDataIdentifier extDataId = new ExternalDataIdentifier(key, User.tempUser, name);
                    fdiSpecList.add(new FieldDataIdentifierSpec(fieldFuncArg, extDataId));
                }
            }
            fdiSpecs = fdiSpecList.toArray(new FieldDataIdentifierSpec[fdiSpecList.size()]);
        }
        return fdiSpecs;
    }


    /**
     * Write the Sundials ODE solver input (CVODE or IDA) for a non-spatial VCML simulation; solved
     * downstream by pyvcell-odesolver. The simulation must be configured for CVODE, IDA or the Combined
     * IDA/CVODE solver; Combined resolves as VCell's CombinedSundialsSolver does (IDA when the math has
     * fast systems, CVODE otherwise).
     *
     * @return the input file written ({@code <basename>.cvodeInput} or {@code <basename>.idaInput})
     */
    public static File vcmlToOdeInput(String vcml_content, String simulation_name, File outputDir) throws XmlParseException, MappingException, SolverException, ExpressionException, MathException {
        GeometrySpec.avoidAWTImageCreation = true;
	    XmlHelper.cloneUsingXML = true;
        VCMongoMessage.enabled = false;

        if (vcml_content.substring(0, 300).contains("<sbml xmlns=\"http://www.sbml.org/sbml")) {
            throw new IllegalArgumentException("expecting VCML content, not SBML");
        }
        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        Simulation sim = bioModel.getSimulation(simulation_name);
        if (sim == null) {
            throw new IllegalArgumentException("Simulation not found: " + simulation_name);
        }
        refreshSimulationMath(bioModel, sim);
        return writeOdeInput(sim, simulation_name, outputDir);
    }

    /**
     * Regenerate the math of the simulation's own application (not every application in the document, so another
     * application VCell cannot generate math for does not get in the way).
     *
     * <p>A rule-based application (reaction rules, expanded into a reaction network by BioNetGen) is the
     * exception: network generation runs BioNetGen's BNG2.pl from a VCell installation, which libvcell does not
     * have. For it the math stored in the document is used, which is the network VCell generated when the model was
     * saved, and what the VCell server runs for a saved simulation. A document without stored math for it cannot be
     * converted.
     */
    static void refreshSimulationMath(BioModel bioModel, Simulation sim) throws MappingException {
        if (!(sim.getSimulationOwner() instanceof SimulationContext simContext)) {
            bioModel.updateAll(false);  // not an application's simulation (a MathModel's): as before
            return;
        }
        bioModel.refreshDependencies();
        if (simContext.createNewTransformer() instanceof NetworkTransformer) {
            if (sim.getMathDescription() == null) {
                throw new IllegalArgumentException("Simulation '" + sim.getName() + "' is in rule-based application '"
                        + simContext.getName() + "', whose reaction network needs BioNetGen to generate, which libvcell"
                        + " does not include; and the document stores no math for it to use instead");
            }
            logger.warn("Application '" + simContext.getName() + "' is rule-based: using its stored math (BioNetGen,"
                    + " which would regenerate its reaction network, is not available in libvcell)");
            return;
        }
        simContext.updateAll(false);
    }

    /**
     * Write the Sundials ODE solver input for a non-spatial SBML model: its first application, simulated
     * from 0 to {@code duration} with output every {@code output_time_step}, on the Combined IDA/CVODE
     * solver (IDA when the math has fast systems, CVODE otherwise).
     *
     * @return the input file written ({@code <basename>.cvodeInput} or {@code <basename>.idaInput})
     */
    public static File sbmlToOdeInput(String sbml_content, double duration, double output_time_step, File outputDir) throws MappingException, PropertyVetoException, SolverException, ExpressionException, VCLoggerException, MathException {
        GeometrySpec.avoidAWTImageCreation = true;
	    XmlHelper.cloneUsingXML = true;
        VCMongoMessage.enabled = false;

        if (sbml_content.substring(0, 300).contains("<vcml xmlns=\"http://sourceforge.net/projects/vcell")) {
            throw new IllegalArgumentException("expecting SBML content, not VCML");
        }
        if (!(duration > 0.0) || !(output_time_step > 0.0)) {
            throw new IllegalArgumentException("duration and output_time_step must be positive, got " + duration + " and " + output_time_step);
        }
        SBMLExporter.MemoryVCLogger vcl = new SBMLExporter.MemoryVCLogger();
        InputStream is = new ByteArrayInputStream(sbml_content.getBytes());
        SBMLImporter importer = new SBMLImporter(is, vcl, true);
        BioModel bioModel = importer.getBioModel();
        bioModel.updateAll(false);

        SimulationContext simContext = bioModel.getSimulationContext(0);
        Simulation sim = new Simulation(simContext.getMathDescription(), simContext);
        sim.getSolverTaskDescription().setSolverDescription(SolverDescription.CombinedSundials);
        sim.getSolverTaskDescription().setTimeBounds(new TimeBounds(0.0, duration));
        sim.getSolverTaskDescription().setOutputTimeSpec(new UniformOutputTimeSpec(output_time_step));
        return writeOdeInput(sim, "the SBML model's simulation", outputDir);
    }

    private static File writeOdeInput(Simulation sim, String simulation_label, File outputDir) throws SolverException {
        if (sim.isSpatial()) {
            throw new IllegalArgumentException("Simulation '" + simulation_label + "' is spatial: use the finite volume input instead");
        }
        SolverDescription solverDescription = sim.getSolverTaskDescription().getSolverDescription();
        boolean useIDA;
        if (solverDescription == SolverDescription.CVODE) {
            useIDA = false;
        } else if (solverDescription == SolverDescription.IDA) {
            useIDA = true;
        } else if (solverDescription == SolverDescription.CombinedSundials) {
            useIDA = sim.getMathDescription().hasFastSystems();
        } else {
            String actual = (solverDescription == null) ? "none" : solverDescription.getDisplayLabel();
            throw new IllegalArgumentException("Simulation '" + simulation_label + "' is configured for the '" + actual + "' solver, expected CVODE, IDA or the Combined IDA/CVODE solver");
        }

        TempSimulation tempSimulation = new TempSimulation(sim, false);
        tempSimulation.setSimulationOwner(sim.getSimulationOwner());
        SimulationJob tempSimulationJob = new SimulationJob(tempSimulation, 0, null);

        SimulationTask simTask = new SimulationTask(tempSimulationJob, 0);
        LocalOdeSolverStandalone solver = new LocalOdeSolverStandalone(simTask, outputDir, useIDA);
        return solver.writeInputFiles();
    }

    public static void sbmlToFiniteVolumeInput(String sbml_content, File outputDir) throws MappingException, PropertyVetoException, SolverException, ExpressionException, VCLoggerException {
        GeometrySpec.avoidAWTImageCreation = true;
	    XmlHelper.cloneUsingXML = true;
        VCMongoMessage.enabled = false;

        SBMLExporter.MemoryVCLogger vcl = new SBMLExporter.MemoryVCLogger();
        boolean bValidateSBML = true;
        // input stream from sbml_content String
        if (sbml_content.substring(0, 300).contains("<vcml xmlns=\"http://sourceforge.net/projects/vcell")) {
            throw new IllegalArgumentException("expecting SBML content, not VCML");
        }
        InputStream is = new ByteArrayInputStream(sbml_content.getBytes());
        SBMLImporter importer = new SBMLImporter(is, vcl, bValidateSBML);
        BioModel bioModel = importer.getBioModel();
        bioModel.updateAll(false);

        final double duration = 5.0;  // endpoint arg
        final double time_step = 0.1;  // endpoint arg
        //final ISize meshSize = new ISize(10, 10, 10);  // future endpoint arg
        SimulationContext simContext = bioModel.getSimulationContext(0);
        Simulation sim = new Simulation(simContext.getMathDescription(), simContext);
        sim.getSolverTaskDescription().setTimeBounds(new TimeBounds(0.0, duration));
        sim.getSolverTaskDescription().setOutputTimeSpec(new UniformOutputTimeSpec(time_step));

        FiniteVolumeRunUtil.writeInputFilesOnly(outputDir, sim);
    }
}
