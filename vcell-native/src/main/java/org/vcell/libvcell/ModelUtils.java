package org.vcell.libvcell;

import cbit.image.ImageException;
import cbit.util.xml.VCLogger;
import cbit.util.xml.VCLoggerException;
import cbit.util.xml.XmlUtil;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.biomodel.ModelUnitConverter;
import cbit.vcell.geometry.GeometryException;
import cbit.vcell.geometry.GeometrySpec;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.mongodb.VCMongoMessage;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import org.vcell.sbml.SbmlException;
import org.vcell.sbml.vcell.SBMLAnnotationUtil;
import org.vcell.sbml.vcell.SBMLExporter;
import org.vcell.sbml.vcell.SBMLImporter;
import org.vcell.util.Pair;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

public class ModelUtils {

    public static void sbml_to_vcml(String sbml_content, Path vcmlPath)
            throws VCLoggerException, XmlParseException, IOException, MappingException {

        GeometrySpec.avoidAWTImageCreation = true;
        VCMongoMessage.enabled = false;
        XmlHelper.cloneUsingXML = true;

        record LoggerMessage(VCLogger.Priority priority, VCLogger.ErrorType errorType, String message) {};
        final ArrayList<LoggerMessage> messages = new ArrayList<>();

        VCLogger vclogger = new VCLogger() {
            @Override public boolean hasMessages() { return false; }
            @Override public void sendAllMessages() { }
            @Override public void sendMessage(Priority p, ErrorType et, String message) {
                messages.add(new LoggerMessage(p,et,message));
            }
        };

        // parse the SBML content
        final BioModel bioModel;
        try (InputStream inputStream = new ByteArrayInputStream(sbml_content.getBytes())) {
            // create a SBMLImporter from the XMLSource
            boolean validateSBML = true;
            SBMLImporter sbmlImporter = new SBMLImporter(inputStream, vclogger, validateSBML);
            bioModel = sbmlImporter.getBioModel();
        }
        bioModel.updateAll(false);

        // check for errors and warnings
        for (LoggerMessage message : messages) {
            if (message.priority == VCLogger.Priority.HighPriority) {
                throw new RuntimeException("Error: " + message.message);
            } else if (message.priority == VCLogger.Priority.MediumPriority) {
                System.err.println("Warning: " + message.message);
            }
        }

        // write the BioModel to a VCML file
        String vcml_str = XmlHelper.bioModelToXML(bioModel);
        XmlUtil.writeXMLStringToFile(vcml_str, vcmlPath.toFile().getAbsolutePath(), true);
    }


    public static void vcml_to_sbml(String vcml_content, String applicationName, Path sbmlPath, boolean roundTripValidation)
            throws XmlParseException, IOException, XMLStreamException, SbmlException, MappingException, ImageException, GeometryException, ExpressionException {
        GeometrySpec.avoidAWTImageCreation = true;
        VCMongoMessage.enabled = false;
        XmlHelper.cloneUsingXML = true;

        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);

        if (applicationName == null || applicationName.isEmpty()) {
            throw new RuntimeException("Error: Application name is null or empty");
        }

        if (bioModel.getSimulationContext(applicationName) == null) {
            throw new RuntimeException("Error: Simulation context not found for application name: " + applicationName);
        }

        // change the unit system to SBML preferred units if not already.
        final BioModel sbmlPreferredUnitsBM;
        if (!bioModel.getModel().getUnitSystem().compareEqual(ModelUnitConverter.createSbmlModelUnitSystem())) {
            sbmlPreferredUnitsBM = ModelUnitConverter.createBioModelWithSBMLUnitSystem(bioModel);
            if(sbmlPreferredUnitsBM == null) {
                throw new RuntimeException("Unable to clone BioModel with SBML unit system");
            }
        } else {
            sbmlPreferredUnitsBM = bioModel;
        }

        SimulationContext simContext = sbmlPreferredUnitsBM.getSimulationContext(applicationName);

        int sbml_level = 3;
        int sbml_version = 1;
        SBMLExporter sbmlExporter = new SBMLExporter(simContext, sbml_level, sbml_version, roundTripValidation);
        String sbml_string = sbmlExporter.getSBMLString();

        // cleanup the string of all the "sameAs" statements
        sbml_string = SBMLAnnotationUtil.postProcessCleanup(sbml_string);

        XmlUtil.writeXMLStringToFile(sbml_string, sbmlPath.toFile().getAbsolutePath(), true);
    }

    public static void vcml_to_vcml(String vcml_content, Path vcmlPath) throws XmlParseException, IOException, MappingException {
        GeometrySpec.avoidAWTImageCreation = true;
        VCMongoMessage.enabled = false;
        XmlHelper.cloneUsingXML = true;

        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);
        // write the BioModel to a VCML file
        String vcml_str = XmlHelper.bioModelToXML(bioModel);
        XmlUtil.writeXMLStringToFile(vcml_str, vcmlPath.toFile().getAbsolutePath(), true);
    }
}
