import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarFile;

/**
 * Writes vcell-native/src/main/native-image/jsbml/reachability-metadata.json: every jsbml tree node class with its
 * declared and public methods and declared fields registered for reflection.
 *
 * <p>Why: jsbml's {@code AbstractTreeNode.toString()} reflects over a node's {@code isSet*}/{@code get*}/{@code set*}
 * methods and its fields, and jsbml calls it while building debug messages even when debug logging is off (for
 * instance {@code XMLNodeReader.processNamespace} formats its context object, an {@code Annotation}). The recording run
 * (MainRecorder under native-image-agent) only registers what its models happen to reach, so the native library failed
 * on other models with {@code MissingReflectionRegistrationError: ... org.sbml.jsbml.Annotation.isSetAbout()}
 * (BioModels 56, 94, 150, 168, 169, 192).
 *
 * <p>Run from the repository root after building vcell-native as Java (regenerate when the jsbml version changes):
 * {@code java -cp vcell-native/target/vcell-native-1.0-SNAPSHOT.jar scripts/JsbmlReflection.java}
 */
public class JsbmlReflection {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Path out = Path.of(args.length > 0 ? args[0] : "vcell-native/src/main/native-image/jsbml/reachability-metadata.json");
        Class<?> treeNode = Class.forName("org.sbml.jsbml.AbstractTreeNode", false, JsbmlReflection.class.getClassLoader());
        String jar = treeNode.getProtectionDomain().getCodeSource().getLocation().getPath();
        TreeSet<String> names = new TreeSet<>();
        try (JarFile jarFile = new JarFile(jar)) {
            jarFile.stream().map(e -> e.getName())
                .filter(n -> n.startsWith("org/sbml/jsbml/") && n.endsWith(".class") && !n.contains("$"))
                .forEach(n -> names.add(n.substring(0, n.length() - ".class".length()).replace('/', '.')));
        }
        List<String> entries = new ArrayList<>();
        for (String name : names) {
            Class<?> c;
            try {
                c = Class.forName(name, false, JsbmlReflection.class.getClassLoader());
            } catch (Throwable e) {
                continue; // depends on a class this build leaves out
            }
            if (!treeNode.isAssignableFrom(c) || c.isInterface()) {
                continue;
            }
            entries.add("        {\n            \"type\": \"" + name + "\",\n"
                + "            \"allDeclaredMethods\": true,\n"
                + "            \"allPublicMethods\": true,\n"
                + "            \"allDeclaredFields\": true\n        }");
        }
        Files.createDirectories(out.getParent());
        Files.writeString(out, "{\n    \"reflection\": [\n" + String.join(",\n", entries) + "\n    ]\n}\n");
        System.out.println("wrote " + entries.size() + " jsbml tree node classes to " + out);
    }
}
