package org.vcell.libvcell.solvers;

import cbit.vcell.messaging.server.SimulationTask;
import cbit.vcell.solver.SolverException;
import cbit.vcell.solvers.MovingBoundaryFileWriter;
import cbit.vcell.solvers.MovingBoundarySolver;

import java.io.File;
import java.io.PrintWriter;

/**
 * Standalone variant of {@link MovingBoundarySolver} that writes the Moving Boundary solver
 * input file(s) without launching (or even locating) the native solver executable.
 *
 * <p>The base {@code MovingBoundarySolver.initialize()} both writes the input and calls
 * {@code setMathExecutable(getMathExecutableCommand())}, and the latter requires the
 * MovingBoundary solver binary to be installed on disk (it throws otherwise). In the libvcell
 * context we only want the input files, so this class exposes an input-only entry point that
 * mirrors the input-writing portion of {@code initialize()}. This parallels how
 * {@link org.vcell.libvcell.solvers.LocalFVSolverStandalone} exposes a public {@code initialize()}
 * for the Finite Volume solver.
 */
public class LocalMovingBoundarySolverStandalone extends MovingBoundarySolver {

    public LocalMovingBoundarySolverStandalone(SimulationTask simTask, File dir) throws SolverException {
        super(simTask, dir, false);
    }

    /**
     * Write only the Moving Boundary solver input (the {@code <basename>mb.xml} file, plus the
     * {@code .functions} file), skipping the native-executable lookup performed by
     * {@link MovingBoundarySolver#initialize()}.
     */
    public void writeInputFiles() throws SolverException {
        writeFunctionsFile();
        String inputFilename = getBaseName() + "mb.xml";
        try (PrintWriter pw = new PrintWriter(inputFilename)) {
            // MovingBoundaryFileWriter ignores the resampledGeometry argument and reads the geometry
            // from the simulation's MathDescription, so null is acceptable here. Messaging is off.
            MovingBoundaryFileWriter mbfw = new MovingBoundaryFileWriter(pw, simTask, null, false, getBaseName());
            mbfw.write();
        } catch (Exception e) {
            throw new SolverException("Can't write Moving Boundary input file " + inputFilename, e);
        }
    }
}
