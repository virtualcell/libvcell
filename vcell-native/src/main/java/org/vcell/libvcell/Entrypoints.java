package org.vcell.libvcell;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.json.simple.JSONValue;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import static org.vcell.libvcell.SolverUtils.sbmlToFiniteVolumeInput;
import static org.vcell.libvcell.SolverUtils.vcmlToFiniteVolumeInput;
import static org.vcell.libvcell.ModelUtils.vcml_to_sbml;
import static org.vcell.libvcell.ModelUtils.sbml_to_vcml;
import static org.vcell.libvcell.ModelUtils.vcml_to_vcml;


public class Entrypoints {
    private static final Logger logger = LogManager.getLogger(Entrypoints.class);
    // Store allocated memory to prevent premature deallocation
    private static final ConcurrentHashMap<Long, CTypeConversion.CCharPointerHolder> allocatedMemory = new ConcurrentHashMap<>();


    private static CCharPointer createString(String str) {
        CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(str);
        CCharPointer ptr = holder.get();
        allocatedMemory.put(ptr.rawValue(), holder);
        return ptr;
    }


    @CEntryPoint(name = "freeString", documentation = "Release memory allocated for a string")
    public static void freeString(
            IsolateThread ignoredThread,
            CCharPointer ptr) {
        if (ptr.isNonNull()) {
            allocatedMemory.remove(ptr.rawValue());
        }
    }

    // serialized in JSON and returned as a String (CCharPointer)
    public record ReturnValue(boolean success, String message) {
        public String toJson() {
            String escaped_message = JSONValue.escape(message);
            return "{\"success\":" + success + ",\"message\":\"" + escaped_message + "\"}";
        }
    }


    @CEntryPoint(
            name = "vcmlToFiniteVolumeInput",
            documentation = """
                    Converts VCML file into Finite Volume Input files.
                      vcml_content: text of VCML XML document
                      simulation_name: name of the simulation to convert
                      output_dir_path: path to the output directory (expected to be subdirectory of the workspace)
                      Returns a JSON string with success status and message"""
    )
    public static CCharPointer entrypoint_vcmlToFiniteVolumeInput(
            IsolateThread ignoredThread,
            CCharPointer vcml_content,
            CCharPointer simulation_name,
            CCharPointer output_dir_path) {
        ReturnValue returnValue;
        try {
            String vcmlContentStr = CTypeConversion.toJavaString(vcml_content);
            String simulationName = CTypeConversion.toJavaString(simulation_name);
            String outputDirPathStr = CTypeConversion.toJavaString(output_dir_path);
            File outputDir = new File(outputDirPathStr);
            File parentDir = outputDir.getParentFile();
            vcmlToFiniteVolumeInput(vcmlContentStr, simulationName, parentDir, outputDir);
            returnValue = new ReturnValue(true, "Success");
        }catch (Throwable t) {
            logger.error("Error processing spatial model", t);
            returnValue = new ReturnValue(false, t.getMessage());
        }
        // return result as a json string
        String json = returnValue.toJson();
        logger.info("Returning from vcmlToFiniteVolumeInput: " + json);
        return createString(json);
    }

    @CEntryPoint(
            name = "sbmlToFiniteVolumeInput",
            documentation = """
                    Converts SBML file into Finite Volume Input files
                      sbml_content: text content of SBML XML document
                      output_dir_path: path to the output directory
                      Returns a JSON string with success status and message"""
    )
    public static CCharPointer entrypoint_sbmlToFiniteVolumeInput(
            IsolateThread ignoredThread,
            CCharPointer sbml_content,
            CCharPointer output_dir_path) {
        ReturnValue returnValue;
        try {
            String sbmlContent = CTypeConversion.toJavaString(sbml_content);
            String outputDirPathStr = CTypeConversion.toJavaString(output_dir_path);
            sbmlToFiniteVolumeInput(sbmlContent, new File(outputDirPathStr));
            returnValue = new ReturnValue(true, "Success");
        }catch (Throwable t) {
            logger.error("Error processing spatial model", t);
            returnValue = new ReturnValue(false, t.getMessage());
        }
        // return result as a json string
        String json = returnValue.toJson();
        logger.info("Returning from sbmlToFiniteVolumeInput: " + json);
        return createString(json);
    }

