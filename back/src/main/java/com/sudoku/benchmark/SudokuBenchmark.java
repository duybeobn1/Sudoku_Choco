package com.sudoku.benchmark;

import com.sudoku.heuristics.*;
import com.sudoku.model.*;
import com.sudoku.solver.*;
import com.sudoku.solver.CompleteSolver.*; // Enums
import com.sudoku.util.MiniZincParser;

import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;

public class SudokuBenchmark {

    private static final String BASE_URL = "https://www.hakank.org/minizinc/sudoku_problems2/";
    private static final String LOCAL_DIR = "benchmarks_data";
    private static final int[] EASY = {0};
    private static final int[] MEDIUM = {36};
    private static final int[] HARD = {89};
    private static final String CSV_FILE = "benchmark_results.csv";

    public static void runBenchmark(Consumer<String> resultStreamer) {
        // We use a FileWriter to persist results to disk alongside streaming them
        try (PrintWriter fileWriter = new PrintWriter(new FileWriter(CSV_FILE))) {
            
            File dir = new File(LOCAL_DIR);
            if (!dir.exists()) dir.mkdirs();

            // CSV Header
            String header = "Instance,Difficulty,Strategy,Val,Cons,Restart,SolverType,Time(ms),Iter,Back,Status";
            
            // Send Header to API and File
            resultStreamer.accept(header);
            fileWriter.println(header);

            System.out.println("Starting Benchmark Suite...");

            // Pass fileWriter to runSuite
            runSuite(EASY, "Easy", resultStreamer, fileWriter);
            runSuite(MEDIUM, "Medium", resultStreamer, fileWriter);
            runSuite(HARD, "Hard", resultStreamer, fileWriter);
            
            System.out.println("Benchmark finished. Results saved to " + CSV_FILE);

        } catch (Exception e) {
            e.printStackTrace();
            resultStreamer.accept("Error: " + e.getMessage());
        }
    }

    private static void runSuite(int[] indices, String difficulty, Consumer<String> streamer, PrintWriter fileWriter) {
        for (int index : indices) {
            String filename = "sudoku_p" + index + ".dzn";
            try {
                File file = downloadIfNotExists(filename);
                
                MiniZincParser.SudokuInstance instance = MiniZincParser.parse(file, difficulty);
                SudokuGrid grid = new SudokuGrid(instance.grid, instance.n);

                // ==========================
                // 1. COMPLETE SOLVER CONFIGS
                // ==========================
                
                // Config 1: Baseline
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.INPUT_ORDER, ValueHeuristic.MIN, "DEFAULT", RestartType.NONE, 0, 0);

                // Config 2: InputOrder + Luby
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.INPUT_ORDER, ValueHeuristic.MIN, "DEFAULT", RestartType.LUBY, 100, 2);

                // Config 3: DomOverWDeg (Smart)
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "DEFAULT", RestartType.NONE, 0, 0);

                // Config 4: DomOverWDeg + Luby + AC
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "AC", RestartType.LUBY, 100, 2);

                // Config 5: Random + Geometric
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.RANDOM_VAL, "DEFAULT", RestartType.GEOMETRIC, 10, 1.1);

                // ==========================
                // 2. INCOMPLETE SOLVERS
                // ==========================

                // MRV
                IncompleteSolver incMrv = new IncompleteSolver(grid);
                incMrv.setHeuristic(new MRVHeuristic());
                testSolver(incMrv, "Incomplete_MRV", "-", "-", "-", "-", filename, difficulty, streamer, fileWriter);

                // Degree
                IncompleteSolver incDeg = new IncompleteSolver(grid);
                incDeg.setHeuristic(new DegreeHeuristic());
                testSolver(incDeg, "Incomplete_Degree", "-", "-", "-", "-", filename, difficulty, streamer, fileWriter);

                // Greedy
                testSolver(new GreedyIncompleteSolver(grid), "Greedy_MRV", "-", "-", "-", "-", filename, difficulty, streamer, fileWriter);

            } catch (Exception e) {
                System.err.println("Error processing " + filename + ": " + e.getMessage());
            }
        }
    }

    private static void testComplete(SudokuGrid grid, String instance, String diff, Consumer<String> streamer, PrintWriter fileWriter,
                                     SearchStrategy strat, ValueHeuristic val, String cons, RestartType rest, int base, double fact) {
        
        CompleteSolver solver = new CompleteSolver(grid);
        solver.setStrategy(strat);
        solver.setValueHeuristic(val);
        solver.setConsistencyLevel(cons);
        solver.setRestart(rest, base, fact);
        
        String restartStr = (rest == RestartType.NONE) ? "-" : rest.toString() + "(" + base + ")";
        
        testSolver(solver, "Complete", strat.toString(), val.toString(), cons, restartStr, instance, diff, streamer, fileWriter);
    }

    private static void testSolver(SudokuSolver solver, String type, String strat, String val, String cons, String rest, String instance, String diff, Consumer<String> streamer, PrintWriter fileWriter) {
        // Print "Running..." to console
        String configStr = String.format("%s | %s | %s", type, strat, val);
        System.out.print("Running " + instance + " [" + configStr + "] ... ");
        
        SolverResult result;
        try {
            result = solver.solve();
        } catch (Throwable t) {
            System.out.println("CRASH: " + t.getMessage());
            return; // Skip streaming a broken line, or stream an error line
        }

        String status = result.isSolved() ? "SOLVED" : "TIMEOUT";
        if (!result.isSolved() && result.getTimeMs() < 500) status = "FAILED";

        // Print Result to console
        System.out.println(status + " (" + result.getTimeMs() + "ms)");

        String line = String.format("%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%s",
                instance, diff, strat, val, cons, rest, type,
                result.getTimeMs(), result.getIterations(), result.getBacktracks(), status);
        
        streamer.accept(line);
        fileWriter.println(line);
        try { Thread.sleep(20); } catch (Exception e) {}
    }

    
    // ... (Keep downloadIfNotExists, SudokuInstance, and MiniZincParser exactly as before) ...
    private static File downloadIfNotExists(String filename) throws IOException {
        File file = new File(LOCAL_DIR, filename);
        if (file.exists() && file.length() > 0) return file;
        
        System.out.println("Downloading " + filename + "...");
        java.net.URL url = new java.net.URL(BASE_URL + filename);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (file.exists()) file.delete();
            throw e;
        }
        return file;
    }
}
