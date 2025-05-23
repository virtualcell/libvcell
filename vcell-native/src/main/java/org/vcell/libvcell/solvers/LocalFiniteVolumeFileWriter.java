package org.vcell.libvcell.solvers;

import cbit.vcell.field.FieldDataIdentifierSpec;
import cbit.vcell.field.FieldFunctionArguments;
import cbit.vcell.field.FieldUtilities;
import cbit.vcell.geometry.Geometry;
import cbit.vcell.math.Variable;
import cbit.vcell.math.VariableType;
import cbit.vcell.messaging.server.SimulationTask;
import cbit.vcell.parser.DivideByZeroException;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.simdata.DataSet;
import cbit.vcell.simdata.SimDataBlock;
import cbit.vcell.simdata.SimulationData;
import cbit.vcell.simdata.VCData;
import cbit.vcell.solver.Simulation;
import cbit.vcell.solvers.FiniteVolumeFileWriter;
import org.vcell.util.DataAccessException;
import org.vcell.util.document.ExternalDataIdentifier;
import org.vcell.util.document.User;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

public class LocalFiniteVolumeFileWriter extends FiniteVolumeFileWriter {

    public LocalFiniteVolumeFileWriter(PrintWriter pw, SimulationTask simTask, Geometry geo, File workingDir) {
        super(pw, simTask, geo, workingDir);
    }

    @Override
    protected void writeFieldData() throws ExpressionException, DataAccessException {
        FieldDataIdentifierSpec[] fieldDataIDSpecs = simTask.getSimulationJob().getFieldDataIdentifierSpecs();
        if(fieldDataIDSpecs == null || fieldDataIDSpecs.length == 0){
            return;
        }

        printWriter.println("# Field Data");
        printWriter.println("FIELD_DATA_BEGIN");
        printWriter.println("#id, type, new name, name, varname, time, filename");

        FieldFunctionArguments psfFieldFunc = null;

        Variable var = simTask.getSimulationJob().getSimulationSymbolTable().getVariable(Simulation.PSF_FUNCTION_NAME);
        if(var != null){
            FieldFunctionArguments[] ffas = FieldUtilities.getFieldFunctionArguments(var.getExpression());
            if(ffas == null || ffas.length == 0){
                throw new DataAccessException("Point Spread Function " + Simulation.PSF_FUNCTION_NAME + " can only be a single field function.");
            } else {
                Expression newexp = new Expression(ffas[0].infix());
                if(!var.getExpression().compareEqual(newexp)){
                    throw new DataAccessException("Point Spread Function " + Simulation.PSF_FUNCTION_NAME + " can only be a single field function.");
                }
                psfFieldFunc = ffas[0];
            }
        }

        int index = 0;
        HashSet<FieldDataIdentifierSpec> uniqueFieldDataIDSpecs = new HashSet<>();
        uniqueFieldDataNSet = new HashSet<>();
        for (FieldDataIdentifierSpec fieldDataIDSpec : fieldDataIDSpecs) {
            if (!uniqueFieldDataIDSpecs.contains(fieldDataIDSpec)) {
                FieldFunctionArguments ffa = fieldDataIDSpec.getFieldFuncArgs();
                File newResampledFieldDataFile = new File(workingDirectory,
                        SimulationData.createCanonicalResampleFileName(simTask.getSimulationJob().getVCDataIdentifier(),
                                fieldDataIDSpec.getFieldFuncArgs())
                );
                uniqueFieldDataIDSpecs.add(fieldDataIDSpec);
                VariableType varType = fieldDataIDSpec.getFieldFuncArgs().getVariableType();
                final VariableType dataVarType = getVariableTypeFromFieldDataFiles(fieldDataIDSpec, ffa);
                if (varType.equals(VariableType.UNKNOWN)) {
                    varType = dataVarType;
                } else if (!varType.equals(dataVarType)) {
                    throw new IllegalArgumentException("field function variable type (" + varType.getTypeName() + ") doesn't match real variable type (" + dataVarType.getTypeName() + ")");
                }
                if (psfFieldFunc != null && psfFieldFunc.equals(ffa)) {
                    psfFieldIndex = index;
                }
                String fieldDataID = "_VCell_FieldData_" + index;
                printWriter.println(index + " " + varType.getTypeName() + " " + fieldDataID + " " + ffa.getFieldName() + " " + ffa.getVariableName() + " " + ffa.getTime().flatten().infix() + " " + newResampledFieldDataFile);
                uniqueFieldDataNSet.add(
                        new FieldDataNumerics(
                                SimulationData.createCanonicalFieldFunctionSyntax(
                                        ffa.getFieldName(),
                                        ffa.getVariableName(),
                                        ffa.getTime().evaluateConstant(),
                                        ffa.getVariableType().getTypeName()),
                                fieldDataID));
                index++;
            }
        }

        if(psfFieldIndex >= 0){
            printWriter.println("PSF_FIELD_DATA_INDEX " + psfFieldIndex);
        }
        printWriter.println("FIELD_DATA_END");
        printWriter.println();
    }

    private VariableType getVariableTypeFromFieldDataFiles(FieldDataIdentifierSpec fieldDataIDSpec, FieldFunctionArguments ffa) throws DataAccessException {

        try {
            final VariableType dataVarType;
            //
            // First, look to see if the processed field data file already exists, if so, use it to determine the variable type
            //
            DataSet dataSet = new DataSet();
            ExternalDataIdentifier existingFieldDataID = new ExternalDataIdentifier(this.simTask.getSimKey(), User.tempUser, ffa.getFieldName());
            File existingFieldDataFile = new File(workingDirectory, SimulationData.createCanonicalResampleFileName(existingFieldDataID, ffa));
            if (existingFieldDataFile.exists()) {
                // field data file may already exist
                // 1. from pyvcell writing the field data file directly into this directory for an image-based field data
                dataSet.read(existingFieldDataFile, null);
                int varTypeInteger = dataSet.getVariableTypeInteger(fieldDataIDSpec.getFieldFuncArgs().getVariableName());
                dataVarType = VariableType.getVariableTypeFromInteger(varTypeInteger);
            }else {
                //
                // Else, read the unprocessed simulation results referenced in the FieldDataIdentifierSpec and extract the VariableType
                //
                File ext_data_dir = new File(workingDirectory.getParentFile(), ffa.getFieldName());
                VCData vcData = new SimulationData(fieldDataIDSpec.getExternalDataIdentifier(), ext_data_dir, ext_data_dir, null);
                SimDataBlock simDataBlock = vcData.getSimDataBlock(null, ffa.getVariableName(), ffa.getTime().evaluateConstant());
                dataVarType = simDataBlock.getVariableType();
            }
            return dataVarType;
        } catch (IOException | ExpressionException | DataAccessException e) {
            throw new DataAccessException("Error reading field data file: " + e.getMessage());
        }
    }

}
