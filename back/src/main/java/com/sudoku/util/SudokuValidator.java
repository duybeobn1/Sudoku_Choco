package com.sudoku.util;

import com.sudoku.model.SudokuGrid;

/**
 * Utility class for validating Sudoku grids and moves.
 */
public class SudokuValidator {

    public static boolean isValid(SudokuGrid grid) {
        int size = grid.getSize();
        int blockSize = grid.getBlockSize();

        for (int i = 0; i < size; i++) {
            if (!checkRow(grid, i, size)) return false;
            if (!checkCol(grid, i, size)) return false;
        }

        for (int r = 0; r < blockSize; r++) {
            for (int c = 0; c < blockSize; c++) {
                if (!checkBlock(grid, r, c, blockSize)) return false;
            }
        }
        return true;
    }

    private static boolean checkRow(SudokuGrid grid, int row, int size) {
        boolean[] seen = new boolean[size + 1];
        for (int j = 0; j < size; j++) {
            int val = grid.get(row, j);
            if (val != 0) {
                if (seen[val]) return false;
                seen[val] = true;
            }
        }
        return true;
    }

    private static boolean checkCol(SudokuGrid grid, int col, int size) {
        boolean[] seen = new boolean[size + 1];
        for (int i = 0; i < size; i++) {
            int val = grid.get(i, col);
            if (val != 0) {
                if (seen[val]) return false;
                seen[val] = true;
            }
        }
        return true;
    }

    private static boolean checkBlock(SudokuGrid grid, int blockRow, int blockCol, int blockSize) {
        int size = grid.getSize();
        boolean[] seen = new boolean[size + 1];
        int rStart = blockRow * blockSize;
        int cStart = blockCol * blockSize;

        for (int r = rStart; r < rStart + blockSize; r++) {
            for (int c = cStart; c < cStart + blockSize; c++) {
                int val = grid.get(r, c);
                if (val != 0) {
                    if (seen[val]) return false;
                    seen[val] = true;
                }
            }
        }
        return true;
    }
}
