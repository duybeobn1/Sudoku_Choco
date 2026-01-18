package com.sudoku.solver;

import com.sudoku.heuristics.CellHeuristic;
import com.sudoku.heuristics.MRVHeuristic;
import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;

/**
 * Optimized Incomplete Solver using backtracking with Forward Checking and Naked Singles propagation.
 * Based on your robust legacy implementation with an Undo Stack.
 */
public class IncompleteSolver extends SudokuSolver {

    private CellHeuristic heuristic = new MRVHeuristic();
    private boolean propagate = true;
    private long maxIterations = 200_000; // Limit to avoid freezing on hard instances

    // Undo stack for fast backtracking
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

    public IncompleteSolver(SudokuGrid grid) {
        super(grid);
    }

    public void setHeuristic(CellHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    public void setPropagate(boolean propagate) {
        this.propagate = propagate;
    }

    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        
        // Initialize memory
        int N = grid.getSize();
        undoStack = new Change[N * N * 50]; // Large enough buffer
        stackPointer = 0;

        // Initial Propagation
        if (propagate) {
            if (!propagateAllClues()) {
                finalizeResult(false);
                return result;
            }
        }

        boolean solved = backtrack();
        finalizeResult(solved);
        return result;
    }

    private boolean backtrack() {
        iterations++;
        if (iterations > maxIterations) return false;

        if (grid.isFull()) return true;

        // Heuristic selection
        int[] cell = heuristic.selectCell(grid);
        if (cell == null) return false;

        int row = cell[0];
        int col = cell[1];
        int candidateMask = grid.getCandidates(row, col);

        if (candidateMask == 0) return false; // Dead end

        int undoMark = stackPointer;

        // Try candidates
        while (candidateMask != 0) {
            int bit = candidateMask & -candidateMask;
            int value = Integer.numberOfTrailingZeros(bit);
            candidateMask &= ~bit;

            // Try assignment
            if (assign(row, col, value)) {
                boolean consistent = true;
                
                // Propagate
                if (propagate) {
                    consistent = propagateFromAssignment(row, col, value) && propagateSingles();
                }

                if (consistent) {
                     if (backtrack()) return true;
                }
            }

            // Backtrack: Undo all changes made since undoMark
            undo(undoMark);
            backtracks++;
        }
        
        return false;
    }

    // --- Core Operations ---

    private boolean assign(int r, int c, int value) {
        pushChange(r, c);
        grid.set(r, c, value);
        grid.setCandidates(r, c, 0); // Assigned cells have no candidates
        return true;
    }

    private void pushChange(int r, int c) {
        if (stackPointer >= undoStack.length) {
            // Resize if needed
            Change[] bigger = new Change[undoStack.length * 2];
            System.arraycopy(undoStack, 0, bigger, 0, undoStack.length);
            undoStack = bigger;
        }
        undoStack[stackPointer++] = new Change(r, c, grid.get(r, c), grid.getCandidates(r, c));
    }

    private void undo(int mark) {
        while (stackPointer > mark) {
            Change change = undoStack[--stackPointer];
            grid.set(change.r, change.c, change.oldValue);
            grid.setCandidates(change.r, change.c, change.oldCandidates);
        }
    }

    // --- Propagation Logic ---

    private boolean propagateAllClues() {
        int N = grid.getSize();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int value = grid.get(i, j);
                if (value != 0) {
                    grid.setCandidates(i, j, 0);
                    if (!propagateFromAssignment(i, j, value)) return false;
                }
            }
        }
        // Also run singles initially
        return propagateSingles();
    }

    private boolean propagateFromAssignment(int row, int col, int value) {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                if (!removeCandidateOrFail(row, j, value)) return false;
            }
        }
        // Col
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) {
                if (!removeCandidateOrFail(i, col, value)) return false;
            }
        }
        // Block
        int br = (row / n) * n;
        int bc = (col / n) * n;
        for (int r = br; r < br + n; r++) {
            for (int c = bc; c < bc + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c)) {
                    if (!removeCandidateOrFail(r, c, value)) return false;
                }
            }
        }
        return true;
    }

    private boolean removeCandidateOrFail(int r, int c, int value) {
        int oldMask = grid.getCandidates(r, c);
        int newMask = oldMask & ~(1 << value);
        
        if (newMask == oldMask) return true; // No change

        pushChange(r, c);
        grid.setCandidates(r, c, newMask);
        
        return newMask != 0; // Fail if domain empty
    }

    private boolean propagateSingles() {
        int N = grid.getSize();
        boolean changed;
        
        // Loop until stable (no new singles found)
        // Ideally we would use a queue, but this loop is simple and effective for singles
        do {
            changed = false;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid.isEmpty(i, j)) {
                        int mask = grid.getCandidates(i, j);
                        if (mask == 0) return false;

                        // Check if power of two (single candidate)
                        if ((mask & (mask - 1)) == 0) {
                            int value = Integer.numberOfTrailingZeros(mask);
                            assign(i, j, value);
                            if (!propagateFromAssignment(i, j, value)) return false;
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);
        
        return true;
    }
}
