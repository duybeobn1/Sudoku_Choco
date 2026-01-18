# Sudoku Solver & Benchmark Suite

A high-performance Sudoku Solver and Benchmarking platform combining **Constraint Programming (Choco Solver)** and **Heuristic Search**. It features a modern Web UI for interactive solving and real-time benchmark visualization.

## 🚀 Quick Start

### 1. Build the Backend
Prerequisites: Java 17+, Maven.

```bash
cd back
# Compile and package the project into a JAR
mvn clean package
```

### 2. Run the API Server
Start the backend server which handles solving logic, benchmarking, and file serving.

```bash
# Run the JAR in API mode
java -jar target/sudoku-solver-1.0-SNAPSHOT.jar api
```
*The server will start on `http://localhost:8080`.*

### 3. Launch the Frontend
Open the `front/index.html` file in your browser.
*   **Recommended:** Use a local server (e.g., VS Code **Live Server**) to serve the `front` folder on `http://127.0.0.1:5500`.
*   *Note: Ensure the backend API is running before using the web interface.*

---

## ✨ Features

### 🧩 Interactive Solver
*   **Multiple Grid Sizes:** Supports standard 9x9, but also 4x4, 16x16, and 25x25 grids.
*   **Example Loading:** Instantly load benchmark instances (e.g., `p0`, `p36`) from the server.
*   **Real-time Solving:**
    *   **Complete Solver:** Uses **Choco Solver** (Constraint Programming) with configurable strategies (InputOrder, DomOverWDeg, Luby Restarts).
    *   **Incomplete Solver:** Backtracking with heuristics (Degree, MRV).
    *   **Greedy Solver:** Extremely fast constructive heuristic (non-guaranteed).

### 📊 Benchmark Suite
*   **Real-time Streaming (SSE):** Watch benchmark progress live on the web dashboard.
*   **Live Charts:**
    *   Average Time by Configuration.
    *   Success Rate by Difficulty.
    *   **Method Comparison:** Visual comparison of Complete vs. Incomplete vs. Greedy performance.
*   **Persistence:**
    *   Results are automatically saved to `benchmark_results.csv`.
    *   History is reloaded automatically when reopening the dashboard.
*   **MiniZinc Support:** Automatically downloads and parses `.dzn` benchmark files.

---

## 🏗️ Architecture

### Backend (Java)
*   **Core Logic:** `com.sudoku.solver` containing the CP model and backtracking algorithms.
*   **API Layer:** `com.sudoku.api.SudokuApi` using standard `com.sun.net.httpserver`.
    *   `POST /api/solve/{type}`: Solves a given grid.
    *   `GET /api/benchmark`: Streams benchmark results via Server-Sent Events (SSE).
    *   `GET /api/benchmark/history`: Serves the saved CSV file.
    *   `GET /api/example/{id}`: Loads `.dzn` files from `benchmarks_data/`.
*   **Utils:** `PuzzleParser` for robust MiniZinc data extraction.

### Frontend (HTML/CSS/JS)
*   **Vanilla JS:** No heavy framework dependencies.
*   **Chart.js:** For rendering benchmark analytics.
*   **Dynamic UI:** CSS Grid for responsive Sudoku boards (up to 25x25).

---

## 📂 Project Structure

```
.
├── src/main/java/com/sudoku/
│   ├── api/            # HTTP Handlers (Solve, Benchmark, History)
│   ├── benchmark/      # Benchmark Runner & Logic
│   ├── solver/         # Solver Implementations (Complete, Greedy...)
│   └── util/           # PuzzleParser (MiniZinc .dzn)
├── front/              # Web Interface
│   ├── index.html      # Main Dashboard
│   ├── style.css       # Teal/Light Theme
│   └── js/             # app.js (Logic) & api.js (Fetch calls)
├── benchmarks_data/    # Downloaded .dzn files (Auto-generated)
├── benchmark_results.csv # Persistent results file (Auto-generated)
└── pom.xml             # Maven Configuration
```

---

## 🎯 Solver Strategies

### Complete Solver (Constraint Programming)
Guarantees a solution if one exists using **Choco Solver**.

| Strategy | Description |
|----------|-------------|
| `InputOrder` | Variables assigned in input order (baseline) |
| `DomOverWDeg` | Domain over Weighted Degree heuristic (advanced) |
| `Luby` | Luby sequence-based restart strategy (for hard instances) |

### Incomplete Solver
Fast backtracking with preprocessing heuristics.

| Heuristic | Description |
|-----------|-------------|
| `Degree` | Select cell with most constraints |
| `MRV` | Minimum Remaining Values (most constrained first) |

### Greedy Solver
Extremely fast constructive heuristic (no backtracking, no guarantee).

---

## 📊 Benchmark Results

Results are persisted in `benchmark_results.csv` with the following columns:

```
Instance,Difficulty,Strategy,Value,Constraints,Restart,SolverType,TimeMs,Iterations,Backtracks,Status
p0,Easy,InputOrder,9,324,Luby,Complete,145.32,1250,45,SOLVED
p1,Medium,DomOverWDeg,9,324,Luby,Complete,1892.15,8920,2341,SOLVED
p2,Hard,Greedy,9,324,None,Greedy,0.12,0,0,TIMEOUT
```

---

## ⚙️ Configuration

### Backend
*   **Benchmark Instances:** Edit the difficulty lists in `SudokuBenchmark.java`:
    ```java
    private static final int[] EASY = {0, 1, 2, ...};
    private static final int[] MEDIUM = {10, 11, 12, ...};
    private static final int[] HARD = {50, 51, 52, ...};
    ```

*   **Download Source:** MiniZinc data is auto-fetched from the configured URL.

### Frontend
*   **API Base URL:** Change in `api.js` if backend runs on a different port:
    ```javascript
    const API_BASE = 'http://localhost:8080';
    ```

---

## 🧪 Running Tests

### Single Instance Solver
1.  Click on **Solver** tab.
2.  Select a grid size (default: 9x9).
3.  (Optional) Load an example puzzle.
4.  Choose solver type and strategy.
5.  Click **Solve**.

### Full Benchmark Suite
1.  Click on **Benchmark** tab.
2.  Charts and table populate automatically from `benchmark_results.csv` on page load.
3.  Click **Start Global Benchmark** to run a fresh suite.
4.  Watch real-time progress with live chart updates.
5.  Results are saved to disk automatically.

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| **Port 8080 already in use** | Change the port in `SudokuApi.java` or kill the existing process |
| **CORS errors** | Backend is configured with CORS headers; ensure both frontend and backend URLs are correct |
| **Charts not rendering** | Refresh the page after benchmark completes; ensure `chart.js` library is loaded |

---

## 📦 Dependencies

### Backend
*   **Java 17+**
*   **Choco Solver 4.10.x** (Constraint Programming)
*   **Maven** (Build tool)

### Frontend
*   **Chart.js 3.x** (Analytics visualization)
*   **Vanilla JavaScript** (No framework)
*   **Modern Browser** (ES6+ support)