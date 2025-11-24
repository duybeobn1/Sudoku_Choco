import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
// NEW IMPORTS for robustness: ESat
import org.chocosolver.util.ESat;
// REMOVED: import org.chocosolver.solver.search.restart.Restarts;

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
    
    private static final int[] INSTANCE_INDICES = {0, 1, 36, 89}; 

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
                e.printStackTrace();
            }
        }

        // 2. Run Comparison
        System.out.printf("%n%-15s | %-6s | %-20s | %-10s | %-10s | %-10s%n", 
            "Instance", "Size", "Strategy", "Time(s)", "Nodes", "Result");
        System.out.println("----------------------------------------------------------------------------------------");

        for (SudokuInstance inst : instances) {
            // METHOD A: General / Naive (InputOrder)
            solve(inst, "Default/InputOrder", false);

            // METHOD B: Optimized / Tuned (DomOverWDeg + Restarts)
            solve(inst, "Tuned/DomOverWDeg", true);
            
            System.out.println("----------------------------------------------------------------------------------------");
        }
    }

    // ==========================================
    // SOLVER ENGINE
    // ==========================================
    private static void solve(SudokuInstance instance, String strategyName, boolean optimized) {
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
                if (instance.grid[i][j]!= 0) {
                    model.arithm(grid[i][j], "=", instance.grid[i][j]).post();
                }
            }
        }

        // 2. Configure Solver Parameters (The Core Task)
        Solver solver = model.getSolver();
        
        IntVar[] allVars = flatten(grid);

        if (optimized) {
            // --- MODIFIED PARAMETERS ---
            solver.setSearch(Search.domOverWDegSearch(allVars));
            
            // FIX E: Corrected Luby Restart using the older, stable setLubyRestart signature
            // (base, counter, scaleFactor)
            solver.setLubyRestart(500, new FailCounter(model, 1), 2);

        } else {
            // --- DEFAULT / GENERAL PARAMETERS ---
            solver.setSearch(Search.inputOrderLBSearch(allVars));
        }

        // Limit time to prevent infinite hangs on the naive method
        solver.limitTime("10s"); 

        // 3. Solve & Measure
        solver.solve();

        // 4. Report
        String time = String.format("%.3f", solver.getTimeCount());
        long nodes = solver.getNodeCount();
        
        // FIX F: Status check using ESat
        String status = solver.isFeasible() == ESat.TRUE ? "SAT" : "UNKNOWN";
        
        if (solver.isStopCriterionMet()) status = "TIMEOUT";

        System.out.printf("%-15s | %dx%d | %-20s | %-10s | %-10d | %-10s%n", 
            instance.name, N, N, strategyName, time, nodes, status);
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

    // Parser for Hakank's .dzn files (Handles underscores and dimensions)
    static class MiniZincParser {
        public static SudokuInstance parse(File file) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder dataString = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments
                if (line.isEmpty() || line.startsWith("%")) continue;

                // CRITICAL FIX 1: Convert MiniZinc empty cell '_' to '0'
                line = line.replace("_", "0");

                // CRITICAL FIX 2: Remove dimension definitions like "1..n" or "1..9"
                // Otherwise, the parser reads these '1's as part of the puzzle.
                line = line.replaceAll("\\d+\\.\\.[a-zA-Z0-9]+", " ");

                // CRITICAL FIX 3: Skip metadata lines like "n = 9;" or "int: N = 9;"
                // We only want the array data. 
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
            
            // AUTO-DETECT Size Logic
            // Total Cells = N*N. Therefore N = sqrt(Total Cells).
            int totalCells = data.length;
            int N = (int) Math.sqrt(totalCells); 
            // In Sudoku, N is n*n (e.g., 9 = 3*3). So n = sqrt(N).
            int n = (int) Math.sqrt(N);          

            // Validation: Ensure it's a perfect square
            if (N * N != totalCells) {
                 throw new IOException("Invalid data count: Found " + totalCells + 
                                       " numbers. This is not a perfect square (e.g., 81, 256). " +
                                       "Check if dimensions (1..n) or underscores (_) are being parsed incorrectly.");
            }
            
            return new SudokuInstance(file.getName(), n, data);
        }
    }
}