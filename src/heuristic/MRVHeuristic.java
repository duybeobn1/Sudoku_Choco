package src.heuristic;

import src.model.SudokuGrid;

/**
 * Minimum Remaining Values (MRV) heuristic.
 * Selects the empty cell with the fewest possible values.
 * This reduces branching factor and improves search efficiency.
 */
public class MRVHeuristic implements CellHeuristic {
    @Override
    public int[] selectCell(SudokuGrid grid) {
        int N = grid.getSize();
        int minCandidates = N + 1;
        int[] bestCell = null;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid.isEmpty(i, j)) {
                    int candidateCount = grid.countCandidates(i, j);
                    if (candidateCount < minCandidates) {
                        minCandidates = candidateCount;
                        bestCell = new int[]{i, j};
                    }
                }
            }
        }

        return bestCell;
    }
}
