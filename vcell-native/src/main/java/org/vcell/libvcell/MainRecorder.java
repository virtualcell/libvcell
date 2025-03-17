package org.vcell.libvcell;

import cbit.vcell.resource.PropertyLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;

import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;


public class MainRecorder {
    private static final Logger logger = LogManager.getLogger(MainRecorder.class);

    public static void main(String[] args) {
        try {
            File sbml_file = new File(args[0]);
            File vcml_file = new File(args[1]);
            String vcml_sim_name = args[2];
            File output_dir = new File(args[3]);
            logger.info("Logger logging");
            PropertyLoader.setProperty(PropertyLoader.vcellServerIDProperty, "none");
            PropertyLoader.setProperty(PropertyLoader.mongodbDatabase, "none");


            try (FileInputStream f_sbml = new FileInputStream(sbml_file)) {
                byte[] data = f_sbml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + sbml_file.getAbsolutePath());
                String sbml_str = new String(data);
                sbmlToFiniteVolumeInput(sbml_str, output_dir);
                //vcmlToFiniteVolumeInput(vcml_str, sim_name, new File(args[1]));
            }
            // read sbml_file and create a string object
            try (FileInputStream f_vcml = new FileInputStream(vcml_file)) {
                byte[] data = f_vcml.readAllBytes();
                logger.info("Read " + data.length + " bytes from " + vcml_file.getAbsolutePath());
                String vcml_str = new String(data);
                vcmlToFiniteVolumeInput(vcml_str, vcml_sim_name, output_dir);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("Error processing spatial model", e);
        }
    }
}
