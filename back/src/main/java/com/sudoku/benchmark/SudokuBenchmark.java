package com.sudoku.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream; // Enums
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import com.sudoku.heuristics.DegreeHeuristic;
import com.sudoku.heuristics.MRVHeuristic;
import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;
import com.sudoku.solver.CompleteSolver;
import com.sudoku.solver.CompleteSolver.RestartType;
import com.sudoku.solver.CompleteSolver.SearchStrategy;
import com.sudoku.solver.CompleteSolver.ValueHeuristic;
import com.sudoku.solver.GreedyIncompleteSolver;
import com.sudoku.solver.IncompleteSolver;
import com.sudoku.solver.SudokuSolver;
import com.sudoku.util.MiniZincParser;

public class SudokuBenchmark {

    private static final String BASE_URL = "https://www.hakank.org/minizinc/sudoku_problems2/";
    private static final String LOCAL_DIR = "benchmarks_data";
    private static final String CSV_FILE = "benchmark_results.csv";
    private static final int[] EASY = { 0, 5, 8 };
    private static final int[] MEDIUM = { 36, 41, 44 };
    private static final int[] HARD = { 89 };
    private static final String[] GENERATED = {

            "sudoku_16x16_d02_1.dzn",
            "sudoku_16x16_d02_2.dzn",
            "sudoku_16x16_d03_1.dzn",
            "sudoku_16x16_d03_2.dzn",
            "sudoku_25x25_d02_1.dzn",
            "sudoku_25x25_d02_2.dzn",
            "sudoku_25x25_d03_1.dzn",
            "sudoku_25x25_d03_2.dzn",
            "sudoku_25x25_d04_1.dzn",
            "sudoku_25x25_d04_2.dzn",
    };

    public static void runBenchmark(Consumer<String> resultStreamer) {
        // We use a FileWriter to persist results to disk alongside streaming them
        try (PrintWriter fileWriter = new PrintWriter(new FileWriter(CSV_FILE))) {

            File dir = new File(LOCAL_DIR);
            if (!dir.exists())
                dir.mkdirs();

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
            runGeneratedSuite(GENERATED, "Generated", resultStreamer, fileWriter);

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
                        SearchStrategy.INPUT_ORDER, ValueHeuristic.MIN, "AC", RestartType.LUBY, 100, 2);

                // Config 3: DomOverWDeg (Smart)
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "DEFAULT", RestartType.NONE, 0, 0);

