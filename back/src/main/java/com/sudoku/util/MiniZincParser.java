package com.sudoku.util;

import java.io.*;
import java.util.Arrays;

/**
 * Parser for MiniZinc format Sudoku files.
 * Handles the format produced by hakank's Sudoku problems.
 */
public class MiniZincParser {

    /**
     * Parse a MiniZinc file and return a SudokuInstance with metadata.
     *
     * @param file the MiniZinc file
     * @param difficulty the difficulty level
     * @return a SudokuInstance containing the parsed data
     * @throws IOException if the file cannot be read
     */
    public static SudokuInstance parse(File file, String difficulty) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder dataString = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("%")) continue;
            if (line.startsWith("n") && line.contains("=")) continue;

            String clean = line.replace("_", "0").replaceAll("[^0-9]", " ");
            dataString.append(clean).append(" ");
        }
        reader.close();

        int[] data = Arrays.stream(dataString.toString().trim().split("\\s+"))
            .filter(s -> !s.isEmpty())
            .mapToInt(Integer::parseInt)
            .toArray();

        int totalCells = data.length;
        int N = (int) Math.sqrt(totalCells);

        // Logique de fallback pour taille
         if (N * N != totalCells) {
            int[] sizes = {9, 16, 25};
            for (int s : sizes) {
                if (totalCells >= s*s && (totalCells - s*s) < 10) {
                    N = s;
                    int[] cleanData = new int[N*N];
                    System.arraycopy(data, totalCells - (N*N), cleanData, 0, N*N);
                    data = cleanData;
                    break;
                }
            }
        }

        int n = (int) Math.sqrt(N);
        return new SudokuInstance(file.getName(), difficulty, n, data);
    }

    /**
     * Data class representing a Sudoku instance with metadata.
     */
    public static class SudokuInstance {
        public String name;
        public String difficulty;
        public int n; // Block size (3)
        public int[][] grid;

        public SudokuInstance(String name, String difficulty, int n, int[] flatData) {
            this.name = name;
            this.difficulty = difficulty;
            this.n = n;
            int N = n * n;
            this.grid = new int[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    this.grid[i][j] = flatData[i * N + j];
                }
            }
        }
    }
}