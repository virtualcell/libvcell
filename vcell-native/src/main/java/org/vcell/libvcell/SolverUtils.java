package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.field.FieldDataIdentifierSpec;
import cbit.vcell.field.FieldFunctionArguments;
import cbit.vcell.field.FieldUtilities;
import cbit.vcell.geometry.GeometrySpec;
import cbit.vcell.mapping.MappingException;
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


    public static void sbmlToFiniteVolumeInput(String sbml_content, File outputDir) throws MappingException, PropertyVetoException, SolverException, ExpressionException, VCLoggerException {
        GeometrySpec.avoidAWTImageCreation = true;
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
