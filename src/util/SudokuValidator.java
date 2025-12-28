package src.util;

import src.model.SudokuGrid;

/**
 * Sudoku validation utilities.
 * Provides methods to check if a grid or placement is valid according to Sudoku rules.
 */
public class SudokuValidator {
    /**
     * Check if the entire grid is valid (no conflicts).
     * A valid grid has no duplicate values in any row, column, or block.
     */
    public static boolean isValid(SudokuGrid grid) {
        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Check rows
        for (int i = 0; i < N; i++) {
            if (!isRowValid(grid, i)) return false;
        }

        // Check columns
        for (int j = 0; j < N; j++) {
            if (!isColumnValid(grid, j)) return false;
        }

        // Check blocks
        for (int blockRow = 0; blockRow < n; blockRow++) {
            for (int blockCol = 0; blockCol < n; blockCol++) {
                if (!isBlockValid(grid, blockRow, blockCol)) return false;
            }
        }

        return true;
    }

    /**
     * Check if a value can be placed at position (row, col).
     */
    public static boolean isValidPlacement(SudokuGrid grid, int row, int col, int value) {
        if (!grid.isEmpty(row, col)) return false;

        int N = grid.getSize();
        int n = grid.getBlockSize();

        // Check row
        for (int j = 0; j < N; j++) {
            if (j != col && grid.get(row, j) == value) return false;
        }

        // Check column
        for (int i = 0; i < N; i++) {
            if (i != row && grid.get(i, col) == value) return false;
        }

        // Check block
        int blockRow = (row / n) * n;
        int blockCol = (col / n) * n;
        for (int r = blockRow; r < blockRow + n; r++) {
            for (int c = blockCol; c < blockCol + n; c++) {
                if ((r != row || c != col) && grid.get(r, c) == value) return false;
            }
        }

        return true;
    }

    /**
     * Check if a row is valid.
     */
    private static boolean isRowValid(SudokuGrid grid, int row) {
        int N = grid.getSize();
        boolean[] seen = new boolean[N + 1];

        for (int j = 0; j < N; j++) {
            int value = grid.get(row, j);
            if (value != 0) {
                if (seen[value]) return false;
                seen[value] = true;
            }
        }

        return true;
    }

    /**
     * Check if a column is valid.
     */
    private static boolean isColumnValid(SudokuGrid grid, int col) {
        int N = grid.getSize();
        boolean[] seen = new boolean[N + 1];

        for (int i = 0; i < N; i++) {
            int value = grid.get(i, col);
            if (value != 0) {
                if (seen[value]) return false;
                seen[value] = true;
            }
        }

        return true;
    }

    /**
     * Check if a block is valid.
     */
    private static boolean isBlockValid(SudokuGrid grid, int blockRow, int blockCol) {
        int n = grid.getBlockSize();
        int N = grid.getSize();
        boolean[] seen = new boolean[N + 1];

        int startRow = blockRow * n;
        int startCol = blockCol * n;

        for (int r = startRow; r < startRow + n; r++) {
            for (int c = startCol; c < startCol + n; c++) {
                int value = grid.get(r, c);
                if (value != 0) {
                    if (seen[value]) return false;
                    seen[value] = true;
                }
            }
        }

        return true;
    }
}
