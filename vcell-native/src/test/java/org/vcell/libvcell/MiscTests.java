package org.vcell.libvcell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.vcell.libvcell.ModelUtils.vcml_to_vcml;

public class MiscTests {

	@Test
	public void testErrorMessageCreation() throws IOException {
		String vcmlContent;
		try (InputStream is = MiscTests.class.getResourceAsStream("/bad_vcml.vcml")) {
			Assertions.assertNotNull(is);
			vcmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		Path ignored = Files.createTempFile("dummy", ".vcml");
		try {
			vcml_to_vcml(vcmlContent, ignored);
		} catch (Exception e) {
			String actualErrMsg = Entrypoints.generateErrorReport("Something went wrong!", e).strip();

			// The report opens with the caller-supplied top-level message, followed by a structured
			// summary of the nested cause chain. These parts are deterministic, so assert them exactly.
			// We deliberately do NOT assert the verbatim stack-trace frames: they embed vcell-core line
			// numbers (which drift as the submodule updates) and test-runner frames (which differ between
			// IDE and Maven Surefire), making a full-string comparison inherently brittle.
			Assertions.assertTrue(actualErrMsg.startsWith(EXPECTED_SUMMARY), actualErrMsg);

			// A stack-trace section is appended, deepest cause first, listing real frames from the failure.
			Assertions.assertTrue(actualErrMsg.contains("Stack Traces:"), actualErrMsg);
			Assertions.assertTrue(actualErrMsg.contains("1) ExpressionBindingException:"), actualErrMsg);
			Assertions.assertTrue(actualErrMsg.contains("0) XmlParseException:"), actualErrMsg);
			Assertions.assertTrue(actualErrMsg.contains("cbit.vcell.parser.ASTIdNode.bind("), actualErrMsg);
			Assertions.assertTrue(actualErrMsg.contains("cbit.vcell.xml.XmlReader.getSpeciesContextSpecs("), actualErrMsg);
			Assertions.assertTrue(actualErrMsg.contains("org.vcell.libvcell.ModelUtils.vcml_to_vcml("), actualErrMsg);
			return;
		}
		throw new IllegalStateException("An exception was not thrown when one should have been!");
	}

	private static final String EXPECTED_SUMMARY = """
Something went wrong!
Error:
0 --> XmlParseException :: Error setting Velocity parameter for 'H3
1 -----> ExpressionBindingException :: 'norm_X' is either not found in your model or is not allowed to be used in the current context. Check that you have provided the correct and full name (e.g. Ca_Cytosol).""".strip();

}
