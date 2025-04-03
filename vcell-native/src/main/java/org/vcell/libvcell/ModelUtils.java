package org.vcell.libvcell;

import cbit.util.xml.VCLogger;
import cbit.util.xml.VCLoggerException;
import cbit.util.xml.XmlUtil;
import cbit.vcell.biomodel.BioModel;
import cbit.vcell.mapping.MappingException;
import cbit.vcell.mapping.SimulationContext;
import cbit.vcell.xml.XMLSource;
import cbit.vcell.xml.XmlHelper;
import cbit.vcell.xml.XmlParseException;
import org.vcell.sbml.SbmlException;
import org.vcell.sbml.vcell.SBMLExporter;
import org.vcell.sbml.vcell.SBMLImporter;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

public class ModelUtils {

    public static void sbml_to_vcml(String sbml_content, Path vcmlPath)
            throws VCLoggerException, XmlParseException, IOException, MappingException {

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


    public static void vcml_to_sbml(String vcml_content, String applicationName, Path sbmlPath)
            throws XmlParseException, IOException, XMLStreamException, SbmlException, MappingException {

        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);
        SimulationContext simContext = bioModel.getSimulationContext(applicationName);
        boolean validateSBML = true;
        SBMLExporter sbmlExporter = new SBMLExporter(simContext, 3, 1, validateSBML);
        String sbml_string = sbmlExporter.getSBMLString();
        XmlUtil.writeXMLStringToFile(sbml_string, sbmlPath.toFile().getAbsolutePath(), true);
    }

    public static void vcml_to_vcml(String vcml_content, Path vcmlPath) throws XmlParseException, IOException, MappingException {
        BioModel bioModel = XmlHelper.XMLToBioModel(new XMLSource(vcml_content));
        bioModel.updateAll(false);
        // write the BioModel to a VCML file
        String vcml_str = XmlHelper.bioModelToXML(bioModel);
        XmlUtil.writeXMLStringToFile(vcml_str, vcmlPath.toFile().getAbsolutePath(), true);
    }
}
