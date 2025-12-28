package src.benchmark;

import src.heuristic.DegreeHeuristic;
import src.heuristic.HybridHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.CompleteSolver.SearchStrategy;
import src.solver.IncompleteSolver;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Comprehensive Sudoku Solver Benchmark.
 * Tests both Complete (Choco) and Incomplete (Backtracking) solvers
 * with multiple heuristics and difficulty levels.
 * Generates detailed CSV reports.
 */
public class SudokuBenchmark {

    // ==========================================
    // CONFIGURATION
    // ==========================================

    // MiniZinc puzzle download source
    private static final String BASE_URL = "https://www.hakank.org/minizinc/sudoku_problems2/";
    private static final String LOCAL_DIR = "benchmarks";
    private static final String CSV_OUTPUT = "benchmarks/benchmark_results.csv";

    // Test instances representing various difficulty levels
    private static final int[] EASY_INDICES = {0};           // Very easy
    private static final int[] MEDIUM_INDICES = {1, 2, 3};   // Medium difficulty
    private static final int[] HARD_INDICES = {10, 20, 30};  // Hard puzzles
    private static final int[] LARGE_INDICES = {36};         // 16x16 puzzles

    // Benchmark configuration
    private static final int BENCHMARK_RUNS = 3;             // Runs per configuration
    private static final long ITERATION_LIMIT = 100_000;     // Max iterations for incomplete solver
    private static final int TIMEOUT_SECONDS = 30;           // Timeout for each solver

