package com.sudoku.solver;

import com.sudoku.heuristics.CellHeuristic;
import com.sudoku.heuristics.MRVHeuristic;
import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;

public class GreedyIncompleteSolver extends SudokuSolver {

    private CellHeuristic heuristic = new MRVHeuristic();
    private long maxBacktracks = 10_000; 

    public GreedyIncompleteSolver(SudokuGrid grid) {
        super(grid);
    }

    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        boolean solved = search();
        if (solved && !isValid()) solved = false;
        finalizeResult(solved);
        return result;
    }

    private boolean search() {
        iterations++;
        // Timeout protection
        if ((System.currentTimeMillis() - startTime) > 2000) return false;

        if (grid.isFull()) return true;

        int[] cell = heuristic.selectCell(grid);
        if (cell == null) return false;

        int r = cell[0];
        int c = cell[1];
        int size = grid.getSize();

        for (int v = 1; v <= size; v++) {
            // Fast validation check
            if (isValidPlacement(r, c, v)) {
                grid.set(r, c, v);
                if (search()) return true;
                
                grid.set(r, c, 0);
                backtracks++;
                if (backtracks > maxBacktracks) return false;
            }
        }
        return false;
    }
    
    private boolean isValidPlacement(int r, int c, int v) {
        int size = grid.getSize();
        int blk = grid.getBlockSize();
        for(int k=0; k<size; k++) {
            if(k!=c && grid.get(r, k) == v) return false;
            if(k!=r && grid.get(k, c) == v) return false;
        }
        int br = (r/blk)*blk;
        int bc = (c/blk)*blk;
        for(int i=0; i<blk; i++) {
            for(int j=0; j<blk; j++) {
                int rr = br+i, cc = bc+j;
                if((rr!=r || cc!=c) && grid.get(rr, cc) == v) return false;
            }
        }
        return true;
    }
}
