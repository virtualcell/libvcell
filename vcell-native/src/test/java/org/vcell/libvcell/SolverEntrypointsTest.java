package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.math.MathException;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.SolverException;
import cbit.vcell.xml.XmlParseException;
import org.junit.jupiter.api.Test;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;
import static org.vcell.libvcell.TestUtils.*;

public class SolverEntrypointsTest {

    @Test
    public void testSbmlToFiniteVolumeInput() throws PropertyVetoException, SolverException, ExpressionException, MappingException, VCLoggerException, IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        File output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput").toFile();
        sbmlToFiniteVolumeInput(sbmlContent, output_dir);
        assertEquals(4, countFiles(output_dir));
    }

    // TODO: better exception handling by JSBML needed
    @Test
    public void testSbmlToFiniteVolumeInput_not_well_formed() throws IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml").replaceAll("<model ", "<modelXYZ ");
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        ClassCastException exc = assertThrows(ClassCastException.class, () -> sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile()));
        assertEquals(
                "class org.sbml.jsbml.xml.XMLNode cannot be cast to class org.sbml.jsbml.Annotation " +
                "(org.sbml.jsbml.xml.XMLNode and org.sbml.jsbml.Annotation are in unnamed module of loader 'app')",
                exc.getMessage());
    }

    @Test
    public void testSbmlToFiniteVolumeInput_vcml_instead() throws IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile()));
        assertEquals("expecting SBML content, not VCML", exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_field_data() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException, MathException, InterruptedException {
        String vcmlContent = getFileContentsAsString("/FieldDataDemo.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput_"+UUID.randomUUID()).toFile();
        File output_dir = new File(parent_dir, "output_dir");
        File ext_data_dir = new File(parent_dir, "test2_lsm_DEMO");
        assertEquals(0, countFiles(ext_data_dir));
        extractTgz(SolverEntrypointsTest.class.getResourceAsStream("/test2_lsm_DEMO.tgz"), parent_dir);
        listFilesInDirectory(ext_data_dir);
        assertEquals(10, countFiles(ext_data_dir));

        assertEquals(0, countFiles(output_dir));
        String simulationName = "Simulation0";
        vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir);
        listFilesInDirectory(output_dir);
        assertEquals(10, countFiles(ext_data_dir));
        assertEquals(6, countFiles(output_dir));
    }

    @Test
    public void testVcmlToFiniteVolumeInput_field_data_not_found() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException, MathException, InterruptedException {
        String vcmlContent = getFileContentsAsString("/FieldDataDemo.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput_"+UUID.randomUUID()).toFile();
        File output_dir = new File(parent_dir, "output_dir");
        File ext_data_dir = new File(parent_dir, "test2_lsm_DEMO");
        File ext_data_dir_MISSPELLED = new File(parent_dir, "test2_lsm_DEMO_MISSPELLED");
        assertEquals(0, countFiles(ext_data_dir));
        assertEquals(0, countFiles(ext_data_dir_MISSPELLED));
        extractTgz(SolverEntrypointsTest.class.getResourceAsStream("/test2_lsm_DEMO.tgz"), parent_dir);
        Files.move(ext_data_dir.toPath(), ext_data_dir_MISSPELLED.toPath()).toFile();
        assertEquals(0, countFiles(ext_data_dir));
        listFilesInDirectory(ext_data_dir_MISSPELLED);
        assertEquals(10, countFiles(ext_data_dir_MISSPELLED));

        assertEquals(10, countFiles(ext_data_dir_MISSPELLED));
        assertEquals(0, countFiles(output_dir));
        String simulationName = "Simulation0";
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir));
        assertTrue(exc.getMessage().contains("Field data directory does not exist") && exc.getMessage().contains(ext_data_dir.getName()));
        assertEquals(10, countFiles(ext_data_dir_MISSPELLED));
        assertEquals(0, countFiles(output_dir));
    }

    @Test
    public void testVcmlToFiniteVolumeInput() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException, MathException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput").toFile();
        File output_dir = new File(parent_dir, "output_dir");
        String simulationName = "Simulation0";
        vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir);
        assertEquals(4, countFiles(output_dir));
    }

    @Test
    public void testVcmlToFiniteVolumeInput_bad_simname() throws IOException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput").toFile();
        File output_dir = new File(parent_dir, "output_dir");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir));
        assertEquals("Simulation not found: wrong_sim_name", exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_not_well_formed() throws IOException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml")
                .replaceAll("<SimpleReaction ", "<SimpleReactionXYZ ");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput").toFile();
        File output_dir = new File(parent_dir, "output_dir");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        RuntimeException exc = assertThrows(RuntimeException.class, () -> vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir));
        assertEquals(
                "source document is not well-formed\n" +
                        "Error on line 25: The element type \"SimpleReactionXYZ\" must be terminated by the matching end-tag \"</SimpleReactionXYZ>\".",
                exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_sbml_instead() throws IOException {
        String vcmlContent_actually_sbml = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput").toFile();
        File output_dir = new File(parent_dir, "output_dir");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToFiniteVolumeInput(vcmlContent_actually_sbml, simulationName, parent_dir, output_dir));
        assertEquals("expecting VCML content, not SBML", exc.getMessage());
    }

}