    // ==========================================
    // MAIN BENCHMARK ENTRY
    // ==========================================

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   COMPREHENSIVE SUDOKU SOLVER BENCHMARK                     ║");
        System.out.println("║   Complete (Choco) vs Incomplete (Backtracking) Solvers    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Prepare benchmark data
            List<SudokuInstance> instances = loadInstances();
            if (instances.isEmpty()) {
                System.err.println("No puzzle instances loaded. Exiting.");
                return;
            }

            System.out.println("✓ Loaded " + instances.size() + " puzzle instances");
            System.out.println("✓ Benchmark runs per configuration: " + BENCHMARK_RUNS);
            System.out.println();

            // Run benchmark
            List<BenchmarkRecord> allResults = new ArrayList<>();
            runBenchmark(instances, allResults);

            // Export results
            exportToCSV(allResults);

            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  Benchmark Complete! Check 'benchmarks/benchmark_results  ║");
            System.out.println("║  in CSV format for detailed analysis.                     ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("Fatal error during benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // BENCHMARK EXECUTION
    // ==========================================

    /**
     * Load puzzle instances with various difficulty levels
     */
    private static List<SudokuInstance> loadInstances() {
        List<SudokuInstance> instances = new ArrayList<>();
        new File(LOCAL_DIR).mkdirs();

        int[][] allIndices = {EASY_INDICES, MEDIUM_INDICES, HARD_INDICES, LARGE_INDICES};
        String[] difficulty = {"Easy", "Medium", "Hard", "Large"};

        for (int d = 0; d < allIndices.length; d++) {
            for (int i : allIndices[d]) {
                String filename = "sudoku_p" + i + ".dzn";
                File file = new File(LOCAL_DIR, filename);

                try {
                    // Download if needed
                    if (!file.exists()) {
                        System.out.print("  Downloading " + filename + "... ");
                        if (downloadFile(BASE_URL + filename, file)) {
                            System.out.println("✓");
                        } else {
                            System.out.println("✗ (skipping)");
                            continue;
                        }
                    }

                    // Parse
                    System.out.print("  Parsing " + filename + "... ");
                    SudokuInstance inst = MiniZincParser.parse(file, difficulty[d]);
                    instances.add(inst);
                    System.out.println("✓ (" + inst.n + "x" + inst.n + " = " + 
                        (inst.n * inst.n) + "x" + (inst.n * inst.n) + ")");

                } catch (Exception e) {
                    System.out.println("✗ Error: " + e.getMessage());
                }
            }
        }

        return instances;
    }

    /**
     * Run complete benchmark suite
     */
    private static void runBenchmark(List<SudokuInstance> instances, 
                                    List<BenchmarkRecord> allResults) {
        System.out.println();
        System.out.println("Starting benchmark with " + instances.size() + " puzzle(s)...");
        System.out.println();

        for (SudokuInstance inst : instances) {
            System.out.println("------------------------------------------------------------");
            System.out.printf("Instance: %s | Size: %dx%d | Clues: %d | Density: %.1f%%%n",
                inst.name, inst.n * inst.n, inst.n * inst.n,
                inst.countGivenClues(), inst.getDensity() * 100);
            System.out.println("------------------------------------------------------------");

            // Test COMPLETE SOLVER with different strategies
            System.out.println("> Complete Solver (Choco):");
            testCompleteSolver(inst, allResults);
            System.out.println();

            // Test INCOMPLETE SOLVER with different heuristics
            System.out.println("> Incomplete Solver (Backtracking):");
            testIncompleteSolver(inst, allResults);
            System.out.println();
        }
    }

    // ==========================================
    // COMPLETE SOLVER TESTING
    // ==========================================

    /**
     * Test Complete Solver with multiple strategies
     */
    private static void testCompleteSolver(SudokuInstance instance, 
                                          List<BenchmarkRecord> allResults) {
        SearchStrategy[] strategies = {
            SearchStrategy.INPUT_ORDER,
            SearchStrategy.DOM_OVER_WDEG,
            SearchStrategy.MIN_DOM_SIZE
        };

        for (SearchStrategy strategy : strategies) {
            String strategyName = "Complete_" + strategy.name();
            testMultipleRuns(instance, strategyName, () -> {
                SudokuGrid grid = new SudokuGrid(instance.getGrid(), instance.n);
                CompleteSolver solver = new CompleteSolver(grid);
                solver.setStrategy(strategy);
                solver.setTimeout(TIMEOUT_SECONDS);
                return solver.solve();
            }, allResults);
        }
    }

    // ==========================================
    // INCOMPLETE SOLVER TESTING
    // ==========================================

    /**
     * Test Incomplete Solver with multiple heuristics
     */
    private static void testIncompleteSolver(SudokuInstance instance, 
                                            List<BenchmarkRecord> allResults) {
        // Test with MRV heuristic
        testMultipleRuns(instance, "Incomplete_MRV", () -> {
            SudokuGrid grid = new SudokuGrid(instance.getGrid(), instance.n);
            IncompleteSolver solver = new IncompleteSolver(grid);
            solver.setHeuristic(new MRVHeuristic());
            solver.setPropagate(true);
            solver.setMaxIterations(ITERATION_LIMIT);
            return solver.solve();
        }, allResults);

        // Test with Degree heuristic
        testMultipleRuns(instance, "Incomplete_Degree", () -> {
            SudokuGrid grid = new SudokuGrid(instance.getGrid(), instance.n);
            IncompleteSolver solver = new IncompleteSolver(grid);
            solver.setHeuristic(new DegreeHeuristic());
            solver.setPropagate(true);
            solver.setMaxIterations(ITERATION_LIMIT);
            return solver.solve();
        }, allResults);

        // Test with Hybrid heuristic (MRV + Degree)
        testMultipleRuns(instance, "Incomplete_Hybrid", () -> {
            SudokuGrid grid = new SudokuGrid(instance.getGrid(), instance.n);
            IncompleteSolver solver = new IncompleteSolver(grid);
            solver.setHeuristic(new HybridHeuristic());
            solver.setPropagate(true);
            solver.setMaxIterations(ITERATION_LIMIT);
            return solver.solve();
        }, allResults);
    }

    // ==========================================
    // UTILITY: MULTIPLE RUN TESTING
    // ==========================================

    /**
     * Run a solver multiple times and collect statistics
     */
    private static void testMultipleRuns(SudokuInstance instance, 
                                        String solverName,
                                        SolverFactory factory, 
                                        List<BenchmarkRecord> allResults) {
        List<Long> times = new ArrayList<>();
        List<Long> iterations = new ArrayList<>();
        List<Long> backtracks = new ArrayList<>();
        int successCount = 0;

        System.out.print("  " + String.format("%-30s", solverName) + " ");

        for (int run = 0; run < BENCHMARK_RUNS; run++) {
            System.out.print(".");

            try {
                SolverResult result = factory.solve();
                if (result.isSolved()) {
                    successCount++;
                }

                times.add(result.getTimeMs());
                iterations.add(result.getIterations());
                backtracks.add(result.getBacktracks());

            } catch (Exception e) {
                times.add((long)(TIMEOUT_SECONDS * 1000));
                iterations.add(0L);
                backtracks.add(0L);
            }
        }

        System.out.println();

        // Compute statistics
        double meanTime = computeMean(times);
        double stdDevTime = computeStdDev(times, meanTime);
        double meanIterations = computeMean(iterations);
        double meanBacktracks = computeMean(backtracks);

        String status = successCount == BENCHMARK_RUNS ? "SAT" :
                (successCount > 0 ? "PARTIAL(" + successCount + "/" + BENCHMARK_RUNS + ")" : "TIMEOUT");

        // Print inline results
        System.out.printf("  ├─ Time: %.2f±%.2f ms | Iterations: %.0f | Backtracks: %.0f | Status: %s%n",
            meanTime, stdDevTime, meanIterations, meanBacktracks, status);

        // Record for CSV
        allResults.add(new BenchmarkRecord(
            instance.name, solverName, instance.n,
            meanTime, stdDevTime, meanIterations, meanBacktracks,
            status, successCount, BENCHMARK_RUNS, instance.difficulty
        ));
    }

    // ==========================================
    // FILE I/O & UTILITIES
    // ==========================================

    /**
     * Download file from URL
     */
    private static boolean downloadFile(String urlStr, File dest) {
        try (InputStream in = new URL(urlStr).openStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Export benchmark results to CSV
     */
    private static void exportToCSV(List<BenchmarkRecord> records) throws IOException {
        new File(CSV_OUTPUT).getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT))) {
            // Header
            writer.println("Instance,Difficulty,SolverType,Size,Solver," +
                    "MeanTimeMs,StdDevMs,MeanIterations,MeanBacktracks," +
                    "SuccessRate,Status");

            // Data
            for (BenchmarkRecord record : records) {
                writer.printf("%s,%s,%s,%d,%s,%.2f,%.2f,%.0f,%.0f,%.1f%%,%s%n",
                    record.instanceName,
                    record.difficulty,
                    record.solverType,
                    record.gridSize,
                    record.solverName,
                    record.meanTimeMs,
                    record.stdDevMs,
                    record.meanIterations,
                    record.meanBacktracks,
                    record.successRate * 100,
                    record.status
                );
            }
        }

        System.out.println("✓ CSV report exported to: " + CSV_OUTPUT);
    }

    // ==========================================
    // STATISTICS
    // ==========================================

    private static double computeMean(List<Long> values) {
        return values.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
    }

    private static double computeStdDev(List<Long> values, double mean) {
        if (values.size() <= 1) return 0.0;
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    // ==========================================
    // INNER CLASSES
    // ==========================================

    /**
     * Functional interface for solver factories
     */
    @FunctionalInterface
    interface SolverFactory {
        SolverResult solve() throws Exception;
    }

    /**
     * Represents a Sudoku puzzle instance
     */
    static class SudokuInstance {
        String name;
        String difficulty;
        int n; // Block size
        int[][] grid;

        SudokuInstance(String name, String difficulty, int n, int[] flatData) {
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

        int countGivenClues() {
            int count = 0;
            int N = n * n;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid[i][j] != 0) count++;
                }
            }
            return count;
        }

        double getDensity() {
            int N = n * n;
            return (double) countGivenClues() / (N * N);
        }

        int[][] getGrid() {
            int N = n * n;
            int[][] copy = new int[N][N];
            for (int i = 0; i < N; i++) {
                copy[i] = grid[i].clone();
            }
            return copy;
        }
    }

