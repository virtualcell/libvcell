package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.SolverException;
import cbit.vcell.xml.XmlParseException;
import org.junit.jupiter.api.Test;

import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;

public class EntrypointsTest {

    @Test
    public void testSbmlToFiniteVolumeInput() throws PropertyVetoException, SolverException, ExpressionException, MappingException, VCLoggerException, IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile());
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
    public void testVcmlToFiniteVolumeInput() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        Path output_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput");
        String simulationName = "Simulation0";
        vcmlToFiniteVolumeInput(vcmlContent, simulationName, output_dir.toFile());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_bad_simname() throws IOException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        Path output_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToFiniteVolumeInput(vcmlContent, simulationName, output_dir.toFile()));
        assertEquals("Simulation not found: wrong_sim_name", exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_not_well_formed() throws IOException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml")
                .replaceAll("<SimpleReaction ", "<SimpleReactionXYZ ");
        Path output_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        RuntimeException exc = assertThrows(RuntimeException.class, () -> vcmlToFiniteVolumeInput(vcmlContent, simulationName, output_dir.toFile()));
        assertEquals(
                "source document is not well-formed\n" +
                        "Error on line 25: The element type \"SimpleReactionXYZ\" must be terminated by the matching end-tag \"</SimpleReactionXYZ>\".",
                exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_sbml_instead() throws IOException {
        String vcmlContent_actually_sbml = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        Path output_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput");
        String simulationName = "wrong_sim_name";
        // expect to throw an IllegalArgumentException
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToFiniteVolumeInput(vcmlContent_actually_sbml, simulationName, output_dir.toFile()));
        assertEquals("expecting VCML content, not SBML", exc.getMessage());
    }

    private static String getFileContentsAsString(String filename) throws IOException {
        try (InputStream inputStream = EntrypointsTest.class.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new FileNotFoundException("file not found! " + filename);
            }
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }
    }
}
