package src.solver;

import src.model.SudokuGrid;
import src.model.SolverResult;
import src.util.SudokuValidator;

/**
 * Abstract base class for all Sudoku solvers.
 * Provides common functionality and enforces the solver interface.
 */
public abstract class SudokuSolver {
    protected SudokuGrid grid;
    protected SolverResult result;
    protected long startTime;
    protected long iterations;
    protected long backtracks;

    /**
     * Constructor for SudokuSolver.
     *
     * @param grid the SudokuGrid to solve
     */
    public SudokuSolver(SudokuGrid grid) {
        this.grid = new SudokuGrid(grid);  // Make a copy to avoid modifying the original
        this.result = new SolverResult(this.getClass().getSimpleName());
        this.iterations = 0;
        this.backtracks = 0;
    }

    /**
     * Main solving method. Must be implemented by subclasses.
     *
     * @return the SolverResult containing the solution and metrics
     */
    public abstract SolverResult solve();

    /**
     * Validate the current solution.
     */
    protected boolean isValid() {
        return SudokuValidator.isValid(grid);
    }

    /**
     * Check if the puzzle is completely solved.
     */
    protected boolean isSolved() {
        return grid.isFull() && isValid();
    }

    /**
     * Record a backtrack operation.
     */
    protected void recordBacktrack() {
        backtracks++;
    }

    /**
     * Record an iteration operation.
     */
    protected void recordIteration() {
        iterations++;
    }

    /**
     * Finalize the result with metrics.
     */
    protected void finalizeResult(boolean solved) {
        long endTime = System.currentTimeMillis();
        result.setSolved(solved);
        result.setTimeMs(endTime - startTime);
        result.setIterations(iterations);
        result.setBacktracks(backtracks);
        if (solved) {
            result.setSolution(grid.getGrid());
        }
    }
}
