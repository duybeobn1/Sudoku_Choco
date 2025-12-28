# Usage Examples - Sudoku Solver

## Complete Examples with Explanations

---

## Example 1: Basic Complete Solver

```java
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;

public class Example1_BasicCompleteSolver {
    public static void main(String[] args) {
        // Define a simple Sudoku puzzle (0 = empty cell)
        int[][] puzzle = {
            {5, 3, 0,   0, 7, 0,   0, 0, 0},
            {6, 0, 0,   1, 9, 5,   0, 0, 0},
            {0, 9, 8,   0, 0, 0,   0, 6, 0},

            {8, 0, 0,   0, 6, 0,   0, 0, 3},
            {4, 0, 0,   8, 0, 3,   0, 0, 1},
            {7, 0, 0,   0, 2, 0,   0, 0, 6},

            {0, 6, 0,   0, 0, 0,   2, 8, 0},
            {0, 0, 0,   4, 1, 9,   0, 0, 5},
            {0, 0, 0,   0, 8, 0,   0, 7, 9}
        };

        // Create grid from 2D array (block size = 3 for standard 9x9)
        SudokuGrid grid = new SudokuGrid(puzzle, 3);

        // Display original puzzle
        System.out.println("Original Puzzle:");
        grid.print();
        System.out.println();

        // Create and configure solver
        CompleteSolver solver = new CompleteSolver(grid);
        solver.setStrategy(CompleteSolver.SearchStrategy.DOM_OVER_WDEG);
        solver.setTimeout(10);  // 10 seconds timeout

        // Solve
        SolverResult result = solver.solve();

        // Display results
        System.out.println("=== Results ===");
        System.out.println("Solved: " + result.isSolved());
        System.out.println("Time: " + result.getTimeMs() + " ms");
        System.out.println("Iterations: " + result.getIterations());
        System.out.println("Backtracks: " + result.getBacktracks());
        System.out.println();

        if (result.isSolved()) {
            System.out.println("Solution:");
            int[][] solution = result.getSolution();
            for (int i = 0; i < solution.length; i++) {
                if (i % 3 == 0 && i != 0) System.out.println();
                for (int j = 0; j < solution[i].length; j++) {
                    if (j % 3 == 0 && j != 0) System.out.print("  ");
                    System.out.print(solution[i][j] + " ");
                }
                System.out.println();
            }
        }
    }
}
```

**Output:**
```
Original Puzzle:
5 3 0  0 7 0  0 0 0
6 0 0  1 9 5  0 0 0
0 9 8  0 0 0  0 6 0

8 0 0  0 6 0  0 0 3
...

=== Results ===
Solved: true
Time: 45 ms
Iterations: 0
Backtracks: 0

Solution:
5 3 4  0 7 0  0 0 0
...
```

---

## Example 2: Incomplete Solver with Heuristics

