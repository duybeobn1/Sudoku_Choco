package src.heuristic;

import src.model.SudokuGrid;

/**
 * Minimum Remaining Values (MRV) heuristic for variable ordering.
 * 
 * Selects the empty cell with the fewest possible candidate values.
 * This heuristic is also known as the "fail-first" principle, as it
 * tends to detect contradictions earlier in the search tree.
 */
public class MRVHeuristic implements CellHeuristic {

    /**
     * Selects the next cell to fill using the MRV heuristic.
     * 
     * @param grid the current Sudoku grid state
     * @return int array [row, col] of the selected cell, or null if no empty cell exists
     */
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;

        // Scan all cells to find the one with minimum candidates
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!grid.isEmpty(i, j)) continue;

                int candidateCount = Integer.bitCount(grid.getCandidates(i, j));
                
                if (candidateCount < minCandidates) {
                    minCandidates = candidateCount;
                    bestRow = i;
                    bestCol = j;
                    
                    // Early exit optimization: if only 1 candidate, can't do better
                    if (minCandidates == 1) {
                        return new int[]{bestRow, bestCol};
                    }
                }
            }
        }

        return (bestRow == -1) ? null : new int[]{bestRow, bestCol};
    }
}
