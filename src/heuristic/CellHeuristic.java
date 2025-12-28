package src.heuristic;

import src.model.SudokuGrid;

/**
 * Interface for cell selection heuristics in incomplete solver.
 * Implementations determine which cell to fill next during backtracking.
 */
public interface CellHeuristic {
    /**
     * Select the next cell to fill in the Sudoku grid.
     *
     * @param grid the current SudokuGrid state
     * @return an array [row, col] of the selected cell, or null if no cell to select
     */
    int[] selectCell(SudokuGrid grid);
}