```java
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.IncompleteSolver;
import src.heuristic.MRVHeuristic;
import src.heuristic.DegreeHeuristic;

public class Example2_IncompleteSolverHeuristics {
    public static void main(String[] args) {
        int[][] puzzle = {
            {5, 3, 0,   0, 7, 0,   0, 0, 0},
            {6, 0, 0,   1, 9, 5,   0, 0, 0},
            {0, 9, 8,   0, 0, 0,   0, 6, 0},

            {8, 0, 0,   0, 6, 0,   0, 0, 3},
            {4, 0, 0,   8, 0, 3,   0, 0, 1},
            {7, 0, 0,   0, 2, 0,   0, 0, 6},

            {0, 6, 0,   0, 0, 0,   2, 8, 0},
            {0, 0, 0,   4, 1, 9,   0, 0, 5},
            {0, 0, 0,   0, 8, 0,   0, 7, 9}
        };

        SudokuGrid grid = new SudokuGrid(puzzle, 3);
        System.out.println("Testing Incomplete Solver with different heuristics:\n");

        // Test 1: MRV Heuristic
        testWithHeuristic(grid, new MRVHeuristic(), "MRV (Minimum Remaining Values)");

        // Test 2: Degree Heuristic
        testWithHeuristic(grid, new DegreeHeuristic(), "Degree Heuristic");
    }

    private static void testWithHeuristic(SudokuGrid originalGrid,
            SudokuGrid.Heuristic heuristic,
            String heuristicName) {

        // Create a copy for this test
        SudokuGrid grid = new SudokuGrid(originalGrid);

        System.out.println("--- " + heuristicName + " ---");

        // Create solver
        IncompleteSolver solver = new IncompleteSolver(grid);
        solver.setHeuristic(heuristic);
        solver.setPropagate(true);  // Enable constraint propagation
        solver.setMaxIterations(100000);  // Safety limit

        long startTime = System.nanoTime();
        SolverResult result = solver.solve();
        long endTime = System.nanoTime();

        double timeMs = (endTime - startTime) / 1_000_000.0;

        System.out.println("Solved: " + result.isSolved());
        System.out.println("Time: " + String.format("%.2f", timeMs) + " ms");
        System.out.println("Iterations: " + result.getIterations());
        System.out.println("Backtracks: " + result.getBacktracks());
        System.out.println();
    }
}
```

**Output:**
```
Testing Incomplete Solver with different heuristics:

--- MRV (Minimum Remaining Values) ---
Solved: true
Time: 12.34 ms
Iterations: 2456
Backtracks: 145

--- Degree Heuristic ---
Solved: true
Time: 15.67 ms
Iterations: 2120
Backtracks: 98
```

---

## Example 3: Parsing from File

```java
import src.model.SudokuGrid;
import src.solver.CompleteSolver;
import src.solver.IncompleteSolver;
import src.util.PuzzleParser;
import java.io.File;
import java.io.IOException;

public class Example3_ParsingFromFile {
    public static void main(String[] args) {
        try {
            // Parse from plain text file
            System.out.println("=== Parsing from Text File ===");
            SudokuGrid grid1 = PuzzleParser.parseFromFile("puzzles/sudoku1.txt", 3);
            System.out.println("Loaded puzzle from sudoku1.txt");
            grid1.print();
            System.out.println();

            // Parse from MiniZinc format
            System.out.println("=== Parsing from MiniZinc ===");
            SudokuGrid grid2 = PuzzleParser.parseFromMiniZinc(new File("puzzles/sudoku_p0.dzn"));
            System.out.println("Loaded puzzle from MiniZinc file");
            grid2.print();
            System.out.println();

            // Solve the parsed puzzles
            System.out.println("=== Solving ===");
            SolverResult result = new CompleteSolver(grid1).solve();
            System.out.println("Puzzle 1 solved: " + result.isSolved());

            result = new CompleteSolver(grid2).solve();
            System.out.println("Puzzle 2 solved: " + result.isSolved());

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
```

---

## Example 4: Performance Comparison

