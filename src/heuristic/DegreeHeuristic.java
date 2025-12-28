package src.heuristic;

import src.model.SudokuGrid;

/**
 * Degree-based heuristic with tie-breaking.
 * Selects the empty cell with the fewest candidates.
 * If there's a tie, selects the cell that constrains the most other cells.
 */
public class DegreeHeuristic implements CellHeuristic {
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int n = grid.getBlockSize();
        int minCandidates = N + 1;
        int maxDegree = -1;
        int[] bestCell = null;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.isEmpty(i, j)) {
                    int candidateCount = grid.countCandidates(i, j);

                    if (candidateCount < minCandidates ||
                            (candidateCount == minCandidates && computeDegree(grid, i, j, n) > maxDegree)) {
                        minCandidates = candidateCount;
                        maxDegree = computeDegree(grid, i, j, n);
                        bestCell = new int[]{i, j};
                    }
                }
            }
        }

        return bestCell;
    }

    /**
     * Compute the degree of a cell (number of related empty cells).
     */
    private int computeDegree(SudokuGrid grid, int row, int col, int n) {
        int N = grid.getSize();
        int degree = 0;

        // Count empty cells in the same row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.isEmpty(row, j)) degree++;
        }

        // Count empty cells in the same column
        for (int i = 0; i < N; i++) {
            if (i != row && grid.isEmpty(i, col)) degree++;
        }

        // Count empty cells in the same block
        int blockRow = (row / n) * n;
        int blockCol = (col / n) * n;
        for (int r = blockRow; r < blockRow + n; r++) {
            for (int c = blockCol; c < blockCol + n; c++) {
                if ((r != row || c != col) && grid.isEmpty(r, c)) degree++;
            }
        }

        return degree;
    }
}
