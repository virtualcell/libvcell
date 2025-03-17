package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.geometry.GeometrySpec;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.mongodb.VCMongoMessage;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.Simulation;
import cbit.vcell.solver.SolverException;
import cbit.vcell.solver.TimeBounds;
import cbit.vcell.solver.UniformOutputTimeSpec;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vcell.sbml.FiniteVolumeRunUtil;
import org.vcell.sbml.vcell.SBMLExporter;
import org.vcell.sbml.vcell.SBMLImporter;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;


public class SolverUtils {
    private static final Logger logger = LogManager.getLogger(Entrypoints.class);

    public static void vcmlToFiniteVolumeInput(String vcml_content, String simulation_name, File outputDir) throws XmlParseException, MappingException, SolverException, ExpressionException {
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
        FiniteVolumeRunUtil.writeInputFilesOnly(outputDir, sim);
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
