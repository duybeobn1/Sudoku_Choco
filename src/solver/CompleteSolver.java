package src.solver;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import src.model.SudokuGrid;
import src.model.SolverResult;

/**
 * Complete Solver using Choco Constraint Programming library.
 * Guarantees finding a solution if one exists.
 * Uses advanced search strategies for efficiency.
 */
public class CompleteSolver extends SudokuSolver {
    private int timeoutSeconds = 60;  // Default timeout
    private SearchStrategy strategy = SearchStrategy.DOM_OVER_WDEG;

    public enum SearchStrategy {
        INPUT_ORDER,
        DOM_OVER_WDEG,
        MIN_DOM_SIZE,
        ACTIVITY_BASED
    }

    /**
     * Constructor for CompleteSolver.
     *
     * @param grid the SudokuGrid to solve
     */
    public CompleteSolver(SudokuGrid grid) {
        super(grid);
    }

    /**
     * Set the timeout for the solver.
     *
     * @param seconds timeout in seconds
     */
    public void setTimeout(int seconds) {
        this.timeoutSeconds = seconds;
    }

    /**
     * Set the search strategy.
     *
     * @param strategy the SearchStrategy to use
     */
    public void setStrategy(SearchStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Solve the Sudoku puzzle using Choco Solver.
     */
    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();

        try {
            int N = grid.getSize();
            int n = grid.getBlockSize();

            // 1. Create model
            Model model = new Model("Sudoku-" + N + "x" + N);
            IntVar[][] chocoGrid = model.intVarMatrix("cell", N, N, 1, N);

            // 2. Add row and column constraints
            for (int i = 0; i < N; i++) {
                model.allDifferent(chocoGrid[i]).post();
                IntVar[] column = new IntVar[N];
                for (int j = 0; j < N; j++) {
                    column[j] = chocoGrid[j][i];
                }
                model.allDifferent(column).post();
            }

            // 3. Add block constraints
            for (int blockRow = 0; blockRow < n; blockRow++) {
                for (int blockCol = 0; blockCol < n; blockCol++) {
                    IntVar[] block = new IntVar[N];
                    int idx = 0;
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            block[idx++] = chocoGrid[blockRow * n + i][blockCol * n + j];
                        }
                    }
                    model.allDifferent(block).post();
                }
            }

            // 4. Add clue constraints
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid.get(i, j) != 0) {
                        model.arithm(chocoGrid[i][j], "=", grid.get(i, j)).post();
                    }
                }
            }

            // 5. Configure solver
            Solver solver = model.getSolver();
            IntVar[] allVars = flattenMatrix(chocoGrid);

            switch (strategy) {
                case DOM_OVER_WDEG:
                    solver.setSearch(Search.domOverWDegSearch(allVars));
                    break;
                case MIN_DOM_SIZE:
                    solver.setSearch(Search.minDomLBSearch(allVars));
                    break;
                case ACTIVITY_BASED:
                    solver.setSearch(Search.activityBasedSearch(allVars));
                    break;
                case INPUT_ORDER:
                default:
                    solver.setSearch(Search.inputOrderLBSearch(allVars));
                    break;
            }

            solver.limitTime(timeoutSeconds + "s");

            // 6. Solve
            boolean solutionFound = solver.solve();
            long nodes = solver.getMeasures().getNodeCount();
            long backtracks = solver.getMeasures().getBackTrackCount();

            if (solutionFound && solver.isFeasible() == ESat.TRUE) {
                // Copy solution back to grid
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        grid.set(i, j, chocoGrid[i][j].getValue());
                    }
                }
                result.setIterations(nodes);
                result.setBacktracks(backtracks);
                finalizeResult(true);
            } else {
                result.setIterations(nodes);
                result.setBacktracks(backtracks);
                finalizeResult(false);
            }

        } catch (Exception e) {
            System.err.println("Error in CompleteSolver: " + e.getMessage());
            e.printStackTrace();
            finalizeResult(false);
        }

        return result;
    }

    /**
     * Flatten a 2D IntVar matrix to a 1D array.
     */
    private IntVar[] flattenMatrix(IntVar[][] matrix) {
        return java.util.Arrays.stream(matrix)
                .flatMap(java.util.Arrays::stream)
                .toArray(IntVar[]::new);
    }
}
