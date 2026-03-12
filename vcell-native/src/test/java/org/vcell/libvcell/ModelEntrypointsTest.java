package org.vcell.libvcell;

import cbit.image.ImageException;
import cbit.util.xml.VCLoggerException;
import cbit.vcell.geometry.GeometryException;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.xml.XmlParseException;
import org.junit.jupiter.api.Test;
import org.vcell.sbml.SbmlException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.vcell.libvcell.ModelUtils.*;
import static org.vcell.libvcell.TestUtils.getFileContentsAsString;

public class ModelEntrypointsTest {

    @Test
    public void test_sbml_to_vcml() throws MappingException, IOException, XmlParseException, VCLoggerException {
        String sbmlContent = getFileContentsAsString("/TinySpatialProject_Application0.xml");
        File parent_dir = Files.createTempDirectory("sbmlToVcml").toFile();
        File vcml_temp_file = new File(parent_dir, "temp.vcml");
        sbml_to_vcml(sbmlContent, vcml_temp_file.toPath());
        assert(vcml_temp_file.exists());
    }

    @Test
    public void test_vcml_to_sbml_with_round_trip() throws MappingException, IOException, XmlParseException, XMLStreamException, SbmlException, ImageException, GeometryException, ExpressionException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToSbml").toFile();
        String applicationName = "unnamed_spatialGeom";

        File sbml_temp_file_true = new File(parent_dir, "temp_true.sbml");
        vcml_to_sbml(vcmlContent, applicationName, sbml_temp_file_true.toPath(), true);
        assert(sbml_temp_file_true.exists());
    }

    @Test
    public void test_vcml_to_sbml_without_round_trip() throws MappingException, IOException, XmlParseException, XMLStreamException, SbmlException, ImageException, GeometryException, ExpressionException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToSbml").toFile();
        String applicationName = "unnamed_spatialGeom";

        File sbml_temp_file_false = new File(parent_dir, "temp_false.sbml");
        vcml_to_sbml(vcmlContent, applicationName, sbml_temp_file_false.toPath(), false);
        assert(sbml_temp_file_false.exists());
    }

    @Test
    public void test_vcml_to_vcml() throws MappingException, IOException, XmlParseException, XMLStreamException, SbmlException {
        String vcmlContent = getFileContentsAsString("/TinySpatialProject_Application0.vcml");
        File parent_dir = Files.createTempDirectory("vcmlToVcml").toFile();
        File vcml_temp_file = new File(parent_dir, "temp.vcml");
        vcml_to_vcml(vcmlContent, vcml_temp_file.toPath());
        assert(vcml_temp_file.exists());
    }

	@Test
	public void test_get_python_infix(){
		String vcellInfix = "id_1 * csc(id_0 ^ 2.2)";
		String pythonInfix;
		try {
			pythonInfix = get_python_infix(vcellInfix);
		} catch (ExpressionException e) {
			System.err.println("get_python_infix exception: " + e.getMessage());
			assert(false);
			return;
		}
		String expected = "(id_1 * (1.0/math.sin(((id_0)**(2.2)))))";
		assert expected.equals(pythonInfix);
	}

	@Test
	public void test_bad_python_infix_attempt(){
		String vcellInfix = "id_1 / + / /-  cos(/ / /) id_2";
		String pythonInfix;
		try {
			pythonInfix = get_python_infix(vcellInfix);
		} catch (ExpressionException e) {
			return; // this is what we'd expect
		}
		System.err.println("test_bad_python_infix_attempt did not throw an exception");
		assert(false);
	}

	@Test
	public void test_get_num_expr_infix(){
		String vcellInfix = "(id_2 || 3.2) * id_1 * csc(id_0 ^ 2.2)";
		String convertedInfix;
		try {
			convertedInfix = get_numexpr_infix(vcellInfix);
		} catch (ExpressionException e) {
			System.err.println("get_python_infix exception: " + e.getMessage());
			assert(false);
			return;
		}
		String expected = "(where(((0.0!=id_2) | (0.0!=3.2)), id_1 * (1.0/sin(((id_0)**(2.2)))), 0.0))";
		assert expected.equals(convertedInfix);
	}

	@Test
	public void test_bad_num_expr_infix_attempt(){
		String vcellInfix = "id_1 / + / /-  cos(/ / /) id_2";
		String convertedInfix;
		try {
			convertedInfix = get_numexpr_infix(vcellInfix);
		} catch (ExpressionException e) {
			return; // this is what we'd expect
		}
		System.err.println("test_bad_python_infix_attempt did not throw an exception");
		assert(false);
	}
}
