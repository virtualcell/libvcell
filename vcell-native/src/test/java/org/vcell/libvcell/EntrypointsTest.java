package org.vcell.libvcell;

import cbit.util.xml.VCLoggerException;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.solver.SolverException;
import org.junit.jupiter.api.Test;

import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class EntrypointsTest {

    @Test
    public void testSbmlToFiniteVolumeInput() throws PropertyVetoException, SolverException, ExpressionException, MappingException, VCLoggerException, IOException {
        // read the contents of file TinySpacialProject_Application0.xml which is in the root of the resources folder
        String sbmlContent = getFileContentsAsString("/TinySpacialProject_Application0.xml");
        // create tempdir to store the output
        Path output_dir = Files.createTempDirectory("sbmlToFiniteVolumeInput");
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        Entrypoints.sbmlToFiniteVolumeInput(sbmlContent, output_dir.toFile());
    }

    private static String getFileContentsAsString(String filename) throws IOException {
        try (InputStream inputStream = EntrypointsTest.class.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new FileNotFoundException("file not found! " + filename);
            }
            String textContent = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            return textContent;
        }
    }
}
