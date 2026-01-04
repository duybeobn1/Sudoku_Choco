package src.heuristic;

import src.model.SudokuGrid;

/**
 * Degree heuristic for variable ordering with MRV tie-breaking.
 * 
 * Primary criterion: selects the cell with the fewest candidates (MRV).
 * Tie-breaker: among cells with equal candidates, selects the one that
 * constrains the most other empty cells (highest degree).
 * 
 * The degree of a cell is the number of empty cells in its row, column, and block.
 */
public class DegreeHeuristic implements CellHeuristic {

    /**
     * Selects the next cell to fill using the Degree heuristic.
     * 
     * @param grid the current Sudoku grid state
     * @return int array [row, col] of the selected cell, or null if no empty cell exists
     */
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int n = grid.getBlockSize();
        int bestRow = -1;
        int bestCol = -1;
        int minCandidates = Integer.MAX_VALUE;
        int maxDegree = -1;

        // Scan all cells to find the best candidate
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (!grid.isEmpty(i, j)) continue;

                int candidateCount = Integer.bitCount(grid.getCandidates(i, j));
                int degree = computeDegree(grid, i, j, n);

                // Primary: fewer candidates (MRV)
                // Secondary: higher degree (more constraining)
                if (candidateCount < minCandidates ||
                    (candidateCount == minCandidates && degree > maxDegree)) {
                    minCandidates = candidateCount;
                    maxDegree = degree;
                    bestRow = i;
                    bestCol = j;
                }
            }
        }

        return (bestRow == -1) ? null : new int[]{bestRow, bestCol};
    }

    /**
     * Computes the degree of a cell: the number of related empty cells.
     * 
     * Related cells are those in the same row, column, or block.
     * 
     * @param grid the Sudoku grid
     * @param row row index of the cell
     * @param col column index of the cell
     * @param n block size
     * @return degree (number of constrained empty cells)
     */
    private int computeDegree(SudokuGrid grid, int row, int col, int n) {
        int N = grid.getSize();
        int degree = 0;

        // Count empty cells in the same row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                degree++;
            }
        }

        // Count empty cells in the same column
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) {
                degree++;
            }
        }

        // Count empty cells in the same block
        int blockRowStart = (row / n) * n;
        int blockColStart = (col / n) * n;
        for (int r = blockRowStart; r < blockRowStart + n; r++) {
            for (int c = blockColStart; c < blockColStart + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c)) {
                    degree++;
                }
            }
        }

        return degree;
    }
}
