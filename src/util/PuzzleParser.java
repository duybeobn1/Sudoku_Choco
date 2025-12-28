package src.util;

import src.model.SudokuGrid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Parser for Sudoku puzzles from various file formats.
 * Supports plain text format (space or newline separated) and MiniZinc format.
 */
public class PuzzleParser {
    /**
     * Parse a Sudoku puzzle from a plain text file.
     * Expected format: 9 lines of 9 space-separated integers (0 for empty cells).
     *
     * @param filepath the path to the puzzle file
     * @return a SudokuGrid with the parsed puzzle
     * @throws IOException if the file cannot be read
     */
    public static SudokuGrid parseFromFile(String filepath) throws IOException {
        return parseFromFile(filepath, 3);  // Default block size for 9x9 Sudoku
    }

    /**
     * Parse a Sudoku puzzle from a plain text file with custom block size.
     *
     * @param filepath the path to the puzzle file
     * @param blockSize the block size (3 for 9x9, 4 for 16x16, etc.)
     * @return a SudokuGrid with the parsed puzzle
     * @throws IOException if the file cannot be read
     */
    public static SudokuGrid parseFromFile(String filepath, int blockSize) throws IOException {
        int N = blockSize * blockSize;
        int[][] grid = new int[N][N];

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int row = 0;

            while ((line = reader.readLine()) != null && row < N) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                for (int col = 0; col < Math.min(parts.length, N); col++) {
                    try {
                        grid[row][col] = Integer.parseInt(parts[col]);
                    } catch (NumberFormatException e) {
                        grid[row][col] = 0;
                    }
                }
                row++;
            }
        }

        return new SudokuGrid(grid, blockSize);
    }

    /**
     * Parse a Sudoku puzzle from MiniZinc format file.
     * Handles the format produced by hakank's Sudoku problems.
     *
     * @param file the MiniZinc file
     * @return a SudokuGrid with the parsed puzzle
     * @throws IOException if the file cannot be read
     */
    public static SudokuGrid parseFromMiniZinc(File file) throws IOException {
        StringBuilder dataString = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("%")) continue;

                // Convert MiniZinc empty cell '_' to '0'
                line = line.replace("_", "0");

                // Remove dimension definitions
                line = line.replaceAll("\\d+\\.\\.[a-zA-Z0-9]+", " ");

                // Skip metadata lines
                if (line.matches(".*[a-zA-Z0-9]+\\s*=\\s*\\d+;")) continue;

                // Remove "array2d" keyword
                line = line.replace("array2d", " ");

                // Extract only digits
                String cleanLine = line.replaceAll("[^0-9]", " ");
                dataString.append(cleanLine).append(" ");
            }
        }

        // Tokenize and convert
        int[] data = Arrays.stream(dataString.toString().trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();

        // Auto-detect size
        int totalCells = data.length;
        int N = (int) Math.sqrt(totalCells);
        int n = (int) Math.sqrt(N);

        if (N * N != totalCells) {
            throw new IOException("Invalid data count: " + totalCells + " numbers. Not a valid Sudoku square.");
        }

        // Convert flat array to 2D grid
        int[][] grid = new int[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                grid[i][j] = data[i * N + j];
            }
        }

        return new SudokuGrid(grid, n);
    }

    /**
     * Create a Sudoku grid from a 2D array directly.
     *
     * @param gridData the 2D array representing the puzzle
     * @param blockSize the block size
     * @return a SudokuGrid
     */
    public static SudokuGrid createFromArray(int[][] gridData, int blockSize) {
        return new SudokuGrid(gridData, blockSize);
    }

    /**
     * Create an empty Sudoku grid.
     *
     * @param blockSize the block size (3 for standard 9x9)
     * @return an empty SudokuGrid
     */
    public static SudokuGrid createEmpty(int blockSize) {
        int N = blockSize * blockSize;
        int[][] grid = new int[N][N];
        return new SudokuGrid(grid, blockSize);
    }
}
