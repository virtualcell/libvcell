package org.vcell.libvcell.solvers;

import cbit.vcell.field.FieldDataIdentifierSpec;
import cbit.vcell.field.FieldFunctionArguments;
import cbit.vcell.field.FieldUtilities;
import cbit.vcell.math.MathException;
import cbit.vcell.math.Variable;
import cbit.vcell.math.VariableType;
import cbit.vcell.messaging.server.SimulationTask;
import cbit.vcell.parser.Expression;
import cbit.vcell.parser.ExpressionException;
import cbit.vcell.simdata.*;
import cbit.vcell.solver.AnnotatedFunction;
import cbit.vcell.solver.Simulation;
import cbit.vcell.solver.SimulationJob;
import cbit.vcell.solver.SolverException;
import cbit.vcell.solver.server.SimulationMessage;
import cbit.vcell.solver.server.SolverStatus;
import cbit.vcell.solver.test.MathTestingUtilities;
import cbit.vcell.solvers.*;
import org.vcell.util.DataAccessException;
import org.vcell.util.ISize;
import org.vcell.util.document.ExternalDataIdentifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


public class LocalFVSolverStandalone extends FVSolverStandalone {
    final File dataDir;
    final File parentDir;

    public LocalFVSolverStandalone(SimulationTask simulationTask, File dataDir) throws SolverException {
        super(simulationTask, dataDir, false);
        this.dataDir = dataDir;
        this.parentDir = dataDir.getParentFile();
    }

