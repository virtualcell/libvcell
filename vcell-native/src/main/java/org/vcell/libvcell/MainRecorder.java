package org.vcell.libvcell;

import cbit.vcell.resource.PropertyLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;

import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;
import static org.vcell.libvcell.ModelUtils.sbml_to_vcml;
import static org.vcell.libvcell.ModelUtils.vcml_to_sbml;
import static org.vcell.libvcell.ModelUtils.vcml_to_vcml;

public class MainRecorder {
    private static final Logger logger = LogManager.getLogger(MainRecorder.class);

    public static void main(String[] args) {
        try {
            if (args.length != 5) {
                System.out.println("Usage: java -cp <classpath> org.vcell.libvcell.MainRecorder <sbml_file> <vcml_file> <vcml_sim_name> <vcml_app_name> <output_dir>");
                return;
            }
            File sbml_file = new File(args[0]);
            File vcml_file = new File(args[1]);
            String vcml_sim_name = args[2];
            String vcml_app_name = args[3];
            File output_dir = new File(args[4]);
            File parent_dir = output_dir.getParentFile();
            logger.info("Logger logging");
            PropertyLoader.setProperty(PropertyLoader.vcellServerIDProperty, "none");
            PropertyLoader.setProperty(PropertyLoader.mongodbDatabase, "none");

            // exercise the sbmlToFiniteVolumeInput and vcmlToFiniteVolumeInput methods
            try (FileInputStream f_sbml = new FileInputStream(sbml_file)) {
                byte[] data = f_sbml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + sbml_file.getAbsolutePath());
                String sbml_str = new String(data);
                sbmlToFiniteVolumeInput(sbml_str, output_dir);
            }
            try (FileInputStream f_vcml = new FileInputStream(vcml_file)) {
                byte[] data = f_vcml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + vcml_file.getAbsolutePath());
                String vcml_str = new String(data);
                vcmlToFiniteVolumeInput(vcml_str, vcml_sim_name, parent_dir, output_dir);
            }

            // exercise the sbml_to_vcml and vcml_to_sbml methods
            try (FileInputStream f_sbml = new FileInputStream(sbml_file)) {
                byte[] data = f_sbml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + sbml_file.getAbsolutePath());
                String sbml_str = new String(data);

                // create a temporary file for the VCML output
                File temp_vcml_file = new File(output_dir, "temp.vcml");
                boolean validateSBML = true;
                sbml_to_vcml(sbml_str, temp_vcml_file.toPath(), validateSBML);
                // remove temporary file
                if (temp_vcml_file.exists()) {
                    boolean deleted = temp_vcml_file.delete();
                    if (!deleted) {
                        logger.warn("Failed to delete temporary VCML file: " + temp_vcml_file.getAbsolutePath());
                    }
                }
            }
            try (FileInputStream f_vcml = new FileInputStream(vcml_file)) {
                byte[] data = f_vcml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + vcml_file.getAbsolutePath());
                String vcml_str = new String(data);

                // create a temporary file for the SBML output
                File temp_sbml_file = new File(output_dir, "temp.vcml");
                boolean validateSBML = true;
                vcml_to_sbml(vcml_str, vcml_app_name, temp_sbml_file.toPath(), validateSBML);
                // remove temporary file
                if (temp_sbml_file.exists()) {
                    boolean deleted = temp_sbml_file.delete();
                    if (!deleted) {
                        logger.warn("Failed to delete temporary SBML file: " + temp_sbml_file.getAbsolutePath());
                    }
                }

            }

            try (FileInputStream f_vcml = new FileInputStream(vcml_file)) {
                byte[] data = f_vcml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + vcml_file.getAbsolutePath());
                String vcml_str = new String(data);

                // create a temporary file for the VCML output
                File temp_vcml_file = new File(output_dir, "temp.vcml");
                vcml_to_vcml(vcml_str, temp_vcml_file.toPath());
                // remove temporary file
                if (temp_vcml_file.exists()) {
                    boolean deleted = temp_vcml_file.delete();
                    if (!deleted) {
                        logger.warn("Failed to delete temporary VCML file: " + temp_vcml_file.getAbsolutePath());
                    }
                }

            }

            // use reflection to load jsbml classes and call their default constructors
            Class.forName("org.sbml.jsbml.AlgebraicRule").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Annotation").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.AssignmentRule").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Compartment").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.CompartmentType").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Constraint").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Delay").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Event").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.EventAssignment").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.FunctionDefinition").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.InitialAssignment").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.KineticLaw").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.ListOf").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.LocalParameter").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Model").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.ModifierSpeciesReference").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Parameter").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Priority").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.RateRule").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Reaction").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Species").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.SpeciesReference").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.SpeciesType").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.StoichiometryMath").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Trigger").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.Unit").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.UnitDefinition").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.ArraysParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.CompParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.DistribParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.DynParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.FBCParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.GroupsParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.L3LayoutParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.LayoutParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.MathMLStaxParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.MultiParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.QualParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.RenderParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.ReqParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.SBMLCoreParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.SBMLLevel1Rule").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.SBMLRDFAnnotationParser").getDeclaredConstructor().newInstance();
            Class.forName("org.sbml.jsbml.xml.parsers.SpatialParser").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.UncertMLXMLNodeReader").getDeclaredConstructor().newInstance();
//            Class.forName("org.sbml.jsbml.xml.parsers.XMLNodeReader").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("Error processing spatial model", e);
        }
    }
}
