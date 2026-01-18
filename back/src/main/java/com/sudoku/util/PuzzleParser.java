package com.sudoku.util;

import java.io.*;

import com.sudoku.model.SudokuGrid;

public class PuzzleParser {

    public static SudokuGrid parseFile(String path) throws IOException {
        return parseFile(path, 3);
    }

    public static SudokuGrid parseFile(String path, int blockSize) throws IOException {
        int size = blockSize * blockSize;
        int[][] data = new int[size][size];
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null && row < size) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("\\s+");
                for (int col = 0; col < Math.min(parts.length, size); col++) {
                    try {
                        data[row][col] = Integer.parseInt(parts[col]);
                    } catch (NumberFormatException e) {
                        data[row][col] = 0;
                    }
                }
                row++;
            }
        }
        return new SudokuGrid(data, blockSize);
    }
}