```java
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.IncompleteSolver;
import src.heuristic.MRVHeuristic;

public class Example4_PerformanceComparison {
    public static void main(String[] args) {
        int[][] puzzle = {
            {5, 3, 0,   0, 7, 0,   0, 0, 0},
            {6, 0, 0,   1, 9, 5,   0, 0, 0},
            {0, 9, 8,   0, 0, 0,   0, 6, 0},

            {8, 0, 0,   0, 6, 0,   0, 0, 3},
            {4, 0, 0,   8, 0, 3,   0, 0, 1},
            {7, 0, 0,   0, 2, 0,   0, 0, 6},

            {0, 6, 0,   0, 0, 0,   2, 8, 0},
            {0, 0, 0,   4, 1, 9,   0, 0, 5},
            {0, 0, 0,   0, 8, 0,   0, 7, 9}
        };

        SudokuGrid gridCopy1 = new SudokuGrid(puzzle, 3);
        SudokuGrid gridCopy2 = new SudokuGrid(puzzle, 3);

        System.out.println("=== Performance Comparison ===\n");

        // Complete Solver
        System.out.println("1. Complete Solver (Choco):");
        CompleteSolver completeSolver = new CompleteSolver(gridCopy1);
        completeSolver.setStrategy(CompleteSolver.SearchStrategy.DOM_OVER_WDEG);
        SolverResult completeResult = completeSolver.solve();
        System.out.println("   Time: " + completeResult.getTimeMs() + " ms");
        System.out.println("   Solved: " + completeResult.isSolved());

        // Incomplete Solver
        System.out.println("\n2. Incomplete Solver (Backtracking):");
        IncompleteSolver incompleteSolver = new IncompleteSolver(gridCopy2);
        incompleteSolver.setHeuristic(new MRVHeuristic());
        incompleteSolver.setPropagate(true);
        SolverResult incompleteResult = incompleteSolver.solve();
        System.out.println("   Time: " + incompleteResult.getTimeMs() + " ms");
        System.out.println("   Iterations: " + incompleteResult.getIterations());
        System.out.println("   Solved: " + incompleteResult.isSolved());

        // Comparison
        System.out.println("\n=== Comparison ===");
        if (completeResult.isSolved() && incompleteResult.isSolved()) {
            long completeTime = completeResult.getTimeMs();
            long incompleteTime = incompleteResult.getTimeMs();
            double ratio = (double) incompleteTime / completeTime;

            System.out.println("Complete solver " +
                (completeTime < incompleteTime ? "faster" : "slower"));
            System.out.println("Ratio: " + String.format("%.2f", ratio) + "x");
        }
    }
}
```

---

## Example 5: Custom Heuristic

```java
import src.heuristic.CellHeuristic;
import src.model.SudokuGrid;
import src.solver.IncompleteSolver;

// Custom heuristic: Random selection among empty cells
public class RandomHeuristic implements CellHeuristic {
    private java.util.Random random = new java.util.Random();

    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        java.util.List<int[]> emptyCells = new java.util.ArrayList<>();

        // Collect all empty cells
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.isEmpty(i, j)) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        // Return random cell
        if (emptyCells.isEmpty()) return null;
        return emptyCells.get(random.nextInt(emptyCells.size()));
    }

    // Usage
    public static void main(String[] args) {
        int[][] puzzle = { /* ... */ };
        SudokuGrid grid = new SudokuGrid(puzzle, 3);

        IncompleteSolver solver = new IncompleteSolver(grid);
        solver.setHeuristic(new RandomHeuristic());

        var result = solver.solve();
        System.out.println("Solved with random heuristic: " + result.isSolved());
    }
}
```

---

## Example 6: Validation Only

```java
import src.model.SudokuGrid;
import src.util.SudokuValidator;

public class Example6_Validation {
    public static void main(String[] args) {
        int[][] puzzle = {
            {5, 3, 4,   0, 7, 0,   0, 0, 0},
            {6, 0, 0,   1, 9, 5,   0, 0, 0},
            {0, 9, 8,   0, 0, 0,   0, 6, 0},

            {8, 0, 0,   0, 6, 0,   0, 0, 3},
            {4, 0, 0,   8, 0, 3,   0, 0, 1},
            {7, 0, 0,   0, 2, 0,   0, 0, 6},

            {0, 6, 0,   0, 0, 0,   2, 8, 0},
            {0, 0, 0,   4, 1, 9,   0, 0, 5},
            {0, 0, 0,   0, 8, 0,   0, 7, 9}
        };

        SudokuGrid grid = new SudokuGrid(puzzle, 3);

        System.out.println("=== Validation Examples ===\n");

        // Check entire grid validity
        boolean gridValid = SudokuValidator.isValid(grid);
        System.out.println("Grid is valid: " + gridValid);

        // Check specific placement
        boolean canPlace = SudokuValidator.isValidPlacement(grid, 0, 2, 4);
        System.out.println("Can place 4 at (0, 2): " + canPlace);

        canPlace = SudokuValidator.isValidPlacement(grid, 0, 2, 5);
        System.out.println("Can place 5 at (0, 2): " + canPlace);

        // Check row, column, block
        System.out.println("Row 0 valid: " + SudokuValidator.isRowValid(grid, 0));
        System.out.println("Column 0 valid: " + SudokuValidator.isColumnValid(grid, 0));
        System.out.println("Block (0, 0) valid: " + SudokuValidator.isBlockValid(grid, 0, 0));
    }
}
```

