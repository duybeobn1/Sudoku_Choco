package com.sudoku.heuristics;

import com.sudoku.model.SudokuGrid;

/**
 * Degree Heuristic.
 * Uses MRV as primary key and "Degree" (constraints on other unassigned variables) as tie-breaker.
 */
public class DegreeHeuristic implements CellHeuristic {

    @Override
    public int[] selectCell(SudokuGrid grid) {
        int size = grid.getSize();
        int blockSize = grid.getBlockSize();
        
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;
        int maxDegree = -1;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (!grid.isEmpty(i, j)) continue;

                int candidates = Integer.bitCount(grid.getCandidates(i, j));
                
                if (candidates == 0) continue; // Empty domain (dead end)
                if (candidates == 1) return new int[]{i, j}; // Optimize singletons

                if (candidates < minCandidates) {
                    minCandidates = candidates;
                    maxDegree = calculateDegree(grid, i, j, blockSize);
                    bestRow = i;
                    bestCol = j;
                } else if (candidates == minCandidates) {
                    int degree = calculateDegree(grid, i, j, blockSize);
                    if (degree > maxDegree) {
                        maxDegree = degree;
                        bestRow = i;
                        bestCol = j;
                    }
                }
            }
        }
        return (bestRow == -1) ? null : new int[]{bestRow, bestCol};
    }

    private int calculateDegree(SudokuGrid grid, int row, int col, int blockSize) {
        int degree = 0;
        int size = grid.getSize();
        boolean[] seen = new boolean[size * size];

        // Row neighbors
        for (int c = 0; c < size; c++) {
            if (c != col && grid.isEmpty(row, c)) {
                degree++;
                seen[row * size + c] = true;
            }
        }
        // Col neighbors
        for (int r = 0; r < size; r++) {
            if (r != row && grid.isEmpty(r, col) && !seen[r * size + col]) {
                degree++;
                seen[r * size + col] = true;
            }
        }
        // Block neighbors
        int startRow = (row / blockSize) * blockSize;
        int startCol = (col / blockSize) * blockSize;
        for (int r = startRow; r < startRow + blockSize; r++) {
            for (int c = startCol; c < startCol + blockSize; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c) && !seen[r * size + c]) {
                    degree++;
                }
            }
        }
        return degree;
    }
}
