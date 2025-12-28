package src;

import src.benchmark.SudokuBenchmark;
import src.heuristic.DegreeHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.CompleteSolver.SearchStrategy;
import src.solver.IncompleteSolver;

/**
 * Main entry point for Sudoku Solver.
 * Supports demo mode and comprehensive benchmarking.
 *
 * Usage:
 *   java -cp "bin;lib/*" src.Main demo
 *   java -cp "bin;lib/*" src.Main benchmark
 *   java -cp "bin;lib/*" src.Main help
 */
public class Main {
    public static void main(String[] args) {
        // Determine operation mode
        String mode = (args.length > 0) ? args[0].toLowerCase() : "demo";

        switch (mode) {
            case "benchmark":
            case "bench":
                runFullBenchmark();
                break;

            case "demo":
            case "example":
            case "":
                runDemoExamples();
                break;

            case "help":
            case "-h":
            case "--help":
                printHelp();
                break;

            default:
                System.out.println("Unknown command: " + mode);
                System.out.println("Use 'help' for usage information.");
                printHelp();
        }
    }

    /**
     * Run full comprehensive benchmark suite
     */
    private static void runFullBenchmark() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        Starting Comprehensive Benchmark Suite              ║");
        System.out.println("║   Tests: Complete & Incomplete Solvers                      ║");
        System.out.println("║   Multiple Heuristics & Difficulty Levels                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        SudokuBenchmark.main(new String[]{});

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Benchmark Complete! Check 'benchmarks/benchmark_results  ║");
        System.out.println("║  in CSV format for detailed analysis.                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * Run demo examples with both solvers
     */
    private static void runDemoExamples() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         SUDOKU SOLVER - DEMO EXAMPLES                      ║");
        System.out.println("║   Complete (Choco) vs Incomplete (Backtracking)           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Easy 9x9 puzzle
        int[][] easyPuzzle = {
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

        System.out.println("Original Puzzle (with 31 clues):");
        System.out.println();
        SudokuGrid grid = new SudokuGrid(easyPuzzle, 3);
        grid.print();
        System.out.println();

        // Test Complete Solver
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("▶ COMPLETE SOLVER (Choco Constraint Programming)");
        System.out.println("  Strategy: DOM_OVER_WDEG (recommended for harder puzzles)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        testCompleteSolver(new SudokuGrid(easyPuzzle, 3));

        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("▶ INCOMPLETE SOLVER (Backtracking + Heuristics)");
        System.out.println("  Heuristic: MRV (Minimum Remaining Values)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        testIncompleteSolver(new SudokuGrid(easyPuzzle, 3));

        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("▶ INCOMPLETE SOLVER (Backtracking + Heuristics)");
        System.out.println("  Heuristic: Degree (with constraint propagation)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        testIncompleteSolverDegree(new SudokuGrid(easyPuzzle, 3));

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Demo Complete!                                            ║");
        System.out.println("║  For more extensive testing, run: Main benchmark           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * Test Complete Solver
     */
    private static void testCompleteSolver(SudokuGrid grid) {
        CompleteSolver solver = new CompleteSolver(grid);
        solver.setStrategy(SearchStrategy.DOM_OVER_WDEG);
        solver.setTimeout(10);

        long startTime = System.nanoTime();
        SolverResult result = solver.solve();
        long elapsedNs = System.nanoTime() - startTime;
        double elapsedMs = elapsedNs / 1_000_000.0;

        System.out.println("Status:     " + (result.isSolved() ? "✓ SOLVED" : "✗ NOT SOLVED"));
        System.out.println("Time:       " + String.format("%.2f", elapsedMs) + " ms");
        System.out.println("Iterations: " + result.getIterations());
        System.out.println("Backtracks: " + result.getBacktracks());

        if (result.isSolved()) {
            System.out.println();
            System.out.println("Solution:");
            System.out.println();
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

    /**
     * Test Incomplete Solver with MRV Heuristic
     */
    private static void testIncompleteSolver(SudokuGrid grid) {
        IncompleteSolver solver = new IncompleteSolver(grid);
        solver.setHeuristic(new MRVHeuristic());
        solver.setPropagate(true);
        solver.setMaxIterations(100_000);

        long startTime = System.nanoTime();
        SolverResult result = solver.solve();
        long elapsedNs = System.nanoTime() - startTime;
        double elapsedMs = elapsedNs / 1_000_000.0;

        System.out.println("Status:     " + (result.isSolved() ? "✓ SOLVED" : "✗ NOT SOLVED"));
        System.out.println("Time:       " + String.format("%.2f", elapsedMs) + " ms");
        System.out.println("Iterations: " + result.getIterations());
        System.out.println("Backtracks: " + result.getBacktracks());

        if (result.isSolved()) {
            System.out.println();
            System.out.println("Solution:");
            System.out.println();
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

    /**
     * Test Incomplete Solver with Degree Heuristic
     */
    private static void testIncompleteSolverDegree(SudokuGrid grid) {
        IncompleteSolver solver = new IncompleteSolver(grid);
        solver.setHeuristic(new DegreeHeuristic());
        solver.setPropagate(true);
        solver.setMaxIterations(100_000);

        long startTime = System.nanoTime();
        SolverResult result = solver.solve();
        long elapsedNs = System.nanoTime() - startTime;
        double elapsedMs = elapsedNs / 1_000_000.0;

        System.out.println("Status:     " + (result.isSolved() ? "✓ SOLVED" : "✗ NOT SOLVED"));
        System.out.println("Time:       " + String.format("%.2f", elapsedMs) + " ms");
        System.out.println("Iterations: " + result.getIterations());
        System.out.println("Backtracks: " + result.getBacktracks());

        if (result.isSolved()) {
            System.out.println();
            System.out.println("Solution:");
            System.out.println();
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

    /**
     * Print help/usage information
     */
    private static void printHelp() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           SUDOKU SOLVER - Complete Usage Guide             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -cp bin:lib/* src.Main [command]");
        System.out.println();
        System.out.println("COMMANDS:");
        System.out.println("  demo (default)      Run demo examples with easy puzzle");
        System.out.println("  benchmark           Run comprehensive benchmark suite");
        System.out.println("  quick-benchmark     Run limited benchmark (fewer puzzles)");
        System.out.println("  help                Show this help message");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  java -cp \"bin;lib/*\" src.Main demo");
        System.out.println("  java -cp \"bin;lib/*\" src.Main benchmark");
        System.out.println("  java -cp \"bin;lib/*\" src.Main help");
        System.out.println();
        System.out.println("OUTPUT:");
        System.out.println("  Console: Real-time results and progress");
        System.out.println("  CSV:     benchmarks/benchmark_results.csv (benchmark mode)");
        System.out.println();
        System.out.println("SOLVERS:");
        System.out.println("  • Complete Solver (Choco)");
        System.out.println("    - Guaranteed solution");
        System.out.println("    - Multiple search strategies");
        System.out.println("    - Best for medium difficulty");
        System.out.println();
        System.out.println("  • Incomplete Solver (Backtracking)");
        System.out.println("    - Fast on many puzzles");
        System.out.println("    - Customizable heuristics (MRV, Degree)");
        System.out.println("    - Constraint propagation");
        System.out.println();
    }
}
