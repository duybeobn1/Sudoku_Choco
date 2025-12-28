package src.model;

/**
 * Represents the result of solving a Sudoku puzzle.
 * Includes the solution grid and performance metrics.
 */
public class SolverResult {
    private int[][] solution;
    private boolean solved;
    private long timeMs;
    private long iterations;
    private long backtracks;
    private String solverName;

    public SolverResult(String solverName) {
        this.solverName = solverName;
        this.solved = false;
        this.timeMs = 0;
        this.iterations = 0;
        this.backtracks = 0;
    }

    // Getters and Setters
    public int[][] getSolution() {
        return solution;
    }

    public void setSolution(int[][] solution) {
        this.solution = solution;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public long getIterations() {
        return iterations;
    }

    public void setIterations(long iterations) {
        this.iterations = iterations;
    }

    public long getBacktracks() {
        return backtracks;
    }

    public void setBacktracks(long backtracks) {
        this.backtracks = backtracks;
    }

    public String getSolverName() {
        return solverName;
    }

    /**
     * Print the result to console.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(solverName).append(" Result ===\n");
        sb.append("Solved: ").append(solved).append("\n");
        sb.append("Time: ").append(timeMs).append(" ms\n");
        sb.append("Iterations: ").append(iterations).append("\n");
        sb.append("Backtracks: ").append(backtracks).append("\n");
        if (solved && solution != null) {
            sb.append("\nSolution:\n");
            for (int i = 0; i < solution.length; i++) {
                for (int j = 0; j < solution[i].length; j++) {
                    sb.append(solution[i][j]).append(" ");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
