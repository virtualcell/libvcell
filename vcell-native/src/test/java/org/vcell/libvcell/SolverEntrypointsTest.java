package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.math.MathException;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.SolverException;
import cbit.vcell.xml.XmlParseException;
import org.junit.jupiter.api.Test;
import org.vcell.sbml.vcell.SBMLImportException;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.sbmlToOdeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToMovingBoundaryInput;
import static org.vcell.libvcell.SolverUtils.vcmlToOdeInput;
import static org.vcell.libvcell.TestUtils.*;

public class SolverEntrypointsTest {

    @Test
    public void testSbmlToFiniteVolumeInput() throws PropertyVetoException, SolverException, ExpressionException, MappingException, VCLoggerException, IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        File output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput").toFile();
        sbmlToFiniteVolumeInput(sbmlContent, output_dir);
        assertEquals(4, countFiles(output_dir));
    }

    @Test
    public void testSbmlToFiniteVolumeInput_not_well_formed() throws IOException {
        // vcell-core now reads the SBML stream itself and reports malformed XML as an SBMLImportException
        // (it used to surface as a ClassCastException from inside JSBML)
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml").replaceAll("<model ", "<modelXYZ ");
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        SBMLImportException exc = assertThrows(SBMLImportException.class, () -> sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile()));
        assertEquals("Unable to read SBML stream", exc.getMessage());
    }

    @Test
    public void testSbmlToFiniteVolumeInput_vcml_instead() throws IOException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile()));
        assertEquals("expecting SBML content, not VCML", exc.getMessage());
    }

    @Test
    public void testVcmlToFiniteVolumeInput_field_data_already_sampled() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException, MathException, InterruptedException {
        String vcmlContent = getFileContentsAsString("/FieldDataDemo.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToFiniteVolumeInput_"+UUID.randomUUID()).toFile();
        File output_dir = new File(parent_dir, "output_dir");
        assertEquals(0, countFiles(output_dir));
        // prepopulate the output_dir with the resampled field data files, should use these instead.
        extractTgz(SolverEntrypointsTest.class.getResourceAsStream("/test2_lsm_DEMO_resampled.tgz"), output_dir);
        listFilesInDirectory(output_dir);
        assertEquals(2, countFiles(output_dir));

        String simulationName = "Simulation0";
        vcmlToFiniteVolumeInput(vcmlContent, simulationName, parent_dir, output_dir);
        listFilesInDirectory(output_dir);
        assertEquals(6, countFiles(output_dir));
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
    public void testVcmlToFiniteVolumeInput_field_data_not_found() throws IOException {
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

    @Test
    public void testVcmlToMovingBoundaryInput() throws SolverException, ExpressionException, MappingException, IOException, XmlParseException, MathException {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToMovingBoundaryInput_" + UUID.randomUUID()).toFile();
        assertEquals(0, countFiles(output_dir));

        String simulationName = "Simulation33";  // a Moving Boundary simulation in the "2D kinematics analytic" application
        vcmlToMovingBoundaryInput(vcmlContent, simulationName, output_dir);

        // The solver input is a MovingBoundarySetup XML (consumed by vcell-mbsolver's from_xml).
        File[] files = output_dir.listFiles();
        assertNotNull(files);
        File mbInputFile = null;
        for (File f : files) {
            if (f.getName().endsWith("mb.xml")) {
                mbInputFile = f;
                break;
            }
        }
        assertNotNull(mbInputFile, "expected a *mb.xml MovingBoundarySetup input file to be written");
        String mbXml = new String(Files.readAllBytes(mbInputFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(mbXml.contains("<MovingBoundarySetup"), mbXml);
    }

    @Test
    public void testVcmlToMovingBoundaryInput_bad_simname() throws IOException {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToMovingBoundaryInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToMovingBoundaryInput(vcmlContent, "wrong_sim_name", output_dir));
        assertEquals("Simulation not found: wrong_sim_name", exc.getMessage());
    }

    @Test
    public void testVcmlToMovingBoundaryInput_wrong_solver() throws IOException {
        // Simulation0 in TinySpatialProject is a Finite Volume simulation, not Moving Boundary
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File output_dir = Files.createTempDirectory("vcmlToMovingBoundaryInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToMovingBoundaryInput(vcmlContent, "Simulation0", output_dir));
        assertTrue(exc.getMessage().contains("expected the Moving Boundary solver"), exc.getMessage());
    }

    @Test
    public void testVcmlToMovingBoundaryInput_sbml_instead() throws IOException {
        String vcmlContent_actually_sbml = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        File output_dir = Files.createTempDirectory("vcmlToMovingBoundaryInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToMovingBoundaryInput(vcmlContent_actually_sbml, "anything", output_dir));
        assertEquals("expecting VCML content, not SBML", exc.getMessage());
    }


    // -- Sundials ODE input (solved downstream by pyvcell-odesolver) -- //

    private static String firstLine(File f) throws IOException {
        return Files.readAllLines(f.toPath()).get(0);
    }

    @Test
    public void testVcmlToOdeInput_cvode() throws Exception {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput_" + UUID.randomUUID()).toFile();
        SolverUtils.OdeInput odeInput = vcmlToOdeInput(vcmlContent, "Copy of CVODE", output_dir);
        File input = odeInput.inputFile();
        assertFalse(odeInput.usedStoredMath());
        assertTrue(input.getName().endsWith(".cvodeInput"), input.getName());
        assertEquals(output_dir.getCanonicalFile(), input.getCanonicalFile().getParentFile());
        assertEquals("SOLVER CVODE", firstLine(input));
        assertTrue(Files.readString(input.toPath()).contains("NUM_EQUATIONS"));
    }

    @Test
    public void testVcmlToOdeInput_ida() throws Exception {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput_" + UUID.randomUUID()).toFile();
        SolverUtils.OdeInput odeInput = vcmlToOdeInput(vcmlContent, "Copy of IDA", output_dir);
        File input = odeInput.inputFile();
        assertTrue(input.getName().endsWith(".idaInput"), input.getName());
        assertEquals("SOLVER IDA", firstLine(input));
    }

    @Test
    public void testVcmlToOdeInput_combined_resolves_to_cvode_or_ida() throws Exception {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput_" + UUID.randomUUID()).toFile();
        SolverUtils.OdeInput odeInput = vcmlToOdeInput(vcmlContent, "Copy of combined ida/cvode", output_dir);
        File input = odeInput.inputFile();
        String first = firstLine(input);
        assertTrue(first.equals("SOLVER CVODE") || first.equals("SOLVER IDA"), first);
    }

    @Test
    public void testVcmlToOdeInput_wrong_solver() throws IOException {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToOdeInput(vcmlContent, "Copy of forward euler", output_dir));
        assertTrue(exc.getMessage().contains("expected CVODE, IDA or the Combined IDA/CVODE solver"), exc.getMessage());
    }

    @Test
    public void testVcmlToOdeInput_spatial() throws IOException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToOdeInput(vcmlContent, "Simulation0", output_dir));
        assertTrue(exc.getMessage().contains("is spatial"), exc.getMessage());
    }

    @Test
    public void testVcmlToOdeInput_bad_simname() throws IOException {
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput").toFile();
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> vcmlToOdeInput(vcmlContent, "wrong_sim_name", output_dir));
        assertEquals("Simulation not found: wrong_sim_name", exc.getMessage());
    }

    @Test
    public void testSbmlToOdeInput() throws Exception {
        // BIOMD0000000012 (Elowitz 2000, the repressilator; BioModels, CC0): non-spatial, no fast systems
        String sbmlContent = getFileContentsAsString("/BIOMD0000000012_url.xml");
        File output_dir = Files.createTempDirectory("sbmlToOdeInput_" + UUID.randomUUID()).toFile();
        File input = sbmlToOdeInput(sbmlContent, 100.0, 0.5, output_dir);
        assertTrue(input.getName().endsWith(".cvodeInput"), input.getName());
        String text = Files.readString(input.toPath());
        assertTrue(text.startsWith("SOLVER CVODE"), text.substring(0, 40));
        assertTrue(text.contains("ENDING_TIME 100.0"), text);
        assertTrue(text.contains("OUTPUT_TIME_STEP 0.5"), text);
    }

    @Test
    public void testSbmlToOdeInput_bad_arguments() throws IOException {
        String sbmlContent = getFileContentsAsString("/BIOMD0000000012_url.xml");
        File output_dir = Files.createTempDirectory("sbmlToOdeInput").toFile();
        assertThrows(IllegalArgumentException.class, () -> sbmlToOdeInput(sbmlContent, 0.0, 0.5, output_dir));
        String vcmlContent = getFileContentsAsString("/Solver_Suite_6_2.vcml");
        IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> sbmlToOdeInput(vcmlContent, 10.0, 1.0, output_dir));
        assertEquals("expecting SBML content, not VCML", exc.getMessage());
    }

    @Test
    public void testVcmlToOdeInput_rule_based_uses_the_stored_math() throws Exception {
        // public BioModel 100992568 ("CompartmentTransport"): one reaction rule, which VCell would expand into a
        // reaction network with BioNetGen (not available in libvcell), so the document's stored math is used
        String vcmlContent = getFileContentsAsString("/public_biomodel_100992568_rule_based.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput_" + UUID.randomUUID()).toFile();
        SolverUtils.OdeInput odeInput = vcmlToOdeInput(vcmlContent, "Simulation0", output_dir);
        File input = odeInput.inputFile();
        assertTrue(odeInput.usedStoredMath());  // no vcell.installDir here, so BioNetGen cannot run
        assertTrue(odeInput.storedMathReason().startsWith("BioNetGen could not regenerate"), odeInput.storedMathReason());
        String first = firstLine(input);
        assertTrue(first.equals("SOLVER CVODE") || first.equals("SOLVER IDA"), first);
        assertTrue(Files.readString(input.toPath()).contains("NUM_EQUATIONS"));
    }

    @Test
    public void testVcmlToOdeInput_rule_based_regenerated_with_the_bundled_bionetgen() throws Exception {
        // the BioNetGen the Python package bundles (libvcell/vcell_install/bionetgen, run with perl)
        File installDir = new File("../libvcell/vcell_install").getCanonicalFile();
        assertTrue(new File(installDir, "bionetgen/BNG2.pl").isFile(), installDir.toString());
        String vcmlContent = getFileContentsAsString("/public_biomodel_100992568_rule_based.vcml");
        File output_dir = Files.createTempDirectory("vcmlToOdeInput_" + UUID.randomUUID()).toFile();
        System.setProperty("vcell.installDir", installDir.getPath());
        try {
            SolverUtils.OdeInput odeInput = vcmlToOdeInput(vcmlContent, "Simulation0", output_dir);
            assertFalse(odeInput.usedStoredMath(), odeInput.storedMathReason());
            assertTrue(Files.readString(odeInput.inputFile().toPath()).contains("NUM_EQUATIONS"));
        } finally {
            System.clearProperty("vcell.installDir");
        }
    }

}
