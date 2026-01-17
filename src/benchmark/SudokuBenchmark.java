package src.benchmark;

import src.heuristic.DegreeHeuristic;
import src.heuristic.MRVHeuristic;
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.CompleteSolver.SearchStrategy;
import src.solver.CompleteSolver.ValueHeuristic;
import src.solver.CompleteSolver.RestartType;
import src.solver.IncompleteSolver;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Comprehensive Sudoku Solver Benchmark Suite.
 * 
 * Tests both Complete (Choco constraint solver) and Incomplete (Backtracking)
 * solvers with multiple heuristics and difficulty levels.
 * Generates detailed CSV reports for analysis.
 */
public class SudokuBenchmark {

    // ==========================================
    // CONFIGURATION
    // ==========================================

    // Puzzle repository
    private static final String BASE_URL = "https://www.hakank.org/minizinc/sudoku_problems2/";
    private static final String LOCAL_DIR = "benchmarks";
    private static final String CSV_OUTPUT = "benchmarks/benchmark_results.csv";

    // Test instances representing various difficulty levels
    private static final int[] EASY_INDICES = {0};    // Very easy puzzles
    private static final int[] MEDIUM_INDICES = {36}; // Medium difficulty
    private static final int[] HARD_INDICES = {89};   // Hard puzzles

    // Benchmark configuration
    private static final int BENCHMARK_RUNS = 3;                // Runs per configuration
    private static final long ITERATION_LIMIT = 1_000_000;      // Max iterations for incomplete solver
    private static final int TIMEOUT_SECONDS = 60;              // Timeout for each solver (1 min)

    /**
     * Parameterized configuration for a single CompleteSolver run.
     */
    private static class CompleteConfig {
        String name;                 // Label for output / CSV
        SearchStrategy strategy;     // High-level search strategy
        ValueHeuristic valueHeuristic; // Value selection heuristic
        String consistencyLevel;     // Consistency level for allDifferent ("DEFAULT", "AC", "BC")
        RestartType restartType;     // Restart policy type
        boolean useRestarts;         // Enable restarts
        int lubyBase;                // Base for Luby / Geometric
        int lubyUnit;                // FailCounter unit
        int lubyFactor;              // Luby growth factor OR (ratio*10) for geometric
        boolean randomizeOrder;      // Shuffle variable order
        long randomSeed;             // Seed for reproducibility (0 = system nanoTime)

        CompleteConfig(String name,
                       SearchStrategy strategy,
                       ValueHeuristic valueHeuristic,
                       String consistencyLevel,
                       RestartType restartType,
                       boolean useRestarts,
                       int lubyBase,
                       int lubyUnit,
                       int lubyFactor,
                       boolean randomizeOrder,
                       long randomSeed) {
            this.name = name;
            this.strategy = strategy;
            this.valueHeuristic = valueHeuristic;
            this.consistencyLevel = consistencyLevel;
            this.restartType = restartType;
            this.useRestarts = useRestarts;
            this.lubyBase = lubyBase;
            this.lubyUnit = lubyUnit;
            this.lubyFactor = lubyFactor;
            this.randomizeOrder = randomizeOrder;
            this.randomSeed = randomSeed;
        }
    }

