package org.vcell.libvcell;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class TestUtils {
    public static String getFileContentsAsString(String filename) throws IOException {
        try (InputStream inputStream = ModelEntrypointsTest.class.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new FileNotFoundException("file not found! " + filename);
            }
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }
    }

    public static byte[] getFileContentsAsBytes(String filename) throws IOException {
        try (InputStream inputStream = SolverEntrypointsTest.class.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new FileNotFoundException("file not found! " + filename);
            }
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, length);
                }
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    public static int countFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        return Objects.requireNonNull(dir.listFiles()).length;
    }

    public static void listFilesInDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                System.out.println(file.getAbsolutePath());
            }
        }
    }

    public static void extractTgz(InputStream tgzFileStream, File outputDir) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(tgzFileStream);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    File parent = outputFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (OutputStream os = Files.newOutputStream(outputFile.toPath())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }


}
