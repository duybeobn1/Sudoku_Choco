package src.solver;

import src.heuristic.CellHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.util.SudokuValidator;

/**
 * Incomplete Solver using backtracking with constraint propagation.
 * Uses heuristics to guide the search but may not find a solution in all cases.
 * No external dependencies required for core functionality.
 */
public class IncompleteSolver extends SudokuSolver {
    private CellHeuristic heuristic;
    private boolean propagate = true;
    private long maxIterations = Long.MAX_VALUE;

    /**
     * Constructor for IncompleteSolver.
     *
     * @param grid the SudokuGrid to solve
     */
    public IncompleteSolver(SudokuGrid grid) {
        super(grid);
        this.heuristic = new MRVHeuristic();  // Default heuristic: Minimum Remaining Values
    }

    /**
     * Set the cell selection heuristic.
     *
     * @param heuristic the CellHeuristic to use
     */
    public void setHeuristic(CellHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Enable or disable constraint propagation.
     *
     * @param propagate true to enable propagation
     */
    public void setPropagate(boolean propagate) {
        this.propagate = propagate;
    }

    /**
     * Set maximum number of iterations (for timeout simulation).
     *
     * @param maxIterations maximum iterations
     */
    public void setMaxIterations(long maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Solve the Sudoku puzzle using backtracking with heuristics.
     */
    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        grid.reset();

        // Initial propagation
        if (propagate) {
            propagateInitial();
        }

        // Backtracking search
        boolean solved = backtrack();
        finalizeResult(solved);

        return result;
    }

    /**
     * Initial constraint propagation: remove candidates based on clues.
     */
    private void propagateInitial() {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // For each given clue, remove it from all related cells
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.get(i, j) != 0) {
                    int value = grid.get(i, j);

                    // Remove from row
                    for (int col = 0; col < N; col++) {
                        if (col != j && grid.isEmpty(i, col)) {
                            grid.removeCandidate(i, col, value);
                        }
                    }

                    // Remove from column
                    for (int row = 0; row < N; row++) {
                        if (row != i && grid.isEmpty(row, j)) {
                            grid.removeCandidate(row, j, value);
                        }
                    }

                    // Remove from block
                    int blockRow = (i / n) * n;
                    int blockCol = (j / n) * n;
                    for (int r = blockRow; r < blockRow + n; r++) {
                        for (int c = blockCol; c < blockCol + n; c++) {
                            if ((r != i || c != j) && grid.isEmpty(r, c)) {
                                grid.removeCandidate(r, c, value);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Recursive backtracking solver with constraint checking.
     */
    private boolean backtrack() {
        recordIteration();

        // Check iteration limit
        if (iterations > maxIterations) {
            return false;
        }

        // Check if solved
        if (grid.isFull()) {
            return isValid();
        }

        // Check for contradictions
        if (hasContradiction()) {
            return false;
        }

        // Find the next cell to fill using heuristic
        int[] nextCell = heuristic.selectCell(grid);
        if (nextCell == null) {
            return false;  // No cell to fill (should not happen if grid is not full)
        }

        int row = nextCell[0];
        int col = nextCell[1];

        // Try each possible value
        int N = grid.getSize();
        for (int value = 1; value <= N; value++) {
            if (isValidPlacement(row, col, value)) {
                // Make a copy for backtracking
                SudokuGrid backup = new SudokuGrid(grid);

                // Place the value
                grid.set(row, col, value);

                // Apply constraint propagation
                if (propagate) {
                    propagateValue(row, col, value);
                }

                // Recursively solve
                if (backtrack()) {
                    return true;
                }

                // Backtrack
                recordBacktrack();
                grid = new SudokuGrid(backup);
            }
        }

        return false;
    }

    /**
     * Check if there are any cells with no possible candidates.
     */
    private boolean hasContradiction() {
        int N = grid.getSize();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.isEmpty(i, j) && grid.countCandidates(i, j) == 0) {
                    return true;  // Contradiction found
                }
            }
        }
        return false;
    }

    /**
     * Check if a value can be placed at a given position.
     */
    private boolean isValidPlacement(int row, int col, int value) {
        return SudokuValidator.isValidPlacement(grid, row, col, value);
    }

    /**
     * Propagate the constraints after placing a value.
     * Remove the placed value from all related cells.
     */
    private void propagateValue(int row, int col, int value) {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Remove from row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                grid.removeCandidate(row, j, value);
            }
        }

        // Remove from column
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) {
                grid.removeCandidate(i, col, value);
            }
        }

        // Remove from block
        int blockRow = (row / n) * n;
        int blockCol = (col / n) * n;
        for (int r = blockRow; r < blockRow + n; r++) {
            for (int c = blockCol; c < blockCol + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c)) {
                    grid.removeCandidate(r, c, value);
                }
            }
        }
    }
}
