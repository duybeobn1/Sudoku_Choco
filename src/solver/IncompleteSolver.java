package src.solver;

import src.heuristic.CellHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;

/**
 * Incomplete Solver using backtracking with forward-checking constraint propagation.
 * 
 * This solver maintains candidate sets (domains) for each cell using bitmasks
 * and performs efficient constraint propagation. It avoids deep grid copies
 * by using an undo stack, significantly improving performance.
 * 
 * Named "Incomplete" because it can be bounded by maxIterations, though the
 * underlying algorithm is complete (guarantees finding a solution if unbounded).
 */
public class IncompleteSolver extends SudokuSolver {

    private CellHeuristic heuristic = new MRVHeuristic();
    private boolean propagate = true;
    private long maxIterations = Long.MAX_VALUE;

    /**
     * Undo stack entry: records the state of a single cell before modification.
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
     * @param heuristic the heuristic to use (e.g., MRVHeuristic, DegreeHeuristic)
     */
    public void setHeuristic(CellHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Enables or disables constraint propagation.
     * 
     * @param propagate true to enable forward-checking and naked singles propagation
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
        // Allocate undo stack (generous size to avoid resizing during search)
        undoStack = new Change[N * N * 64];
        stackPointer = 0;

        // Initialize candidate sets based on initial clues
        if (propagate) {
            if (!propagateAllClues()) {
                finalizeResult(false);
                return result;
            }
            if (!propagateSingles()) {
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
     * @return true if a solution is found, false otherwise
     */
    private boolean backtrack() {
        recordIteration();
        
        // Check iteration limit (for bounded search)
        if (iterations > maxIterations) {
            return false;
        }

        // Base case: grid is complete and constraints are satisfied
        if (grid.isFull()) {
            return true;
        }

        // Select next cell using heuristic
        int[] cell = heuristic.selectCell(grid);
        if (cell == null) {
            return false; // No unassigned cell (should not occur if grid is not full)
        }

        int row = cell[0];
        int col = cell[1];
        int candidateMask = grid.getCandidates(row, col);

        // Contradiction: no candidates available
        if (candidateMask == 0) {
            return false;
        }

        // Save undo point
        int undoMark = stackPointer;

        // Iterate over all candidate values (using bitmask iteration)
        while (candidateMask != 0) {
            // Extract least significant bit (next candidate value)
            int bit = candidateMask & -candidateMask;
            int value = Integer.numberOfTrailingZeros(bit);
            candidateMask &= ~bit; // Remove this bit

            // Assign value to cell
            if (assign(row, col, value)) {
                boolean consistent = true;
                
                // Forward-checking: propagate constraints
                if (propagate) {
                    consistent = propagateFromAssignment(row, col, value) && propagateSingles();
                }

                // Recursively solve the rest
                if (consistent && backtrack()) {
                    return true; // Solution found
                }
            }

            // Backtrack: restore state to undo mark
            undo(undoMark);
            recordBacktrack();
        }

        return false; // No solution found from this state
    }

    /**
     * Assigns a value to a cell and updates its candidate set to empty.
     * 
     * @param r row index
     * @param c column index
     * @param value value to assign
     * @return true (always succeeds)
     */
    private boolean assign(int r, int c, int value) {
        pushChange(r, c);
        grid.set(r, c, value);
        grid.setCandidates(r, c, 0); // No more candidates for this cell
        return true;
    }

    /**
     * Initial constraint propagation: remove candidates based on all given clues.
     * 
     * @return true if propagation succeeds, false if a contradiction is detected
     */
    private boolean propagateAllClues() {
        int N = grid.getSize();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int value = grid.get(i, j);
                if (value != 0) { // Given clue
                    grid.setCandidates(i, j, 0);
                    if (!propagateFromAssignment(i, j, value)) {
                        return false; // Contradiction in initial state
                    }
                }
            }
        }
        return true;
    }

    /**
     * Forward-checking: removes an assigned value from all related cells
     * (same row, column, and block).
     * 
     * @param row row of the assigned cell
     * @param col column of the assigned cell
     * @param value assigned value
     * @return true if propagation succeeds, false if a contradiction is detected
     */
    private boolean propagateFromAssignment(int row, int col, int value) {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Remove value from all cells in the same row
        for (int j = 0; j < N; j++) {
            if (j == col || !grid.isEmpty(row, j)) continue;
            if (!removeCandidateOrFail(row, j, value)) {
                return false;
            }
        }

        // Remove value from all cells in the same column
        for (int i = 0; i < N; i++) {
            if (i == row || !grid.isEmpty(i, col)) continue;
            if (!removeCandidateOrFail(i, col, value)) {
                return false;
            }
        }

        // Remove value from all cells in the same block
        int blockRowStart = (row / n) * n;
        int blockColStart = (col / n) * n;
        for (int r = blockRowStart; r < blockRowStart + n; r++) {
            for (int c = blockColStart; c < blockColStart + n; c++) {
                if ((r == row && c == col) || !grid.isEmpty(r, c)) continue;
                if (!removeCandidateOrFail(r, c, value)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Removes a candidate value from a cell's domain and detects contradictions.
     * 
     * @param r row index
     * @param c column index
     * @param value candidate value to remove
     * @return true if removal succeeds, false if domain becomes empty (contradiction)
     */
    private boolean removeCandidateOrFail(int r, int c, int value) {
        int oldMask = grid.getCandidates(r, c);
        int newMask = oldMask & ~(1 << value);

        // No change needed
        if (newMask == oldMask) {
            return true;
        }

        // Save state and update
        pushChange(r, c);
        grid.setCandidates(r, c, newMask);

        // Detect immediate contradiction: empty domain
        return newMask != 0;
    }

    /**
     * Naked singles propagation: if a cell has only one candidate left,
     * assign that value and propagate further.
     * 
     * This is a powerful inference rule that significantly prunes the search space.
     * 
     * @return true if propagation succeeds, false if a contradiction is detected
     */
    private boolean propagateSingles() {
        int N = grid.getSize();
        boolean changed;

        do {
            changed = false;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (!grid.isEmpty(i, j)) continue;

                    int mask = grid.getCandidates(i, j);
                    
                    // Contradiction: no candidates left
                    if (mask == 0) {
                        return false;
                    }

                    // Check if exactly one candidate remains (power-of-two test)
                    if ((mask & (mask - 1)) == 0) {
                        int value = Integer.numberOfTrailingZeros(mask);
                        assign(i, j, value);
                        if (!propagateFromAssignment(i, j, value)) {
                            return false;
                        }
                        changed = true; // Continue scanning for more singles
                    }
                }
            }
        } while (changed);

        return true;
    }

    /**
     * Pushes the current state of a cell onto the undo stack.
     * 
     * @param r row index
     * @param c column index
     */
    private void pushChange(int r, int c) {
        // Grow stack if necessary (rare)
        if (stackPointer >= undoStack.length) {
            Change[] bigger = new Change[undoStack.length * 2];
            System.arraycopy(undoStack, 0, bigger, 0, undoStack.length);
            undoStack = bigger;
        }
        undoStack[stackPointer++] = new Change(r, c, grid.get(r, c), grid.getCandidates(r, c));
    }

    /**
     * Restores grid state to a previous undo mark.
     * 
     * @param mark undo stack pointer to restore to
     */
    private void undo(int mark) {
        while (stackPointer > mark) {
            Change change = undoStack[--stackPointer];
            grid.set(change.r, change.c, change.oldValue);
            grid.setCandidates(change.r, change.c, change.oldCandidates);
        }
    }
}
