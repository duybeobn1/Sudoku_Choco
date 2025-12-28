package src;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.IntStream;
import java.text.DecimalFormat;

public class SudokuBenchmark {

    // ==========================================
    // CONFIGURATION
    // ==========================================
    private static final String BASE_URL = "https://www.hakank.org/minizinc/sudoku_problems2/";
    private static final String LOCAL_DIR = "benchmarks";
    
    // Test instances: 9x9, 9x9, 16x16, 25x25
    private static final int[] INSTANCE_INDICES = {0, 1, 36, 89}; 
    
    // Increased time limit to let the naive strategy run longer on the large puzzles
    private static final String TIME_LIMIT = "60s";
    
    // Number of runs for statistical analysis
    private static final int BENCHMARK_RUNS = 5;
    
    // CSV output file
    private static final String CSV_OUTPUT = "../benchmarks/benchmark_results.csv";

    public static void main(String[] args) {
        System.out.println("=== SUDOKU SOLVER BENCHMARK (Choco Solver) ===");
        System.out.println("Runs per configuration: " + BENCHMARK_RUNS);
        
        // 1. Prepare Data
        List<SudokuInstance> instances = new ArrayList<>();
        new File(LOCAL_DIR).mkdirs();

        for (int i : INSTANCE_INDICES) {
            String filename = "sudoku_p" + i + ".dzn";
            File file = new File(LOCAL_DIR, filename);
            
            // Auto-download if missing
            if (!file.exists()) {
                System.out.println("Downloading " + filename + "...");
                if (!downloadFile(BASE_URL + filename, file)) {
                    System.err.println("Failed to download " + filename + ". Skipping.");
                    continue;
                }
            }
            
            // Parse
            try {
                System.out.println("Parsing " + filename + "...");
                instances.add(MiniZincParser.parse(file));
            } catch (Exception e) {
                System.err.println("Error parsing " + filename + ": " + e.getMessage());
            }
        }

        // 2. Run Comparison with Multiple Runs
        List<BenchmarkRecord> allResults = new ArrayList<>();
        
        System.out.printf("%n%-15s | %-6s | %-8s | %-8s | %-20s | %-12s | %-12s | %-12s | %-12s | %-10s%n", 
            "Instance", "Size", "Clues", "Density", "Strategy", "Time(s)", "Nodes", "Fails", "Memory(KB)", "Result");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------");

        for (SudokuInstance inst : instances) {
            System.out.printf("%n[Instance: %s - %dx%d - %d clues (%.1f%% density)]%n", 
                inst.name, inst.n * inst.n, inst.n * inst.n, inst.countGivenClues(), inst.getDensity() * 100);
            
            // METHOD A: Naive Baseline (InputOrder)
            solveMultipleRuns(inst, "Default/InputOrder", SearchStrategy.INPUT_ORDER, allResults);

            // METHOD B: Optimized CP Heuristic (DomOverWDeg + Restarts)
            solveMultipleRuns(inst, "Tuned/DomOverWDeg", SearchStrategy.DOM_OVER_WDEG, allResults);
            
            // METHOD C: First-Fail Heuristic (MinDomSize)
            solveMultipleRuns(inst, "FirstFail/MinDom", SearchStrategy.MIN_DOM_SIZE, allResults);
            
            // METHOD D: Human-Inspired Heuristic (Random Order)
            solveMultipleRuns(inst, "Human/RandomOrder", SearchStrategy.RANDOM, allResults);

            System.out.println("------------------------------------------------------------------------------------------------------------------------------------");
        }
        
        // 3. Write CSV Output
        writeResultsToCSV(allResults);
        System.out.println("%nResults exported to: " + CSV_OUTPUT);
    }

    // ==========================================
    // SOLVER ENGINE & STRATEGIES
    // ==========================================
    
    private enum SearchStrategy {
        INPUT_ORDER, DOM_OVER_WDEG, MIN_DOM_SIZE, RANDOM
    }
    
