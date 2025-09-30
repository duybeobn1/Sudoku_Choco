import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class SudokuSolver {
    public static void main(String[] args) {
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

        // Sample Sudoku clues (0 means unknown)
        int[][] puzzle = {
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
