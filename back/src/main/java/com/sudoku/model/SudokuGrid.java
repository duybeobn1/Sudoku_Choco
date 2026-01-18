package com.sudoku.model;

import java.util.Arrays;

/**
 * Represents a Sudoku grid of arbitrary size (N x N).
 * Supports standard 9x9 (block size 3) as well as other sizes like 16x16 (block size 4).
 */
public class SudokuGrid {

    private final int[][] grid;
    private final int[][] originalGrid;
    private final int blockSize; // e.g., 3 for a 9x9 grid
    private final int size;      // e.g., 9 for a 9x9 grid
    private int[][] candidates; // Bitmask for incomplete solver candidates

    /**
     * Constructs a grid from a 2D array.
     *
     * @param gridData  The 2D array representing the grid.
     * @param blockSize The block size (e.g., 3 for 9x9).
     */
    public SudokuGrid(int[][] gridData, int blockSize) {
        this.blockSize = blockSize;
        this.size = blockSize * blockSize;
        this.grid = deepCopy(gridData);
        this.originalGrid = deepCopy(gridData);
        this.candidates = initializeCandidates();
    }

    /**
     * Copy constructor.
     */
    public SudokuGrid(SudokuGrid other) {
        this.blockSize = other.blockSize;
        this.size = other.size;
        this.grid = deepCopy(other.grid);
        this.originalGrid = deepCopy(other.originalGrid);
        this.candidates = deepCopyCandidates(other.candidates);
    }

    private int[][] initializeCandidates() {
        int[][] cand = new int[size][size];
        int fullMask = (1 << (size + 1)) - 2; // Bits 1..N set to 1
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == 0) {
                    cand[i][j] = fullMask;
                } else {
                    cand[i][j] = 0;
                }
            }
        }
        return cand;
    }

    public int get(int row, int col) {
        return grid[row][col];
    }

    public void set(int row, int col, int value) {
        grid[row][col] = value;
        if (value != 0) {
            candidates[row][col] = 0;
        }
    }

    public boolean isEmpty(int row, int col) {
        return grid[row][col] == 0;
    }

    public int getCandidates(int row, int col) {
        return candidates[row][col];
    }

    public void setCandidates(int row, int col, int mask) {
        candidates[row][col] = mask;
    }

    public int getSize() {
        return size;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int[][] getGrid() {
        return deepCopy(grid);
    }

    public boolean isFull() {
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    public void reset() {
        for (int i = 0; i < size; i++) {
            System.arraycopy(originalGrid[i], 0, grid[i], 0, size);
        }
        this.candidates = initializeCandidates();
    }

    private int[][] deepCopy(int[][] arr) {
        int[][] copy = new int[arr.length][];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = arr[i].clone();
        }
        return copy;
    }

    private int[][] deepCopyCandidates(int[][] cand) {
        int[][] copy = new int[cand.length][];
        for (int i = 0; i < cand.length; i++) {
            copy[i] = cand[i].clone();
        }
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i % blockSize == 0 && i != 0) sb.append("\n");
            for (int j = 0; j < size; j++) {
                if (j % blockSize == 0 && j != 0) sb.append(" ");
                sb.append(grid[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
