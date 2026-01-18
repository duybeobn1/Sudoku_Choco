package com.sudoku.heuristics;

import com.sudoku.model.SudokuGrid;

/**
 * Minimum Remaining Values (MRV) Heuristic.
 * Selects the cell with the fewest valid candidates.
 */
public class MRVHeuristic implements CellHeuristic {

    @Override
    public int[] selectCell(SudokuGrid grid) {
        int size = grid.getSize();
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid.isEmpty(i, j)) {
                    int count = Integer.bitCount(grid.getCandidates(i, j));
                    if (count < minCandidates) {
                        minCandidates = count;
                        bestRow = i;
                        bestCol = j;
                        // Optimization: Cannot do better than 1 candidate
                        if (minCandidates == 1) return new int[]{bestRow, bestCol};
                    }
                }
            }
        }
        return (bestRow == -1) ? null : new int[]{bestRow, bestCol};
    }
}
