package com.sudoku.heuristics;

import com.sudoku.model.SudokuGrid;

/**
 * Interface for cell selection strategies.
 * Determines which empty cell should be filled next.
 */
public interface CellHeuristic {
    /**
     * Selects the next cell to fill.
     * @param grid The current grid state.
     * @return An int array {row, col} or null if no empty cells remain.
     */
    int[] selectCell(SudokuGrid grid);
}