    @CEntryPoint(
            name = "vcmlToSbml",
            documentation = """
                    Converts VCML file into an SBML file.
                      vcml_content: text of VCML XML document
                      application_name: name of the application to export
                      sbml_file_path: path to the SBML file to write
                      validate_sbml: whether to validate the SBML file
                      Returns a JSON string with success status and message"""
    )
    public static CCharPointer entrypoint_vcmlToSbml(
            IsolateThread ignoredThread,
            CCharPointer vcml_content,
            CCharPointer application_name,
            CCharPointer sbml_file_path,
            int validate_sbml) {
        ReturnValue returnValue;
        try {
            String vcmlContentStr = CTypeConversion.toJavaString(vcml_content);
            String applicationName = CTypeConversion.toJavaString(application_name);
            Path sbmlFilePath = new File(CTypeConversion.toJavaString(sbml_file_path)).toPath();
            vcml_to_sbml(vcmlContentStr, applicationName, sbmlFilePath, CTypeConversion.toBoolean(validate_sbml));
            returnValue = new ReturnValue(true, "Success");
        }catch (Throwable t) {
            logger.error("Error translating vcml application to sbml", t);
            returnValue = new ReturnValue(false, t.getMessage());
        }
        // return result as a json string
        String json = returnValue.toJson();
        logger.info("Returning from vcellToSbml: " + json);
        return createString(json);
    }

    @CEntryPoint(
            name = "sbmlToVcml",
            documentation = """
                    Converts SBML file into a VCML file.
                      sbml_content: text of SBML XML document
                      vcml_file_path: path to the VCML file to write
                      validate_sbml: whether to validate the SBML file
                      Returns a JSON string with success status and message"""
    )
    public static CCharPointer entrypoint_sbmlToVcml(
            IsolateThread ignoredThread,
            CCharPointer sbml_content,
            CCharPointer vcml_file_path,
            int validate_sbml) {
        ReturnValue returnValue;
        try {
            String sbmlContentStr = CTypeConversion.toJavaString(sbml_content);
            Path vcmlFilePath = new File(CTypeConversion.toJavaString(vcml_file_path)).toPath();
            sbml_to_vcml(sbmlContentStr, vcmlFilePath, CTypeConversion.toBoolean(validate_sbml));
            returnValue = new ReturnValue(true, "Success");
        }catch (Throwable t) {
            logger.error("Error translating sbml to vcml", t);
            returnValue = new ReturnValue(false, t.getMessage());
        }
        // return result as a json string
        String json = returnValue.toJson();
        logger.info("Returning from sbmlToVcell: " + json);
        return createString(json);
    }

    @CEntryPoint(
            name = "vcmlToVcml",
            documentation = """
                    Updates a VCML file into a fully populated VCML file.
                      vcml_content: text of VCML XML document
                      vcml_file_path: path to the VCML file to write
                      Returns a JSON string with success status and message"""
    )
    public static CCharPointer entrypoint_vcmlToVcml(
            IsolateThread ignoredThread,
            CCharPointer vcml_content,
            CCharPointer vcml_file_path) {
        ReturnValue returnValue;
        try {
            String vcmlContentStr = CTypeConversion.toJavaString(vcml_content);
            Path vcmlFilePath = new File(CTypeConversion.toJavaString(vcml_file_path)).toPath();
            vcml_to_vcml(vcmlContentStr, vcmlFilePath);
            returnValue = new ReturnValue(true, "Success");
        }catch (Throwable t) {
            logger.error("Error refreshing vcml", t);
            returnValue = new ReturnValue(false, t.getMessage());
        }
        // return result as a json string
        String json = returnValue.toJson();
        logger.info("Returning from vcellToVcml: " + json);
        return createString(json);
    }

}
