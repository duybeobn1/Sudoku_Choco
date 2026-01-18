package src.heuristic;

import src.model.SudokuGrid;

/**
 * Optimized Degree Heuristic for variable ordering.
 *
 * Combines Minimum Remaining Values (MRV) with Degree tie-breaking.
 * - Primary criterion: cell with fewest candidates (fail-first)
 * - Tie-breaker: cell constraining most other empty cells (highest degree)
 *
 * Degree = count of empty cells in same row, column, and block.
 * More constraining cells are prioritized for earlier constraint propagation.
 */
public class DegreeHeuristic implements CellHeuristic {

    /**
     * Selects the next cell to fill using MRV with Degree tie-breaking.
     *
     * @param grid the current Sudoku grid state
     * @return int array [row, col] of selected cell, or null if no empty cell exists
     */
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int n = grid.getBlockSize();
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;
        int maxDegree = -1;

        // Scan grid to find best cell
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!grid.isEmpty(i, j)) {
                    continue;
                }

                int candidateCount = Integer.bitCount(grid.getCandidates(i, j));

                // Early exit optimization
                if (candidateCount == 0) {
                    continue; // Skip empty domains
                }
                if (candidateCount == 1) {
                    return new int[]{i, j}; // Can't do better than 1 candidate
                }

                // Compute degree only when needed for comparison
                if (candidateCount < minCandidates) {
                    minCandidates = candidateCount;
                    maxDegree = computeDegree(grid, i, j, n);
                    bestRow = i;
                    bestCol = j;
                } else if (candidateCount == minCandidates) {
                    // Tie-break by degree
                    int degree = computeDegree(grid, i, j, n);
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

    /**
     * Computes degree of a cell: count of empty cells in row, column, and block.
     *
     * Degree represents how many other cells are constrained by this cell,
     * so higher degree cells tend to lead to faster constraint propagation.
     *
     * @param grid the Sudoku grid
     * @param row row index of cell
     * @param col column index of cell
     * @param n block size
     * @return degree (number of constrained empty cells)
     */
    private int computeDegree(SudokuGrid grid, int row, int col, int n) {
        int N = grid.getSize();
        int degree = 0;
        boolean[] counted = new boolean[N * N]; // Avoid double-counting cells at intersections

        // Count empty cells in row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                degree++;
                counted[row * N + j] = true;
            }
        }

        // Count empty cells in column (excluding already counted)
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col) && !counted[i * N + col]) {
                degree++;
                counted[i * N + col] = true;
            }
        }

        // Count empty cells in block (excluding already counted)
        int blockRowStart = (row / n) * n;
        int blockColStart = (col / n) * n;
        for (int r = blockRowStart; r < blockRowStart + n; r++) {
            for (int c = blockColStart; c < blockColStart + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c) && !counted[r * N + c]) {
                    degree++;
                }
            }
        }

        return degree;
    }
}
