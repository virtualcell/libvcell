package org.vcell.libvcell.solvers;

import cbit.vcell.messaging.server.SimulationTask;
import cbit.vcell.solver.SolverException;
import cbit.vcell.solver.ode.CVodeFileWriter;
import cbit.vcell.solver.ode.IDAFileWriter;
import cbit.vcell.solver.ode.SundialsSolver;

import java.io.File;
import java.io.PrintWriter;

/**
 * Standalone variant of the VCell Sundials ODE solvers ({@code CVodeSolverStandalone} and
 * {@code IDASolverStandalone}) that writes the solver input file without launching (or even
 * locating) the native solver executable.
 *
 * <p>The stock solvers' {@code initialize()} writes the input and then calls
 * {@code setMathExecutable(getMathExecutableCommand())}, which requires the SundialsSolverStandalone
 * binary to be installed on disk (it throws otherwise). In the libvcell context only the input is
 * wanted: it is solved downstream by the {@code pyvcell-odesolver} package
 * ({@code pyvcell_odesolver.solve(input_file, output_file)}), which reads either a CVODE or an IDA
 * input file and writes the {@code .ida} results file. This mirrors
 * {@link LocalMovingBoundarySolverStandalone} for the Moving Boundary solver.
 */
public class LocalOdeSolverStandalone extends SundialsSolver {

    private final boolean useIDA;

    /**
     * @param useIDA write IDA input (the DAE solver, needed when the math has fast systems) rather than
     *               CVODE input
     */
    public LocalOdeSolverStandalone(SimulationTask simTask, File dir, boolean useIDA) throws SolverException {
        super(simTask, dir, false);
        this.useIDA = useIDA;
    }

    /**
     * Write only the ODE solver input (the {@code <basename>.cvodeInput} or {@code <basename>.idaInput}
     * file, plus the {@code .functions} file), skipping the native-executable lookup.
     *
     * @return the input file written
     */
    public File writeInputFiles() throws SolverException {
        writeFunctionsFile();
        String inputFilename = getBaseName() + (useIDA ? IDAINPUT_DATA_EXTENSION : CVODEINPUT_DATA_EXTENSION);
        try (PrintWriter pw = new PrintWriter(inputFilename)) {
            // messaging is off: the downstream solver runs locally, with no JMS broker to report to
            if (useIDA) {
                new IDAFileWriter(pw, simTask, false).write();
            } else {
                new CVodeFileWriter(pw, simTask, false).write();
            }
        } catch (Exception e) {
            throw new SolverException("Can't write " + (useIDA ? "IDA" : "CVODE") + " input file " + inputFilename + ": " + e.getMessage(), e);
        }
        return new File(inputFilename);
    }

    @Override
    protected String[] getMathExecutableCommand() {
        throw new UnsupportedOperationException("LocalOdeSolverStandalone writes solver input only; solve it with pyvcell-odesolver");
    }
}
