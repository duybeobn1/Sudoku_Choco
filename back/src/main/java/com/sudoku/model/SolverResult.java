package com.sudoku.model;

/**
 * Encapsulates the result of a solver attempt.
 */
public class SolverResult {

    private final String solverName;
    private boolean solved;
    private int[][] solution;
    private long timeMs;
    private long iterations;
    private long backtracks;

    public SolverResult(String solverName) {
        this.solverName = solverName;
        this.solved = false;
    }

    // Getters and Setters
    public String getSolverName() { return solverName; }
    
    public boolean isSolved() { return solved; }
    public void setSolved(boolean solved) { this.solved = solved; }

    public int[][] getSolution() { return solution; }
    public void setSolution(int[][] solution) { this.solution = solution; }

    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }

    public long getIterations() { return iterations; }
    public void setIterations(long iterations) { this.iterations = iterations; }

    public long getBacktracks() { return backtracks; }
    public void setBacktracks(long backtracks) { this.backtracks = backtracks; }
}
