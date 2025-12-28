package src.heuristic;

import src.model.SudokuGrid;

/**
 * Hybrid Heuristic combining MRV (Minimum Remaining Values) and Degree heuristic.
 * 
 * Strategy:
 * 1. Select the empty cell with the fewest possible candidates (MRV)
 * 2. If there's a tie in candidate count, select the cell that constrains 
 *    the most other empty cells (Degree heuristic)
 * 
 * This hybrid approach is very effective on both easy and hard Sudoku puzzles,
 * providing a good balance between search space reduction and computation cost.
 */
public class HybridHeuristic implements CellHeuristic {

    /**
     * Select the next cell to fill using the hybrid MRV + Degree strategy.
     * 
     * @param grid the current SudokuGrid state
     * @return an array [row, col] of the selected cell, or null if no cell to select
     */
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int n = grid.getBlockSize();
        
        int minCandidates = N + 1;
        int maxDegree = -1;
        int[] bestCell = null;

        // Iterate through all cells to find the best one
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.isEmpty(i, j)) {
                    int candidateCount = grid.countCandidates(i, j);
                    int degree = computeDegree(grid, i, j, n);

                    // Primary criterion: fewest candidates (MRV)
                    // Secondary criterion: highest degree (most constrained neighbors)
                    if (candidateCount < minCandidates || 
                        (candidateCount == minCandidates && degree > maxDegree)) {
                        
                        minCandidates = candidateCount;
                        maxDegree = degree;
                        bestCell = new int[]{i, j};
                    }
                }
            }
        }

        return bestCell;
    }

    /**
     * Compute the degree of a cell (number of related empty cells).
     * 
     * The degree is the count of empty cells that share a constraint with the given cell:
     * - Same row
     * - Same column
     * - Same 3×3 (or n×n) block
     * 
     * A high degree means the cell has many related empty cells, making it highly constraining.
     * 
     * @param grid the current SudokuGrid state
     * @param row the row index of the cell
     * @param col the column index of the cell
     * @param n the block size (3 for standard 9×9 Sudoku)
     * @return the degree (number of related empty cells)
     */
    private int computeDegree(SudokuGrid grid, int row, int col, int n) {
        int N = grid.getSize();
        int degree = 0;

        // Count empty cells in the same row (excluding the cell itself)
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) {
                degree++;
            }
        }

        // Count empty cells in the same column (excluding the cell itself)
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) {
                degree++;
            }
        }

        // Count empty cells in the same block (excluding the cell itself)
        int blockRow = (row / n) * n;
        int blockCol = (col / n) * n;
        
        for (int r = blockRow; r < blockRow + n; r++) {
            for (int c = blockCol; c < blockCol + n; c++) {
                // Avoid counting the cell itself and cells already counted
                if ((r != row || c != col) && grid.isEmpty(r, c)) {
                    degree++;
                }
            }
        }

        return degree;
    }
}