    /**
     * Set of CompleteSolver configurations to benchmark.
     *
     * We explore several combinations of:
     *  - strategy (INPUT_ORDER, DOM_OVER_WDEG, MIN_DOM_SIZE, RANDOM)
     *  - Luby restart base values
     *  - randomized variable orders with different seeds
     */
    private static final List<CompleteConfig> COMPLETE_CONFIGS = Arrays.asList(
        // Baseline: input order, no restarts
        new CompleteConfig(
            "Complete_InputOrder_NoRestart",
            SearchStrategy.INPUT_ORDER,
            ValueHeuristic.MIN,
            "DEFAULT",
            RestartType.LUBY,
            false, 0, 1, 2,
            false, 0L
        ),

        // INPUT_ORDER + Luby restarts with different bases
        new CompleteConfig(
            "Complete_InputOrder_Luby100",
            SearchStrategy.INPUT_ORDER,
            ValueHeuristic.MIN,
            "DEFAULT",
            RestartType.LUBY,
            true, 100, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_InputOrder_Luby500",
            SearchStrategy.INPUT_ORDER,
            ValueHeuristic.MIN,
            "AC",
            RestartType.LUBY,
            true, 500, 1, 2,
            false, 0L
        ),

        // DomOverWDeg with multiple Luby bases
        new CompleteConfig(
            "Complete_DomOverWDeg_NoRestart",
            SearchStrategy.DOM_OVER_WDEG,
            ValueHeuristic.MIN,
            "DEFAULT",
            RestartType.LUBY,
            false, 0, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_DomOverWDeg_Luby100",
            SearchStrategy.DOM_OVER_WDEG,
            ValueHeuristic.MIN,
            "DEFAULT",
            RestartType.LUBY,
            true, 100, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_DomOverWDeg_Luby500",
            SearchStrategy.DOM_OVER_WDEG,
            ValueHeuristic.MAX,
            "AC",
            RestartType.LUBY,
            true, 500, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_DomOverWDeg_Luby1000",
            SearchStrategy.DOM_OVER_WDEG,
            ValueHeuristic.RANDOM_VAL,
            "DEFAULT",
            RestartType.LUBY,
            true, 1000, 1, 2,
            false, 0L
        ),

        // MinDom with and without restarts
        new CompleteConfig(
            "Complete_MinDom_NoRestart",
            SearchStrategy.MIN_DOM_SIZE,
            ValueHeuristic.MIN,
            "DEFAULT",
            RestartType.LUBY,
            false, 0, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_MinDom_Luby200",
            SearchStrategy.MIN_DOM_SIZE,
            ValueHeuristic.RANDOM_VAL,
            "AC",
            RestartType.LUBY,
            true, 200, 1, 2,
            false, 0L
        ),
        new CompleteConfig(
            "Complete_MinDom_Luby800",
            SearchStrategy.MIN_DOM_SIZE,
            ValueHeuristic.MIDDLE,
            "DEFAULT",
            RestartType.LUBY,
            true, 800, 1, 2,
            false, 0L
        ),

        // New: explicit geometric restarts configurations
        new CompleteConfig(
            "Complete_DomOverWDeg_MaxVal_Geom10_1.1",
            SearchStrategy.DOM_OVER_WDEG,
            ValueHeuristic.MAX,
            "DEFAULT",
            RestartType.GEOMETRIC,
            true, 10, 1, 11,   // base=10, ratio=11/10=1.1
            false, 0L
        ),
        new CompleteConfig(
            "Complete_InputOrder_Middle_AC_Geom10_1.2",
            SearchStrategy.INPUT_ORDER,
            ValueHeuristic.MIDDLE,
            "AC",
            RestartType.GEOMETRIC,
            true, 10, 1, 12,   // base=10, ratio=12/10=1.2
            false, 0L
        )
    );

