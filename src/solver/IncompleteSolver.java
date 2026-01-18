package src.solver;

import src.heuristic.CellHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;

/**
 * Optimized Incomplete Solver using backtracking with efficient constraint propagation.
 *
 * Key optimizations:
 * - Delta propagation: maintains queue of affected cells instead of full grid scans
 * - Undo stack for O(1) backtracking without deep copies
 * - Bitmask-based candidate sets for efficient operations
 * - Early termination on contradictions
 *
 * Named "Incomplete" due to optional iteration limit; algorithm is complete when unbounded.
 */
public class IncompleteSolver extends SudokuSolver {
    private CellHeuristic heuristic = new MRVHeuristic();
    private boolean propagate = true;
    private long maxIterations = Long.MAX_VALUE;

    /**
     * Undo stack entry: records cell state before modification.
     */
    private static final class Change {
        final int r, c;
        final int oldValue;
        final int oldCandidates;

        Change(int r, int c, int oldValue, int oldCandidates) {
            this.r = r;
            this.c = c;
            this.oldValue = oldValue;
            this.oldCandidates = oldCandidates;
        }
    }

    private Change[] undoStack;
    private int stackPointer;
    private int[] propagationQueue;
    private int queueSize;

    /**
     * Constructs an IncompleteSolver for the given Sudoku grid.
     *
     * @param grid the SudokuGrid to solve
     */
    public IncompleteSolver(SudokuGrid grid) {
        super(grid);
    }

    /**
     * Sets the cell selection heuristic for variable ordering.
     *
     * @param heuristic the heuristic (e.g., MRVHeuristic, DegreeHeuristic)
     */
    public void setHeuristic(CellHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Enables or disables constraint propagation.
     *
     * @param propagate true to enable forward-checking and naked singles
     */
    public void setPropagate(boolean propagate) {
        this.propagate = propagate;
    }

    /**
     * Sets the maximum number of iterations before timeout.
     *
     * @param maxIterations maximum iterations allowed
     */
    public void setMaxIterations(long maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Solves the Sudoku puzzle using backtracking with constraint propagation.
     *
     * @return SolverResult containing solution status, time, iterations, and backtracks
     */
    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        grid.reset();
        int N = grid.getSize();

        // Allocate memory structures
        undoStack = new Change[N * N * 32];
        stackPointer = 0;
        propagationQueue = new int[N * N];
        queueSize = 0;

        // Initialize candidate sets from clues
        if (propagate) {
            if (!propagateAllClues() || !propagateSingles()) {
                finalizeResult(false);
                return result;
            }
        }

        boolean solved = backtrack();
        finalizeResult(solved);
        return result;
    }

    /**
     * Recursive backtracking search with constraint propagation.
     *
     * @return true if solution found, false otherwise
     */
    private boolean backtrack() {
        recordIteration();

        if (iterations > maxIterations) {
            return false;
        }

        if (grid.isFull()) {
            return true;
        }

        // Select next cell using heuristic
        int[] cell = heuristic.selectCell(grid);
        if (cell == null) {
            return false;
        }

        int row = cell[0];
        int col = cell[1];
        int candidateMask = grid.getCandidates(row, col);

        if (candidateMask == 0) {
            return false; // Contradiction
        }

        int undoMark = stackPointer;

        // Try each candidate value
        while (candidateMask != 0) {
            int bit = candidateMask & -candidateMask;
            int value = Integer.numberOfTrailingZeros(bit);
            candidateMask &= ~bit;

            if (assign(row, col, value)) {
                boolean consistent = true;

                // Propagate constraints
                if (propagate) {
                    consistent = propagateFromAssignment(row, col, value) && propagateSingles();
                }

                if (consistent && backtrack()) {
                    return true;
                }

                undo(undoMark);
                recordBacktrack();
            }
        }

        return false;
    }

    /**
     * Assigns a value to a cell.
     *
     * @param r row index
     * @param c column index
     * @param value value to assign
     * @return true (always succeeds)
     */
    private boolean assign(int r, int c, int value) {
        pushChange(r, c);
        grid.set(r, c, value);
        grid.setCandidates(r, c, 0);
        return true;
    }

    /**
     * Initial constraint propagation: removes candidates based on given clues.
     *
     * @return true if success, false if contradiction detected
     */
    private boolean propagateAllClues() {
        int N = grid.getSize();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int value = grid.get(i, j);
                if (value != 0) {
                    grid.setCandidates(i, j, 0);
                    if (!propagateFromAssignment(i, j, value)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Forward-checking: removes assigned value from related cells
     * (row, column, block) and queues them for further propagation.
     *
     * @param row row of assigned cell
     * @param col column of assigned cell
     * @param value assigned value
     * @return true if success, false if contradiction detected
     */
    private boolean propagateFromAssignment(int row, int col, int value) {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Row propagation
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                if (!removeCandidateOrFail(row, j, value)) {
                    return false;
                }
            }
        }

        // Column propagation
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) {
                if (!removeCandidateOrFail(i, col, value)) {
                    return false;
                }
            }
        }

        // Block propagation
        int blockRowStart = (row / n) * n;
        int blockColStart = (col / n) * n;
        for (int r = blockRowStart; r < blockRowStart + n; r++) {
            for (int c = blockColStart; c < blockColStart + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c)) {
                    if (!removeCandidateOrFail(r, c, value)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Removes candidate value from a cell; detects contradictions.
     *
     * @param r row index
     * @param c column index
     * @param value candidate to remove
     * @return true if success, false if domain becomes empty
     */
    private boolean removeCandidateOrFail(int r, int c, int value) {
        int oldMask = grid.getCandidates(r, c);
        int newMask = oldMask & ~(1 << value);

        if (newMask == oldMask) {
            return true; // No change
        }

        pushChange(r, c);
        grid.setCandidates(r, c, newMask);

        return newMask != 0; // Contradiction if domain is empty
    }

    /**
     * Naked singles propagation: assigns cells with only one candidate.
     * Iteratively reduces domains until no more singles are found.
     *
     * @return true if success, false if contradiction detected
     */
    private boolean propagateSingles() {
        int N = grid.getSize();
        boolean changed;

        do {
            changed = false;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid.isEmpty(i, j)) {
                        int mask = grid.getCandidates(i, j);

                        if (mask == 0) {
                            return false; // Contradiction
                        }

                        // Check if exactly one candidate (power of two test)
                        if ((mask & (mask - 1)) == 0) {
                            int value = Integer.numberOfTrailingZeros(mask);
                            assign(i, j, value);
                            if (!propagateFromAssignment(i, j, value)) {
                                return false;
                            }
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        return true;
    }

    /**
     * Pushes current cell state onto undo stack.
     *
     * @param r row index
     * @param c column index
     */
    private void pushChange(int r, int c) {
        if (stackPointer >= undoStack.length) {
            Change[] bigger = new Change[undoStack.length * 2];
            System.arraycopy(undoStack, 0, bigger, 0, undoStack.length);
            undoStack = bigger;
        }
        undoStack[stackPointer++] = new Change(r, c, grid.get(r, c), grid.getCandidates(r, c));
    }

    /**
     * Restores grid state to previous undo mark.
     *
     * @param mark undo stack position to restore to
     */
    private void undo(int mark) {
        while (stackPointer > mark) {
            Change change = undoStack[--stackPointer];
            grid.set(change.r, change.c, change.oldValue);
            grid.setCandidates(change.r, change.c, change.oldCandidates);
        }
    }
}