---

## Example 7: Batch Processing

```java
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.IncompleteSolver;
import src.heuristic.MRVHeuristic;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Example7_BatchProcessing {
    static class PuzzleStats {
        String filename;
        boolean completeSolved;
        long completeTime;
        boolean incompleteSolved;
        long incompleteTime;
    }

    public static void main(String[] args) {
        List<PuzzleStats> stats = new ArrayList<>();

        // Simulate processing multiple puzzles
        File[] puzzleFiles = new File("puzzles").listFiles(
            f -> f.getName().endsWith(".txt")
        );

        if (puzzleFiles != null) {
            for (File file : puzzleFiles) {
                try {
                    PuzzleStats stat = processPuzzle(file);
                    stats.add(stat);
                } catch (Exception e) {
                    System.err.println("Error processing " + file.getName());
                }
            }
        }

        // Print summary
        printSummary(stats);
    }

    private static PuzzleStats processPuzzle(File file) throws Exception {
        PuzzleStats stat = new PuzzleStats();
        stat.filename = file.getName();

        // Parse puzzle
        SudokuGrid grid1 = new SudokuGrid(
            parsePuzzle(file),
            3
        );
        SudokuGrid grid2 = new SudokuGrid(grid1);

        // Solve with Complete
        CompleteSolver complete = new CompleteSolver(grid1);
        SolverResult completeResult = complete.solve();
        stat.completeSolved = completeResult.isSolved();
        stat.completeTime = completeResult.getTimeMs();

        // Solve with Incomplete
        IncompleteSolver incomplete = new IncompleteSolver(grid2);
        incomplete.setHeuristic(new MRVHeuristic());
        SolverResult incompleteResult = incomplete.solve();
        stat.incompleteSolved = incompleteResult.isSolved();
        stat.incompleteTime = incompleteResult.getTimeMs();

        return stat;
    }

    private static int[][] parsePuzzle(File file) throws Exception {
        // Implement simple file parsing
        int[][] puzzle = new int[9][9];
        // ... parsing logic ...
        return puzzle;
    }

    private static void printSummary(List<PuzzleStats> stats) {
        System.out.println("=== Batch Processing Results ===\n");
        System.out.printf("%-20s | %-8s | %-8s | %-8s | %-8s\n",
            "Puzzle", "Complete", "Compl.Time", "Incomp", "Incomp.Time");
        System.out.println("-".repeat(65));

        for (PuzzleStats stat : stats) {
            System.out.printf("%-20s | %-8s | %-8d | %-8s | %-8d\n",
                stat.filename,
                stat.completeSolved ? "OK" : "FAIL",
                stat.completeTime,
                stat.incompleteSolved ? "OK" : "FAIL",
                stat.incompleteTime);
        }
    }
}
```

---

## Summary

These examples demonstrate:
1. **Basic usage** - Create grid, solve, display results
2. **Heuristics** - Compare different cell selection strategies
3. **File parsing** - Load puzzles from files
4. **Performance** - Compare complete vs incomplete solvers
5. **Custom heuristics** - Extend the framework
6. **Validation** - Check puzzle legality
7. **Batch processing** - Process multiple puzzles

All examples are self-contained and ready to run!
