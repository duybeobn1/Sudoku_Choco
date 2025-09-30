import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

public class SudokuSolver {

    public static int[][] readPuzzleFromFile(String filepath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filepath));
        int[][] puzzle = new int[9][9];
        for (int i = 0; i < 9; i++) {
            String[] tokens = lines.get(i).trim().split("\\s+");
            for (int j = 0; j < 9; j++) {
                puzzle[i][j] = Integer.parseInt(tokens[j]);
            }
        }
        return puzzle;
    }

    public static void main(String[] args) throws IOException {
        // Create model
        Model model = new Model("Sudoku");

        // Create 9x9 matrix of variables with domain 1..9
        IntVar[][] grid = model.intVarMatrix("grid", 9, 9, 1, 9);

        // Rows constraints
        for (int i = 0; i < 9; i++) {
            model.allDifferent(grid[i]).post();
        }

        // Columns constraints
        for (int j = 0; j < 9; j++) {
            IntVar[] column = new IntVar[9];
            for (int i = 0; i < 9; i++) {
                column[i] = grid[i][j];
            }
            model.allDifferent(column).post();
        }

        // Blocks constraints
        for (int blockRow = 0; blockRow < 3; blockRow++) {
            for (int blockCol = 0; blockCol < 3; blockCol++) {
                IntVar[] block = new IntVar[9];
                int idx = 0;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        block[idx++] = grid[blockRow * 3 + i][blockCol * 3 + j];
                    }
                }
                model.allDifferent(block).post();
            }
        }

        // Read Sudoku puzzle from file
        int[][] puzzle = readPuzzleFromFile("sudoku.txt");

        // Post clues as equality constraints
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (puzzle[i][j] != 0) {
                    grid[i][j].eq(puzzle[i][j]).post();
                }
            }
        }

        // Solve and print solution
        if (model.getSolver().solve()) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    System.out.print(grid[i][j].getValue() + " ");
                }
                System.out.println();
            }
        } else {
            System.out.println("No solution found.");
        }
    }
}
