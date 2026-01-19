package com.sudoku.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;
import com.sudoku.benchmark.SudokuBenchmark;
import com.sudoku.solver.*;
import com.sudoku.util.MiniZincParser;
import com.sudoku.util.MiniZincParser.SudokuInstance;

import java.util.Arrays;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Lightweight HTTP API for the Sudoku Solver.
 * Endpoints:
 * POST /api/solve/complete
 * POST /api/solve/incomplete
 * POST /api/solve/greedy
 * GET /api/benchmark
 * GET /api/benchmark/history
 * GET /api/example/{id}
 */
public class SudokuApi {
    private static final int PORT = 8080;
    private static final String BENCHMARK_DIR = "benchmarks_data";

    public static void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/solve/complete", new SolveHandler("complete"));
        server.createContext("/api/solve/incomplete", new SolveHandler("incomplete"));
        server.createContext("/api/solve/greedy", new SolveHandler("greedy"));
        server.createContext("/api/benchmark", new BenchmarkHandler());
        server.createContext("/api/example/", new ExampleHandler());
        server.createContext("/api/benchmark/history", new HistoryHandler());

        server.setExecutor(null); // Default executor
        System.out.println("API Server started on port " + PORT);
        server.start();
    }

    /**
     * Adds standard CORS headers to allow requests from any origin.
     */
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    static class SolveHandler implements HttpHandler {
        private final String type;

        public SolveHandler(String type) {
            this.type = type;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // 1. Read Request Body
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // 2. Parse Grid
            int[][] data = parseGridFromJson(body);
            if (data == null) {
                String err = "Invalid JSON format";
                exchange.sendResponseHeaders(400, err.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(err.getBytes()); }
                return;
            }

            SudokuGrid grid = new SudokuGrid(data, (int) Math.sqrt(data.length));

            // 3. Solve
            SudokuSolver solver;
            switch (type) {
                case "complete": solver = new CompleteSolver(grid); break;
                case "greedy": solver = new GreedyIncompleteSolver(grid); break;
                default: solver = new IncompleteSolver(grid); break;
            }

            SolverResult result = solver.solve();

            // 4. Send Response
            String jsonResponse = toJson(result);
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class BenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Specific headers for Server-Sent Events (SSE)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Send 200 OK immediately to keep connection open
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody();
                 PrintWriter writer = new PrintWriter(os, false, StandardCharsets.UTF_8)) {
                
                SudokuBenchmark.runBenchmark((csvLine) -> {
                    System.out.println("Sending: " + csvLine);
                    writer.print("data: " + csvLine + "\n\n");
                    writer.flush();
                });

                writer.print("data: end\n\n");
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ExampleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Parse ID from URL: /api/example/36
            String path = exchange.getRequestURI().getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);

            try {
                int id = Integer.parseInt(idStr);
                String filename = "sudoku_p" + id + ".dzn";
                File file = new File(BENCHMARK_DIR, filename);

                if (!file.exists()) {
                    String msg = "{\"error\": \"File not found. Run benchmark first to download.\"}";
                    exchange.sendResponseHeaders(404, msg.length());
                    try (OutputStream os = exchange.getResponseBody()) { os.write(msg.getBytes()); }
                    return;
                }

                // Use the Parser
                SudokuInstance instance = MiniZincParser.parse(file, "Unknown");
                
                // Convert to JSON array of arrays
                String json = gridToJson(instance.grid);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(json.getBytes()); }

            } catch (Exception e) {
                e.printStackTrace();
                String err = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, err.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(err.getBytes()); }
            }
        }

        private String gridToJson(int[][] grid) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < grid.length; i++) {
                sb.append(Arrays.toString(grid[i]));
                if (i < grid.length - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            File csv = new File("benchmark_results.csv");
            if (!csv.exists()) {
                String empty = "Instance,Diff,Strategy,Val,Cons,Restart,Solver,Time,Iter,Back,Status\n";
                exchange.getResponseHeaders().add("Content-Type", "text/csv");
                exchange.sendResponseHeaders(200, empty.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(empty.getBytes()); }
                return;
            }

            byte[] bytes = Files.readAllBytes(csv.toPath());
            exchange.getResponseHeaders().add("Content-Type", "text/csv");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    // --- JSON Helpers ---

    private static int[][] parseGridFromJson(String json) {
        try {
            String clean = json.replace("[", "").replace("]", "").trim();
            if (clean.isEmpty()) return new int[0][0];

            String[] tokens = clean.split(",");
            int total = tokens.length;
            int size = (int) Math.sqrt(total);
            
            int[][] grid = new int[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    String t = tokens[i * size + j].trim();
                    if(!t.isEmpty()) {
                        grid[i][j] = Integer.parseInt(t);
                    }
                }
            }
            return grid;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String toJson(SolverResult res) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"solver\": \"").append(res.getSolverName()).append("\",");
        sb.append("\"solved\": ").append(res.isSolved()).append(",");
        sb.append("\"timeMs\": ").append(res.getTimeMs()).append(",");
        sb.append("\"solution\": ");

        if (res.getSolution() != null) {
            sb.append("[");
            int[][] sol = res.getSolution();
            for (int i = 0; i < sol.length; i++) {
                sb.append(Arrays.toString(sol[i]));
                if (i < sol.length - 1) sb.append(",");
            }
            sb.append("]");
        } else {
            sb.append("null");
        }
        sb.append("}");
        return sb.toString();
    }
}