    private static void solveMultipleRuns(SudokuInstance instance, String strategyName, 
                                           SearchStrategy strategy, List<BenchmarkRecord> allResults) {
        List<Double> times = new ArrayList<>();
        List<Long> nodes = new ArrayList<>();
        List<Long> fails = new ArrayList<>();
        List<Long> memories = new ArrayList<>();
        int successCount = 0;
        
        for (int run = 0; run < BENCHMARK_RUNS; run++) {
            BenchmarkResult result = solveSingle(instance, strategyName, strategy);
            if (result.success) successCount++;
            times.add(result.time);
            nodes.add(result.nodes);
            fails.add(result.fails);
            memories.add(result.memoryKB);
        }
        
        // Compute statistics
        double meanTime = computeMean(times);
        double stdDevTime = computeStdDev(times, meanTime);
        long meanNodes = (long) computeMean(nodes.stream().mapToDouble(Long::doubleValue).boxed().collect(java.util.stream.Collectors.toList()));
        long meanFails = (long) computeMean(fails.stream().mapToDouble(Long::doubleValue).boxed().collect(java.util.stream.Collectors.toList()));
        long meanMemory = (long) computeMean(memories.stream().mapToDouble(Long::doubleValue).boxed().collect(java.util.stream.Collectors.toList()));
        
        String status = successCount == BENCHMARK_RUNS ? "SAT" : 
                       (successCount > 0 ? "PARTIAL(" + successCount + "/" + BENCHMARK_RUNS + ")" : "TIMEOUT");
        
        DecimalFormat df = new DecimalFormat("#.###");
        
        System.out.printf("%-15s | %dx%d | %-8d | %6.1f%% | %-20s | %s±%s | %-12d | %-12d | %-12d | %-10s%n", 
            instance.name, instance.n * instance.n, instance.n * instance.n, 
            instance.countGivenClues(), instance.getDensity() * 100,
            strategyName, df.format(meanTime), df.format(stdDevTime), 
            meanNodes, meanFails, meanMemory, status);
        
        // Store for CSV export
        allResults.add(new BenchmarkRecord(instance, strategyName, strategy, 
            meanTime, stdDevTime, meanNodes, meanFails, meanMemory, status));
    }
    