    // ==========================================
    // MAIN BENCHMARK ENTRY
    // ==========================================

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         COMPREHENSIVE SUDOKU SOLVER BENCHMARK              ║");
        System.out.println("║   Complete (Choco) vs Incomplete (Backtracking) Solvers   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            // Load puzzle instances
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

            // Export results to CSV
            exportToCSV(allResults);

            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("✓ Benchmark Complete! Check 'benchmarks/benchmark_results.csv' ║");
            System.out.println("║            CSV for detailed analysis.                      ║");
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
     * Loads puzzle instances with various difficulty levels.
     * Downloads puzzles from the repository if not already cached locally.
     */
    private static List<SudokuInstance> loadInstances() {
        List<SudokuInstance> instances = new ArrayList<>();
        new File(LOCAL_DIR).mkdirs();

        int[][] allIndices = {EASY_INDICES, MEDIUM_INDICES, HARD_INDICES};
        String[] difficulties = {"Easy", "Medium", "Hard"};

        for (int d = 0; d < allIndices.length; d++) {
            for (int i : allIndices[d]) {
                String filename = "sudoku_p" + i + ".dzn";
                File file = new File(LOCAL_DIR, filename);

                try {
                    // Download if not cached
                    if (!file.exists()) {
                        System.out.print("  Downloading " + filename + "... ");
                        if (downloadFile(BASE_URL + filename, file)) {
                            System.out.println("✓");
                        } else {
                            System.out.println("✗ (skipping)");
                            continue;
                        }
                    }

                    // Parse puzzle
                    System.out.print("  Parsing " + filename + "... ");
                    SudokuInstance inst = MiniZincParser.parse(file, difficulties[d]);
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
     * Runs the complete benchmark suite on all instances.
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

            // Test Complete Solver with different strategies
            System.out.println("> Complete Solver (Choco):");
            testCompleteSolver(inst, allResults);
            System.out.println();

            // Test Incomplete Solver with different heuristics
            System.out.println("> Incomplete Solver (Backtracking):");
            testIncompleteSolver(inst, allResults);
            System.out.println();
        }
    }

    // ==========================================
    // COMPLETE SOLVER TESTING
    // ==========================================

    /**
     * Build a human-readable description for a CompleteSolver configuration.
     * This makes it explicit (for console, CSV, and HTML) which strategy,
     * value heuristic, consistency level, and restart policy are used.
     */
    private static String describeCompleteConfig(CompleteConfig c) {
        String restartDesc;
        if (!c.useRestarts || c.lubyBase <= 0) {
            restartDesc = "NoRestart";
        } else if (c.restartType == RestartType.LUBY) {
            // Use semicolons instead of commas to keep CSV parsing simple
            restartDesc = String.format("Luby(base=%d;factor=%d)", c.lubyBase, c.lubyFactor);
        } else {
            double ratio = c.lubyFactor / 10.0; // e.g. 11 -> 1.1
            restartDesc = String.format("Geom(base=%d;ratio=%.1f)", c.lubyBase, ratio);
        }

        String orderDesc = c.randomizeOrder
                ? ("RandomOrder(seed=" + c.randomSeed + ")")
                : "DeterministicOrder";

        // Use " | " separators instead of commas inside the label so that
        // the CSV (comma-separated) can be parsed with a simple split(',').
        return String.format(
                "Complete[%s | Val=%s | Cons=%s | %s | %s]",
                c.strategy,
                c.valueHeuristic,
                c.consistencyLevel,
                restartDesc,
                orderDesc
        );
    }

    /**
     * Tests Complete Solver with multiple parameterized configurations.
     */
    private static void testCompleteSolver(SudokuInstance instance,
                                           List<BenchmarkRecord> allResults) {
        for (CompleteConfig config : COMPLETE_CONFIGS) {
            String solverName = describeCompleteConfig(config);
            testMultipleRuns(instance, solverName, () -> {
                SudokuGrid grid = new SudokuGrid(instance.getGrid(), instance.n);
                CompleteSolver solver = new CompleteSolver(grid);
                solver.setStrategy(config.strategy);
                solver.setValueHeuristic(config.valueHeuristic);
                solver.setConsistencyLevel(config.consistencyLevel);
                solver.setRestartType(config.restartType);
                solver.setTimeout(TIMEOUT_SECONDS);
                solver.configureRestarts(config.useRestarts, config.lubyBase, config.lubyUnit, config.lubyFactor);
                solver.configureRandomization(config.randomizeOrder, config.randomSeed);
                return solver.solve();
            }, allResults);
        }
    }

    // ==========================================
    // INCOMPLETE SOLVER TESTING
    // ==========================================

    /**
     * Tests Incomplete Solver with multiple heuristics.
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
    }

    // ==========================================
    // UTILITY: MULTIPLE RUN TESTING
    // ==========================================

    /**
     * Runs a solver multiple times and collects statistics.
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
            } catch (Throwable e) { // catch Error as well (e.g., OutOfMemoryError)
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
     * Downloads a file from a URL to a local destination.
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
     * Exports benchmark results to CSV format.
     */
    private static void exportToCSV(List<BenchmarkRecord> records) throws IOException {
        new File(CSV_OUTPUT).getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT))) {
            // Header
            writer.println("Instance,Difficulty,SolverType,Size,Solver," +
                           "MeanTimeMs,StdDevMs,MeanIterations,MeanBacktracks," +
                           "SuccessRate,Status");

            // Data rows
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
     * Functional interface for solver factories.
     */
    @FunctionalInterface
    interface SolverFactory {
        SolverResult solve() throws Exception;
    }

    /**
     * Represents a Sudoku puzzle instance with metadata.
     */
    static class SudokuInstance {
        String name;
        String difficulty;
        int n;       // Block size (e.g., 3 for 9x9 Sudoku)
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
     * Benchmark result record for CSV export.
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
            this.solverType = solverName.contains("Complete") ? "Complete" : "Incomplete";
        }
    }

    /**
     * MiniZinc format parser with automatic size detection.
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

                // Replace underscores with 0 (MiniZinc convention for empty cells)
                line = line.replace("_", "0");

                // Skip variable declarations
                if (line.matches("^[a-zA-Z]\\w*\\s*=.*")) continue;

                // Remove array2d syntax, keep only numbers
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
            int N = (int) Math.sqrt(totalCells);  // e.g., 9 for 9x9
            int n = (int) Math.sqrt(N);           // e.g., 3 for 9x9

            // Validate perfect square structure
            if (N * N != totalCells) {
                throw new IOException("Invalid data: " + totalCells +
                                      " cells is not a perfect Sudoku square");
            }

            if (n * n != N) {
                throw new IOException("Invalid data: grid size " + N +
                                      " does not have integer block size");
            }

            return new SudokuInstance(file.getName(), difficulty, n, data);
        }
    }
}
