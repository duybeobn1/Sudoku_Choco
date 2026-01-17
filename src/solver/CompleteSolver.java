package src.solver;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;
import src.model.SudokuGrid;
import src.model.SolverResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Complete Solver using Choco Constraint Programming library.
 * Guarantees finding a solution if one exists.
 * Uses advanced search strategies for efficiency.
 */
public class CompleteSolver extends SudokuSolver {

    private int timeoutSeconds = 60; // Default timeout
    private SearchStrategy strategy = SearchStrategy.DOM_OVER_WDEG;

    // Additional tunable parameters for benchmarking / experimentation
    private boolean useRestarts = false;
    private int lubyBase = 0;
    private int lubyUnit = 1;
    private int lubyFactor = 2;
    private boolean randomizeOrder = false;
    private long randomSeed = 0L;

    public enum SearchStrategy {
        INPUT_ORDER,
        DOM_OVER_WDEG,
        MIN_DOM_SIZE,
        RANDOM
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
     * Enable/disable Luby restart policy and set its parameters.
     */
    public void configureRestarts(boolean useRestarts, int lubyBase, int lubyUnit, int lubyFactor) {
        this.useRestarts = useRestarts;
        this.lubyBase = lubyBase;
        this.lubyUnit = lubyUnit;
        this.lubyFactor = lubyFactor;
    }

    /**
     * Configure randomization of the variable ordering.
     * If randomizeOrder is true and seed != 0, the seed is used for reproducibility.
     */
    public void configureRandomization(boolean randomizeOrder, long seed) {
        this.randomizeOrder = randomizeOrder;
        this.randomSeed = seed;
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

            // Optionally randomize variable order
            IntVar[] orderedVars = allVars;
            if (randomizeOrder) {
                List<IntVar> vars = new ArrayList<>(Arrays.asList(allVars));
                Random rnd = (randomSeed == 0L) ? new Random(System.nanoTime()) : new Random(randomSeed);
                Collections.shuffle(vars, rnd);
                orderedVars = vars.toArray(new IntVar[0]);
            }

            // Configure search strategy
            switch (strategy) {
                case DOM_OVER_WDEG:
                    solver.setSearch(Search.domOverWDegSearch(orderedVars));
                    break;
                case MIN_DOM_SIZE:
                    solver.setSearch(Search.minDomLBSearch(orderedVars));
                    break;
                case RANDOM:
                    // RANDOM here means: rely on randomized variable order + simple LB search
                    solver.setSearch(Search.inputOrderLBSearch(orderedVars));
                    break;
                case INPUT_ORDER:
                default:
                    solver.setSearch(Search.inputOrderLBSearch(orderedVars));
                    break;
            }

            // Optionally configure restart policy
            if (useRestarts && lubyBase > 0) {
                solver.setLubyRestart(lubyBase, new FailCounter(model, lubyUnit), lubyFactor);
            }

            solver.limitTime(timeoutSeconds + "s");

            // 6. Solve
            boolean solutionFound = solver.solve();
            long nodes = solver.getMeasures().getNodeCount();
            long backtrackCount = solver.getMeasures().getBackTrackCount();

            if (solutionFound && solver.isFeasible() == ESat.TRUE) {
                // Copy solution back to grid
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        grid.set(i, j, chocoGrid[i][j].getValue());
                    }
                }
                this.iterations = nodes;
                this.backtracks = backtrackCount;
                finalizeResult(true);
            } else {
                this.iterations = nodes;
                this.backtracks = backtrackCount;
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
