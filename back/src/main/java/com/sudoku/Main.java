package com.sudoku;

import com.sudoku.api.SudokuApi;
import com.sudoku.model.SudokuGrid;
import com.sudoku.solver.CompleteSolver;
import com.sudoku.solver.IncompleteSolver;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        String mode = (args.length > 0) ? args[0].toLowerCase() : "api";

        try {
            switch (mode) {
                case "api":
                    System.out.println("Starting Sudoku API Server...");
                    SudokuApi.start();
                    break;
                    
                case "demo":
                    runDemo();
                    break;
                    
                default:
                    System.out.println("Usage: java back.Main [api|demo]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runDemo() {
        int[][] easy = {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };

        System.out.println("Running Incomplete Solver Demo...");
        SudokuGrid grid = new SudokuGrid(easy, 3);
        IncompleteSolver solver = new IncompleteSolver(grid);
        System.out.println(solver.solve().getSolution() != null ? "Solved!" : "Failed");
    }
}