    /**
     * Benchmark result record for CSV export
     */
    static class BenchmarkRecord {
        String instanceName;
        String difficulty;
        String solverType;
        int gridSize;
        String solverName;
        double meanTimeMs;
        double stdDevMs;
        double meanIterations;
        double meanBacktracks;
        double successRate;
        String status;

        BenchmarkRecord(String instanceName, String solverName, int blockSize,
                       double meanTimeMs, double stdDevMs, double meanIterations,
                       double meanBacktracks, String status, int successCount, 
                       int totalRuns, String difficulty) {
            this.instanceName = instanceName;
            this.solverName = solverName;
            this.gridSize = blockSize * blockSize;
            this.meanTimeMs = meanTimeMs;
            this.stdDevMs = stdDevMs;
            this.meanIterations = meanIterations;
            this.meanBacktracks = meanBacktracks;
            this.status = status;
            this.successRate = (double) successCount / totalRuns;
            this.difficulty = difficulty;

            // Determine solver type
            if (solverName.contains("Complete")) {
                this.solverType = "Complete";
            } else {
                this.solverType = "Incomplete";
            }
        }
    }

    /**
     * MiniZinc format parser with auto-size detection
     * FIXED: Correctly parses array2d format and detects grid size
     */
    static class MiniZincParser {
        static SudokuInstance parse(File file, String difficulty) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder dataString = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("%")) continue;

                // Replace underscores with 0
                line = line.replace("_", "0");

                // Skip variable declarations like "n = 3;" or "grid = ..."
                if (line.matches("^[a-zA-Z]\\w*\\s*=.*")) continue;

                // Skip array2d syntax, keep only numbers
                line = line.replace("array2d", " ");
                line = line.replace("(1..", " ");
                line = line.replace("..", " ");

                // Extract all numbers from this line
                String cleanLine = line.replaceAll("[^0-9]", " ");
                dataString.append(cleanLine).append(" ");
            }

            reader.close();

            // Parse all numbers into array
            int[] data = Arrays.stream(dataString.toString().trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();

            // Auto-detect grid size
            int totalCells = data.length;

            // For 9x9 Sudoku: totalCells = 81, N = 9, n = 3
            // For 16x16 Sudoku: totalCells = 256, N = 16, n = 4
            int N = (int) Math.sqrt(totalCells);
            int n = (int) Math.sqrt(N);

            // Validate that it's a perfect square
            if (N * N != totalCells) {
                throw new IOException("Invalid data: " + totalCells + 
                    " cells is not a perfect Sudoku square");
            }

            // Validate that n is perfect square root
            if (n * n != N) {
                throw new IOException("Invalid data: grid size " + N + 
                    " does not have integer block size");
            }

            return new SudokuInstance(file.getName(), difficulty, n, data);
        }
    }
}
