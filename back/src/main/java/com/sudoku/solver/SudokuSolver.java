package com.sudoku.solver;

import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;
import com.sudoku.util.SudokuValidator;

public abstract class SudokuSolver {

    protected final SudokuGrid grid;
    protected final SolverResult result;
    protected long startTime;
    protected long iterations;
    protected long backtracks;

    public SudokuSolver(SudokuGrid grid) {
        this.grid = new SudokuGrid(grid); 
        this.result = new SolverResult(this.getClass().getSimpleName());
    }

    public abstract SolverResult solve();

    protected void finalizeResult(boolean solved) {
        result.setSolved(solved);
        result.setTimeMs(System.currentTimeMillis() - startTime);
        result.setIterations(iterations);
        result.setBacktracks(backtracks);
        if (solved) {
            result.setSolution(grid.getGrid());
        }
    }
    
    protected boolean isValid() {
        return SudokuValidator.isValid(grid);
    }
}