    private static BenchmarkResult solveSingle(SudokuInstance instance, String strategyName, SearchStrategy strategy) {
        int n = instance.n;       // Block size (e.g., 3)
        int N = n * n;            // Grid size (e.g., 9)
        
        // 1. Build Model
        Model model = new Model("Sudoku-" + N + "x" + N);
        IntVar[][] grid = model.intVarMatrix("c", N, N, 1, N);

        // Constraints: Rows & Columns
        for (int i = 0; i < N; i++) {
            model.allDifferent(grid[i]).post();
            model.allDifferent(getColumn(grid, i)).post();
        }

        // Constraints: Sub-grids (Blocks)
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                model.allDifferent(getBlock(grid, r, c, n)).post();
            }
        }

        // Constraints: Clues (Given numbers)
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (instance.grid[i][j] != 0) {
                    model.arithm(grid[i][j], "=", instance.grid[i][j]).post();
                }
            }
        }

        // 2. Configure Solver Parameters
        Solver solver = model.getSolver();
        IntVar[] allVars = flatten(grid);

        switch (strategy) {
            case DOM_OVER_WDEG:
                // Tuned CP Heuristic: Fail-First Variable Ordering + Restarts
                solver.setSearch(Search.domOverWDegSearch(allVars));
                // Luby Restart: Helps escape large thrashing subtrees on difficult problems
                solver.setLubyRestart(500, new FailCounter(model, 1), 2);
                break;
            case MIN_DOM_SIZE:
                // First-Fail Principle: Choose variable with smallest domain first
                solver.setSearch(Search.minDomLBSearch(allVars));
                break;
            case RANDOM:
                // Human/Random Heuristic: Naive variable ordering with randomization
                List<IntVar> randomVars = new ArrayList<>(Arrays.asList(allVars));
                Collections.shuffle(randomVars, new Random(System.nanoTime())); // Different seed each run
                solver.setSearch(Search.inputOrderLBSearch(randomVars.toArray(new IntVar[0])));
                break;
            case INPUT_ORDER:
            default:
                // Default Baseline: Naive sequential variable ordering
                solver.setSearch(Search.inputOrderLBSearch(allVars));
                break;
        }

        // Set time limit for all strategies
        solver.limitTime(TIME_LIMIT);
        
        // 3. Measure Memory Before
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest garbage collection for more accurate measurement
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 4. Solve & Measure
        solver.solve();

        // 5. Measure Memory After
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedKB = (memoryAfter - memoryBefore) / 1024;

        // 6. Collect Results
        double time = solver.getTimeCount();
        long nodes = solver.getNodeCount();
        long fails = solver.getFailCount();
        boolean success = solver.isFeasible() == ESat.TRUE && !solver.isStopCriterionMet();
        
        return new BenchmarkResult(time, nodes, fails, memoryUsedKB, success);
    }

    // ==========================================
    // UTILITIES & PARSING
    // ==========================================
    
    private static boolean downloadFile(String urlStr, File dest) {
        try (InputStream in = new URL(urlStr).openStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Download error for " + urlStr + ": " + e.getMessage());
            return false;
        }
    }

    private static IntVar[] flatten(IntVar[][] matrix) {
        return Arrays.stream(matrix).flatMap(Arrays::stream).toArray(IntVar[]::new);
    }

    private static IntVar[] getColumn(IntVar[][] grid, int col) {
        IntVar[] ret = new IntVar[grid.length];
        for (int i = 0; i < grid.length; i++) ret[i] = grid[i][col];
        return ret;
    }

    private static IntVar[] getBlock(IntVar[][] grid, int blockRow, int blockCol, int n) {
        IntVar[] ret = new IntVar[n * n];
        int idx = 0;
        int startRow = blockRow * n;
        int startCol = blockCol * n;
        
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                ret[idx++] = grid[startRow + r][startCol + c];
            }
        }
        return ret;
    }

    static class SudokuInstance {
        String name;
        int n; // block size
        int[][] grid;
        
        public SudokuInstance(String name, int n, int[] flatData) {
            this.name = name;
            this.n = n;
            int N = n * n;
            this.grid = new int[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    this.grid[i][j] = flatData[i * N + j];
                }
            }
        }
        
        public int countGivenClues() {
            int count = 0;
            int N = n * n;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid[i][j] != 0) count++;
                }
            }
            return count;
        }
        
        public double getDensity() {
            int N = n * n;
            return (double) countGivenClues() / (N * N);
        }
    }
    
    static class BenchmarkResult {
        double time;
        long nodes;
        long fails;
        long memoryKB;
        boolean success;
        
        public BenchmarkResult(double time, long nodes, long fails, long memoryKB, boolean success) {
            this.time = time;
            this.nodes = nodes;
            this.fails = fails;
            this.memoryKB = memoryKB;
            this.success = success;
        }
    }
    
    static class BenchmarkRecord {
        SudokuInstance instance;
        String strategyName;
        SearchStrategy strategy;
        double meanTime;
        double stdDevTime;
        long meanNodes;
        long meanFails;
        long meanMemory;
        String status;
        
        public BenchmarkRecord(SudokuInstance instance, String strategyName, SearchStrategy strategy,
                             double meanTime, double stdDevTime, long meanNodes, long meanFails, 
                             long meanMemory, String status) {
            this.instance = instance;
            this.strategyName = strategyName;
            this.strategy = strategy;
            this.meanTime = meanTime;
            this.stdDevTime = stdDevTime;
            this.meanNodes = meanNodes;
            this.meanFails = meanFails;
            this.meanMemory = meanMemory;
            this.status = status;
        }
    }

    // Final, robust MiniZinc Parser (Handles underscores, dimensions, and auto-detects size)
    static class MiniZincParser {
        public static SudokuInstance parse(File file) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder dataString = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments
                if (line.isEmpty() || line.startsWith("%")) continue;

                // FIX 1: Convert MiniZinc empty cell '_' to '0'
                line = line.replace("_", "0");

                // FIX 2: Remove dimension definitions (e.g., "1..n")
                line = line.replaceAll("\\d+\\.\\.[a-zA-Z0-9]+", " ");

                // FIX 3: Skip metadata lines (e.g., "n = 9;")
                if (line.matches(".*[a-zA-Z0-9]+\\s*=\\s*\\d+;")) continue;

                // Remove "array2d" keyword so the '2' isn't read
                line = line.replace("array2d", " ");

                // Now extract only the digits
                String cleanLine = line.replaceAll("[^0-9]", " ");
                dataString.append(cleanLine).append(" ");
            }
            reader.close();
            
            // Tokenize and Convert
            int[] data = Arrays.stream(dataString.toString().trim().split("\\s+"))
                             .filter(s -> !s.isEmpty())
                             .mapToInt(Integer::parseInt)
                             .toArray();
            
            // AUTO-DETECT Size Logic: Total Cells = N*N. N = sqrt(Total Cells). n = sqrt(N).
            int totalCells = data.length;
            int N = (int) Math.sqrt(totalCells); 
            int n = (int) Math.sqrt(N);          

            // Validation: Ensure it's a valid Sudoku size (a perfect fourth power)
            if (N * N != totalCells) {
                 throw new IOException("Invalid data count: Found " + totalCells + 
                                       " numbers. Not a valid Sudoku square (N^2).");
            }
            
            return new SudokuInstance(file.getName(), n, data);
        }
    }
    
    // ==========================================
    // STATISTICAL UTILITIES
    // ==========================================
    
    private static double computeMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private static double computeStdDev(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private static void writeResultsToCSV(List<BenchmarkRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_OUTPUT))) {
            // Write header
            writer.println("Instance,Size,Clues,Density,Strategy,MeanTime,StdDevTime,MeanNodes,MeanFails,MeanMemoryKB,Status");
            
            // Write data
            for (BenchmarkRecord record : records) {
                int N = record.instance.n * record.instance.n;
                writer.printf("%s,%d,%d,%.4f,%s,%.6f,%.6f,%d,%d,%d,%s%n",
                    record.instance.name,
                    N,
                    record.instance.countGivenClues(),
                    record.instance.getDensity(),
                    record.strategyName,
                    record.meanTime,
                    record.stdDevTime,
                    record.meanNodes,
                    record.meanFails,
                    record.meanMemory,
                    record.status);
            }
            
            System.out.println("\nCSV export complete: " + CSV_OUTPUT);
        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
        }
    }
}
