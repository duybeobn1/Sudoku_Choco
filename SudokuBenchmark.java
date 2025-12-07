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

    public static void main(String[] args) {
        System.out.println("=== SUDOKU SOLVER BENCHMARK (Choco Solver) ===");
        
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

        // 2. Run Comparison
        // Updated header to include the new 'Fails' column
        System.out.printf("%n%-15s | %-6s | %-20s | %-10s | %-10s | %-10s | %-10s%n", 
            "Instance", "Size", "Strategy", "Time(s)", "Nodes", "Fails", "Result");
        System.out.println("---------------------------------------------------------------------------------------------------");

        for (SudokuInstance inst : instances) {
            // METHOD A: Naive Baseline (InputOrder)
            solve(inst, "Default/InputOrder", SearchStrategy.INPUT_ORDER);

            // METHOD B: Optimized CP Heuristic (DomOverWDeg + Restarts)
            solve(inst, "Tuned/DomOverWDeg", SearchStrategy.DOM_OVER_WDEG);
            
            // METHOD C: Human-Inspired Heuristic (Random Order)
            // Simulates a non-sequential, non-optimized "human glance" approach for a third baseline
            solve(inst, "Human/RandomOrder", SearchStrategy.RANDOM);

            System.out.println("---------------------------------------------------------------------------------------------------");
        }
    }

    // ==========================================
    // SOLVER ENGINE & STRATEGIES
    // ==========================================
    
    private enum SearchStrategy {
        INPUT_ORDER, DOM_OVER_WDEG, RANDOM
    }
    
    private static void solve(SudokuInstance instance, String strategyName, SearchStrategy strategy) {
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
                // Using the older stable method signature: (base, counter, limit)
                solver.setLubyRestart(500, new FailCounter(model, 1), 2);
                break;
            case RANDOM:
                // Human/Random Heuristic: Naive variable ordering with randomization
                List<IntVar> randomVars = new ArrayList<>(Arrays.asList(allVars));
                Collections.shuffle(randomVars, new Random(42)); // Fixed seed for reproducible random order
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

        // 3. Solve & Measure
        solver.solve();

        // 4. Report
        String time = String.format("%.3f", solver.getTimeCount());
        long nodes = solver.getNodeCount();
        long fails = solver.getFailCount(); // Captures the number of backtracks/conflicts
        
        String status = solver.isFeasible() == ESat.TRUE ? "SAT" : "UNKNOWN";
        
        if (solver.isStopCriterionMet()) status = "TIMEOUT";

        // Print results including Failures
        System.out.printf("%-15s | %dx%d | %-20s | %-10s | %-10d | %-10d | %-10s%n", 
            instance.name, N, N, strategyName, time, nodes, fails, status);
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
}