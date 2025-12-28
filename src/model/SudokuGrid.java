package src.model;

/**
 * Represents a Sudoku grid with support for arbitrary sizes (9x9, 16x16, etc.).
 * Provides grid manipulation, validation, and constraint tracking.
 */
public class SudokuGrid {
    private int[][] grid;
    private int[][] originalGrid;
    private int n;      // Block size (3 for 9x9, 4 for 16x16)
    private int N;      // Grid size (9 for 9x9, 16 for 16x16)
    private int[][] candidates;  // For incomplete solver: possible values for each cell

    /**
     * Constructor for SudokuGrid.
     *
     * @param gridData the 2D array representing the Sudoku grid
     * @param blockSize the block size (3 for standard Sudoku)
     */
    public SudokuGrid(int[][] gridData, int blockSize) {
        this.n = blockSize;
        this.N = n * n;
        this.grid = deepCopy(gridData);
        this.originalGrid = deepCopy(gridData);
        this.candidates = initializeCandidates();
    }

    /**
     * Copy constructor.
     */
    public SudokuGrid(SudokuGrid other) {
        this.n = other.n;
        this.N = other.N;
        this.grid = deepCopy(other.grid);
        this.originalGrid = deepCopy(other.originalGrid);
        this.candidates = deepCopyCandidates(other.candidates);
    }

    /**
     * Initialize the candidates for each empty cell.
     * Initially, each empty cell has all values 1..N as candidates.
     */
    private int[][] initializeCandidates() {
        int[][] cand = new int[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] == 0) {
                    cand[i][j] = (1 << (N + 1)) - 2;  // Bitmask: all values 1..N
                } else {
                    cand[i][j] = 0;  // No candidates for filled cells
                }
            }
        }
        return cand;
    }

    /**
     * Get the value at position (row, col).
     */
    public int get(int row, int col) {
        return grid[row][col];
    }

    /**
     * Set the value at position (row, col).
     */
    public void set(int row, int col, int value) {
        grid[row][col] = value;
        if (value != 0) {
            candidates[row][col] = 0;
        }
    }

    /**
     * Check if a cell is empty (contains 0).
     */
    public boolean isEmpty(int row, int col) {
        return grid[row][col] == 0;
    }

    /**
     * Check if a cell is given (from the original puzzle).
     */
    public boolean isGiven(int row, int col) {
        return originalGrid[row][col] != 0;
    }

    /**
     * Get the candidate set for a cell (as bitmask).
     */
    public int getCandidates(int row, int col) {
        return candidates[row][col];
    }

    /**
     * Set the candidate set for a cell (as bitmask).
     */
    public void setCandidates(int row, int col, int cand) {
        candidates[row][col] = cand;
    }

    /**
     * Count the number of candidates for a cell.
     */
    public int countCandidates(int row, int col) {
        return Integer.bitCount(candidates[row][col]);
    }

    /**
     * Check if a specific value is a candidate for a cell.
     */
    public boolean isCandidate(int row, int col, int value) {
        return (candidates[row][col] & (1 << value)) != 0;
    }

    /**
     * Remove a candidate from a cell.
     */
    public boolean removeCandidate(int row, int col, int value) {
        int oldCand = candidates[row][col];
        candidates[row][col] &= ~(1 << value);
        return oldCand != candidates[row][col];
    }

    /**
     * Get the grid size (e.g., 9 for a 9x9 Sudoku).
     */
    public int getSize() {
        return N;
    }

    /**
     * Get the block size (e.g., 3 for a 9x9 Sudoku).
     */
    public int getBlockSize() {
        return n;
    }

    /**
     * Get a copy of the grid.
     */
    public int[][] getGrid() {
        return deepCopy(grid);
    }

    /**
     * Get a copy of the original grid (with clues only).
     */
    public int[][] getOriginalGrid() {
        return deepCopy(originalGrid);
    }

    /**
     * Check if the grid is completely filled.
     */
    public boolean isFull() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] == 0) return false;
            }
        }
        return true;
    }

    /**
     * Count the number of given clues.
     */
    public int countClues() {
        int count = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (originalGrid[i][j] != 0) count++;
            }
        }
        return count;
    }

    /**
     * Print the grid to console.
     */
    public void print() {
        for (int i = 0; i < N; i++) {
            if (i % n == 0 && i != 0) System.out.println();
            for (int j = 0; j < N; j++) {
                if (j % n == 0 && j != 0) System.out.print("  ");
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Reset the grid to its original state.
     */
    public void reset() {
        this.grid = deepCopy(originalGrid);
        this.candidates = initializeCandidates();
    }

    /**
     * Create a deep copy of a 2D integer array.
     */
    private int[][] deepCopy(int[][] arr) {
        int[][] copy = new int[arr.length][];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = arr[i].clone();
        }
        return copy;
    }

    /**
     * Create a deep copy of the candidates array.
     */
    private int[][] deepCopyCandidates(int[][] cand) {
        int[][] copy = new int[cand.length][];
        for (int i = 0; i < cand.length; i++) {
            copy[i] = cand[i].clone();
        }
        return copy;
    }
}
