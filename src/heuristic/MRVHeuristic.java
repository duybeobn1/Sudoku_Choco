package src.heuristic;

import src.model.SudokuGrid;

/**
 * Minimum Remaining Values (MRV) Heuristic - already optimized.
 *
 * Selects the empty cell with the fewest candidate values.
 * This fail-first strategy rapidly prunes the search space by detecting
 * contradictions earlier in the search tree.
 *
 * Features:
 * - O(N²) scan with early exit on minimum (1 candidate)
 * - Efficient bitmask-based candidate counting via Integer.bitCount()
 * - No redundant computations
 */
public class MRVHeuristic implements CellHeuristic {

    /**
     * Selects the next cell to fill using the MRV heuristic.
     *
     * @param grid the current Sudoku grid state
     * @return int array [row, col] of selected cell, or null if no empty cell exists
     */
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;

        // Scan all cells to find minimum
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!grid.isEmpty(i, j)) {
                    continue;
                }

                int candidateCount = Integer.bitCount(grid.getCandidates(i, j));

                if (candidateCount < minCandidates) {
                    minCandidates = candidateCount;
                    bestRow = i;
                    bestCol = j;

                    // Early exit: best possible value found
                    if (minCandidates == 1) {
                        return new int[]{bestRow, bestCol};
                    }
                }
            }
        }

        return (bestRow == -1) ? null : new int[]{bestRow, bestCol};
    }
}