    @Override
    public void initialize() throws SolverException {
        try {
            Simulation sim = simTask.getSimulation();
            if(sim.isSerialParameterScan()){
                //write functions file for all the simulations in the scan
                if (sim.getJobCount() != sim.getScanCount()){
                    throw new SolverException("using scanIndex for JobIndex, assuming no trials");
                }
                for(int scan = 0; scan < sim.getScanCount(); scan++){
                    SimulationJob simJob = new SimulationJob(sim, scan, simTask.getSimulationJob().getFieldDataIdentifierSpecs());
                    // ** Dumping the functions of a simulation into a '.functions' file.
                    String basename = new File(getSaveDirectory(), simJob.getSimulationJobID()).getPath();
                    String functionFileName = basename + FUNCTIONFILE_EXTENSION;

                    Vector<AnnotatedFunction> funcList = simJob.getSimulationSymbolTable().createAnnotatedFunctionsList(simTask.getSimulation().getMathDescription());
                    File existingFunctionFile = new File(functionFileName);
                    if(existingFunctionFile.exists()){
                        Vector<AnnotatedFunction> oldFuncList = FunctionFileGenerator.readFunctionsFile(existingFunctionFile, simTask.getSimulationJobID());
                        for(AnnotatedFunction func : oldFuncList){
                            if(func.isOldUserDefined()){
                                funcList.add(func);
                            }
                        }
                    }

                    //Try to save existing user defined functions
                    FunctionFileGenerator functionFileGenerator = new FunctionFileGenerator(functionFileName, funcList);
                    try {
                        functionFileGenerator.generateFunctionFile();
                    } catch(Exception e){
                        throw new RuntimeException("Error creating .function file for " + functionFileGenerator.getBasefileName() + e.getMessage(), e);
                    }
                }

            } else {
                writeFunctionsFile();
            }

            writeVCGAndResampleFieldData();

            setSolverStatus(new SolverStatus(SolverStatus.SOLVER_RUNNING, SimulationMessage.MESSAGE_SOLVER_RUNNING_INIT));
            fireSolverStarting(SimulationMessage.MESSAGE_SOLVEREVENT_STARTING_INIT);

            setSolverStatus(new SolverStatus(SolverStatus.SOLVER_RUNNING, SimulationMessage.MESSAGE_SOLVER_RUNNING_INPUT_FILE));

            File fvinputFile = new File(getBaseName() + ".fvinput");
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(new FileWriter(fvinputFile));
                LocalFiniteVolumeFileWriter writer = new LocalFiniteVolumeFileWriter(pw, simTask, getResampledGeometry(), dataDir);
                writer.write();
            } finally {
                if(pw != null){
                    pw.close();
                }
            }
        } catch(Exception ex){
            throw new SolverException(ex.getMessage(), ex);
        }
    }

    @Override
    public void writeVCGAndResampleFieldData() throws SolverException {
        try {
            // write subdomains file
            String baseName = new File(getBaseName()).getName();
            SubdomainInfo.write(new File(getSaveDirectory(), baseName + SimDataConstants.SUBDOMAINS_FILE_SUFFIX), simTask.getSimulation().getMathDescription());

            PrintWriter pw = new PrintWriter(new FileWriter(new File(getSaveDirectory(), baseName + SimDataConstants.VCG_FILE_EXTENSION)));
            GeometryFileWriter.write(pw, getResampledGeometry());
            pw.close();

            FieldDataIdentifierSpec[] argFieldDataIDSpecs = simTask.getSimulationJob().getFieldDataIdentifierSpecs();
            if(argFieldDataIDSpecs != null && argFieldDataIDSpecs.length > 0){

                FieldFunctionArguments psfFieldFunc = null;
                Variable var = simTask.getSimulationJob().getSimulationSymbolTable().getVariable(Simulation.PSF_FUNCTION_NAME);
                if(var != null){
                    FieldFunctionArguments[] ffas = FieldUtilities.getFieldFunctionArguments(var.getExpression());
                    if(ffas == null || ffas.length == 0){
                        throw new DataAccessException("Point Spread Function " + Simulation.PSF_FUNCTION_NAME + " can only be a single field function.");
                    } else {
                        Expression newexp;
                        try {
                            newexp = new Expression(ffas[0].infix());
                            if(!var.getExpression().compareEqual(newexp)){
                                throw new DataAccessException("Point Spread Function " + Simulation.PSF_FUNCTION_NAME + " can only be a single field function.");
                            }
                            psfFieldFunc = ffas[0];
                        } catch(ExpressionException e){
                            throw new DataAccessException(e.getMessage(), e);
                        }
                    }
                }

                boolean bResample[] = new boolean[argFieldDataIDSpecs.length];
                Arrays.fill(bResample, true);
                for(int i = 0; i < argFieldDataIDSpecs.length; i++){
                    argFieldDataIDSpecs[i].getFieldFuncArgs().getTime().bindExpression(simTask.getSimulationJob().getSimulationSymbolTable());
                    if(argFieldDataIDSpecs[i].getFieldFuncArgs().equals(psfFieldFunc)){
                        bResample[i] = false;
                    }
                }

                int numMembraneElements = getResampledGeometry().getGeometrySurfaceDescription().getSurfaceCollection().getTotalPolygonCount();
                CartesianMesh simpleMesh = CartesianMesh.createSimpleCartesianMesh(getResampledGeometry().getOrigin(),
                        getResampledGeometry().getExtent(),
                        simTask.getSimulation().getMeshSpecification().getSamplingSize(),
                        getResampledGeometry().getGeometrySurfaceDescription().getRegionImage());

                writeFieldFunctionData(null, argFieldDataIDSpecs, bResample, simpleMesh, numMembraneElements);
            }
        } catch(Exception e){
            throw new SolverException(e.getMessage());
        }
    }

    public void writeFieldFunctionData(OutputContext outputContext, FieldDataIdentifierSpec[] argFieldDataIDSpecs,
                                       boolean[] bResampleFlags, CartesianMesh newMesh,
                                       int simResampleMembraneDataLength) throws DataAccessException, ExpressionException {

        if(argFieldDataIDSpecs == null || argFieldDataIDSpecs.length == 0) return;

        HashMap<FieldDataIdentifierSpec, File> uniqueFieldDataIDSpecAndFileH = new HashMap<>();
        HashMap<FieldDataIdentifierSpec, Boolean> bFieldDataResample = new HashMap<>();
        int i=0;
        for (FieldDataIdentifierSpec fdiSpec: argFieldDataIDSpecs) {
            File ext_dataDir = new File(this.parentDir, fdiSpec.getFieldFuncArgs().getFieldName());
            if (!uniqueFieldDataIDSpecAndFileH.containsKey(fdiSpec)){
                File newResampledFieldDataFile = new File(dataDir, SimulationData.createCanonicalResampleFileName(getSimulationJob().getVCDataIdentifier(), fdiSpec.getFieldFuncArgs()));
                uniqueFieldDataIDSpecAndFileH.put(fdiSpec,newResampledFieldDataFile);
                bFieldDataResample.put(fdiSpec, bResampleFlags[i]);
            }
            i++;
        }
        try {
            Set<Map.Entry<FieldDataIdentifierSpec, File>> resampleSet = uniqueFieldDataIDSpecAndFileH.entrySet();
            for (Map.Entry<FieldDataIdentifierSpec, File> resampleEntry : resampleSet) {
                if (resampleEntry.getValue().exists()) {
                    continue;
                }
                FieldDataIdentifierSpec fieldDataIdSpec = resampleEntry.getKey();
                FieldFunctionArguments fieldFuncArgs = fieldDataIdSpec.getFieldFuncArgs();
                File dataDir = new File(this.parentDir, fieldFuncArgs.getFieldName());
                boolean bResample = bFieldDataResample.get(fieldDataIdSpec);
                CartesianMesh origMesh = getMesh(fieldDataIdSpec.getExternalDataIdentifier());
                VCData vcData = new SimulationData(fieldDataIdSpec.getExternalDataIdentifier(), dataDir, dataDir, null);
                SimDataBlock simDataBlock = vcData.getSimDataBlock(outputContext, fieldFuncArgs.getVariableName(), fieldFuncArgs.getTime().evaluateConstant());
                VariableType varType = fieldFuncArgs.getVariableType();
                VariableType dataVarType = simDataBlock.getVariableType();
                if (!varType.equals(VariableType.UNKNOWN) && !varType.equals(dataVarType)) {
                    throw new IllegalArgumentException("field function variable type (" + varType.getTypeName() + ") doesn't match real variable type (" + dataVarType.getTypeName() + ")");
                }
                double[] origData = simDataBlock.getData();
                double[] newData = null;
                CartesianMesh resampleMesh = newMesh;
                if (!bResample) {
                    if (resampleMesh.getGeometryDimension() != origMesh.getGeometryDimension()) {
                        throw new DataAccessException("Field data " + fieldFuncArgs.getFieldName() + " (" + origMesh.getGeometryDimension()
                                + "D) should have same dimension as simulation mesh (" + resampleMesh.getGeometryDimension() + "D) because it is not resampled to simulation mesh (e.g. Point Spread Function)");
                    }
                    newData = origData;
                    resampleMesh = origMesh;
                } else {
                    if (CartesianMesh.isSpatialDomainSame(origMesh, resampleMesh)) {
                        newData = origData;
                        if (simDataBlock.getVariableType().equals(VariableType.MEMBRANE)) {
                            if (origData.length != simResampleMembraneDataLength) {
                                throw new Exception("FieldData variable \"" + fieldFuncArgs.getVariableName() +
                                        "\" (" + simDataBlock.getVariableType().getTypeName() + ") " +
                                        "resampling failed: Membrane Data lengths must be equal"
                                );
                            }
                        } else if (!simDataBlock.getVariableType().equals(VariableType.VOLUME)) {
                            throw new Exception("FieldData variable \"" + fieldFuncArgs.getVariableName() +
                                    "\" (" + simDataBlock.getVariableType().getTypeName() + ") " +
                                    "resampling failed: Only Volume and Membrane variable types are supported"
                            );
                        }
                    } else {
                        if (!simDataBlock.getVariableType().compareEqual(VariableType.VOLUME)) {
                            throw new Exception("FieldData variable \"" + fieldFuncArgs.getVariableName() +
                                    "\" (" + simDataBlock.getVariableType().getTypeName() + ") " +
                                    "resampling failed: Only VOLUME FieldData variable type allowed when\n" +
                                    "FieldData spatial domain does not match Simulation spatial domain.\n" +
                                    "Check dimension, xsize, ysize, zsize, origin and extent are equal."
                            );
                        }
                        if (origMesh.getSizeY() == 1 && origMesh.getSizeZ() == 1) {
                            newData = MathTestingUtilities.resample1DSpatialSimple(origData, origMesh, resampleMesh);
                        } else if (origMesh.getSizeZ() == 1) {
                            newData = MathTestingUtilities.resample2DSpatialSimple(origData, origMesh, resampleMesh);
                        } else {
                            newData = MathTestingUtilities.resample3DSpatialSimple(origData, origMesh, resampleMesh);
                        }
                    }
                }
                DataSet.writeNew(resampleEntry.getValue(),
                        new String[]{fieldFuncArgs.getVariableName()},
                        new VariableType[]{simDataBlock.getVariableType()},
                        new ISize(resampleMesh.getSizeX(), resampleMesh.getSizeY(), resampleMesh.getSizeZ()),
                        new double[][]{newData});
            }
        } catch (Exception ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }
    public CartesianMesh getMesh(ExternalDataIdentifier extDataID) throws MathException, IOException {
        File dataDir = new File(this.parentDir, extDataID.getName());
        File meshFile = new File(dataDir, SimulationData.createCanonicalMeshFileName(extDataID.getKey(),extDataID.getJobIndex(), false));
        File meshMetricsFile = new File(dataDir, SimulationData.createCanonicalMeshMetricsFileName(extDataID.getKey(),extDataID.getJobIndex(), false));
        File subDomainFile = new File(dataDir, SimulationData.createCanonicalSubdomainFileName(extDataID.getKey(),extDataID.getJobIndex(), false));
        return CartesianMesh.readFromFiles(meshFile, meshMetricsFile, subDomainFile);
    }
}