                // Config 4: DomOverWDeg + Luby + AC
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "AC", RestartType.LUBY, 100, 2);

                // Config 5: Random + Geometric
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.RANDOM_VAL, "DEFAULT", RestartType.GEOMETRIC, 10,
                        1.1);

                // --- DOM OVER WDEG VARIATIONS ---

                // Config 6: The "Champion" Candidate (Max Val + AC + Luby 500)
                // Rationale: Previous logs showed MAX heuristic + AC solved Hard instances
                // instantly.
                // Luby 500 gives enough time to learn weights before restarting.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MAX, "AC", RestartType.LUBY, 500, 2);

                // Config 7: Geometric Growth (Fast start, wide search)
                // Rationale: Geometric restarts (x1.5) grow faster than Luby.
                // Combined with MIN value to see if standard ordering works better with
                // aggressive restarts.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "DEFAULT", RestartType.GEOMETRIC, 100, 1.5);

                // Config 8: High Frequency Learning (Low Luby Base)
                // Rationale: A very low Luby base (10) forces frequent restarts early on.
                // This forces DomOverWDeg to update weights very rapidly in the beginning.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.RANDOM_VAL, "AC", RestartType.LUBY, 10, 2);

                // --- MIN DOM SIZE VARIATIONS ---

                // Config 9: Classic "First-Fail" (MinDom + AC)
                // Rationale: MinDomSize works best when domains are kept small by Arc
                // Consistency (AC).
                // This is the "Textbook" constraint programming approach.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.MIN, "AC", RestartType.LUBY, 200, 2);

                // Config 10: Randomized First-Fail
                // Rationale: MinDom usually picks the "hardest" variable, but if we pick the
                // wrong value (MIN), we get stuck.
                // RANDOM_VAL helps escape the specific value trap while still focusing on
                // difficult variables.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.RANDOM_VAL, "DEFAULT", RestartType.GEOMETRIC, 50,
                        1.2);
                // Config 11: Randomized First-Fail with AC
                // Rationale: Combining RANDOM_VAL with AC to maintain arc consistency while
                // exploring.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.RANDOM_VAL, "AC", RestartType.LUBY, 100, 2);

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
                testSolver(new GreedyIncompleteSolver(grid), "Greedy_MRV", "-", "-", "-", "-", filename, difficulty,
                        streamer, fileWriter);

            } catch (Exception e) {
                System.err.println("Error processing " + filename + ": " + e.getMessage());
            }
        }
    }

    private static void runGeneratedSuite(String[] filenames, String difficulty, Consumer<String> streamer,
            PrintWriter fileWriter) {
        for (String filename : filenames) {
            try {
                File file = new File(LOCAL_DIR + "/generates/generated_instances/" + filename);
                if (!file.exists()) {
                    System.err.println("File not found: " + file.getPath());
                    continue;
                }

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
                        SearchStrategy.INPUT_ORDER, ValueHeuristic.MIN, "AC", RestartType.LUBY, 100, 2);

                // Config 3: DomOverWDeg (Smart)
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "DEFAULT", RestartType.NONE, 0, 0);

                // Config 4: DomOverWDeg + Luby + AC
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "AC", RestartType.LUBY, 100, 2);

                // Config 5: Random + Geometric
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.RANDOM_VAL, "DEFAULT", RestartType.GEOMETRIC, 10,
                        1.1);

                // --- DOM OVER WDEG VARIATIONS ---

                // Config 6: The "Champion" Candidate (Max Val + AC + Luby 500)
                // Rationale: Previous logs showed MAX heuristic + AC solved Hard instances
                // instantly.
                // Luby 500 gives enough time to learn weights before restarting.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MAX, "AC", RestartType.LUBY, 500, 2);

                // Config 7: Geometric Growth (Fast start, wide search)
                // Rationale: Geometric restarts (x1.5) grow faster than Luby.
                // Combined with MIN value to see if standard ordering works better with
                // aggressive restarts.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.MIN, "DEFAULT", RestartType.GEOMETRIC, 100, 1.5);

                // Config 8: High Frequency Learning (Low Luby Base)
                // Rationale: A very low Luby base (10) forces frequent restarts early on.
                // This forces DomOverWDeg to update weights very rapidly in the beginning.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.DOM_OVER_WDEG, ValueHeuristic.RANDOM_VAL, "AC", RestartType.LUBY, 10, 2);

                // --- MIN DOM SIZE VARIATIONS ---

                // Config 9: Classic "First-Fail" (MinDom + AC)
                // Rationale: MinDomSize works best when domains are kept small by Arc
                // Consistency (AC).
                // This is the "Textbook" constraint programming approach.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.MIN, "AC", RestartType.LUBY, 200, 2);

                // Config 10: Randomized First-Fail
                // Rationale: MinDom usually picks the "hardest" variable, but if we pick the
                // wrong value (MIN), we get stuck.
                // RANDOM_VAL helps escape the specific value trap while still focusing on
                // difficult variables.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.RANDOM_VAL, "DEFAULT", RestartType.GEOMETRIC, 50,
                        1.2);
                // Config 11: Randomized First-Fail with AC
                // Rationale: Combining RANDOM_VAL with AC to maintain arc consistency while
                // exploring.
                testComplete(grid, filename, difficulty, streamer, fileWriter,
                        SearchStrategy.MIN_DOM_SIZE, ValueHeuristic.RANDOM_VAL, "AC", RestartType.LUBY, 100, 2);

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
                testSolver(new GreedyIncompleteSolver(grid), "Greedy_MRV", "-", "-", "-", "-", filename, difficulty,
                        streamer, fileWriter);

            } catch (Exception e) {
                System.err.println("Error processing " + filename + ": " + e.getMessage());
            }
        }
    }

    private static void testComplete(SudokuGrid grid, String instance, String diff, Consumer<String> streamer,
            PrintWriter fileWriter,
            SearchStrategy strat, ValueHeuristic val, String cons, RestartType rest, int base, double fact) {

        CompleteSolver solver = new CompleteSolver(grid);
        solver.setStrategy(strat);
        solver.setValueHeuristic(val);
        solver.setConsistencyLevel(cons);
        solver.setRestart(rest, base, fact);

        String restartStr = (rest == RestartType.NONE) ? "-" : rest.toString() + "(" + base + ")";

        testSolver(solver, "Complete", strat.toString(), val.toString(), cons, restartStr, instance, diff, streamer,
                fileWriter);
    }

    private static void testSolver(SudokuSolver solver, String type, String strat, String val, String cons, String rest,
            String instance, String diff, Consumer<String> streamer, PrintWriter fileWriter) {
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
        if (!result.isSolved() && result.getTimeMs() < 500)
            status = "FAILED";

        // Print Result to console
        System.out.println(status + " (" + result.getTimeMs() + "ms)");

        String line = String.format("%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%s",
                instance, diff, strat, val, cons, rest, type,
                result.getTimeMs(), result.getIterations(), result.getBacktracks(), status);

        streamer.accept(line);
        fileWriter.println(line);
        try {
            Thread.sleep(20);
        } catch (Exception e) {
        }
    }


    private static File downloadIfNotExists(String filename) throws IOException {
        File file = new File(LOCAL_DIR, filename);
        if (file.exists() && file.length() > 0)
            return file;

        System.out.println("Downloading " + filename + "...");
        java.net.URL url = new java.net.URL(BASE_URL + filename);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (file.exists())
                file.delete();
            throw e;
        }
        return file;
    }

    public static void main(String[] args) {
        // Regenerates benchmark_results.csv 
        runBenchmark(System.out::println);
    }
}
