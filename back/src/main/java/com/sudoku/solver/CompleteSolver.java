package com.sudoku.solver;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMiddle;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.variables.IntVar;

import com.sudoku.model.SolverResult;
import com.sudoku.model.SudokuGrid;

public class CompleteSolver extends SudokuSolver {

    // Enums pour configuration externe
    public enum SearchStrategy {
        INPUT_ORDER, DOM_OVER_WDEG, MIN_DOM_SIZE
    }

    public enum ValueHeuristic {
        MIN, MAX, MIDDLE, RANDOM_VAL
    }

    public enum RestartType {
        NONE, LUBY, GEOMETRIC
    }

    // Configuration par défaut
    private int timeoutSeconds = 60;
    private SearchStrategy strategy = SearchStrategy.DOM_OVER_WDEG;
    private ValueHeuristic valueHeuristic = ValueHeuristic.MIN;
    private String consistencyLevel = "DEFAULT"; // "AC", "BC", etc.
    private RestartType restartType = RestartType.NONE;
    private int restartBase = 100;
    private double restartFactor = 1.5;

    public CompleteSolver(SudokuGrid grid) {
        super(grid);
    }

    // Setters pour la configuration
    public void setTimeout(int seconds) {
        this.timeoutSeconds = seconds;
    }

    public void setStrategy(SearchStrategy s) {
        this.strategy = s;
    }

    public void setValueHeuristic(ValueHeuristic v) {
        this.valueHeuristic = v;
    }

    public void setConsistencyLevel(String c) {
        this.consistencyLevel = c;
    }

    public void setRestart(RestartType type, int base, double factor) {
        this.restartType = type;
        this.restartBase = base;
        this.restartFactor = factor;
    }

    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        int size = grid.getSize();
        int blockSize = grid.getBlockSize();

        // 1. Model
        Model model = new Model("Sudoku");
        IntVar[][] vars = model.intVarMatrix("c", size, size, 1, size);
        IntVar[] allVars = flatten(vars);

        // 2. Constraints
        String consistency = consistencyLevel;
        if (consistencyLevel.equals("DEFAULT")) {
            if (strategy == SearchStrategy.INPUT_ORDER) {
                consistency = "DEFAULT"; 
            } else {
                consistency = "AC"; // Pour DomOverWDeg, on garde la puissance
            }
        }
        // Rows & Cols
        for (int i = 0; i < size; i++) {
            model.allDifferent(vars[i], consistency).post();
            IntVar[] colVars = new IntVar[size];
            for (int j = 0; j < size; j++) {
                colVars[j] = vars[j][i];
            }
            model.allDifferent(colVars, consistency).post();
        }
        // Blocks
        for (int br = 0; br < blockSize; br++) {
            for (int bc = 0; bc < blockSize; bc++) {
                IntVar[] blockVars = new IntVar[size];
                int idx = 0;
                for (int i = 0; i < blockSize; i++) {
                    for (int j = 0; j < blockSize; j++) {
                        blockVars[idx++] = vars[br * blockSize + i][bc * blockSize + j];
                    }
                }
                model.allDifferent(blockVars, consistency).post();
            }
        }
        // Presets
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid.get(i, j) != 0) {
                    model.arithm(vars[i][j], "=", grid.get(i, j)).post();
                }
            }
        }

        // 3. Search Configuration
        Solver solver = model.getSolver();
        solver.limitTime(timeoutSeconds + "s");

        // Value Selector
        IntValueSelector valSel;
        switch (valueHeuristic) {
            case MAX:
                valSel = new IntDomainMax();
                break;
            case MIDDLE:
                valSel = new IntDomainMiddle(true);
                break;
            case RANDOM_VAL:
                valSel = new IntDomainRandom(0);
                break;
            default:
                valSel = new IntDomainMin();
                break;
        }

        // Variable Selector & Strategy
        IntStrategy intStrategy;
        switch (strategy) {
            case INPUT_ORDER:
                intStrategy = Search.intVarSearch(new InputOrder<>(model), valSel, allVars);
                break;
            case MIN_DOM_SIZE:
                intStrategy = Search.intVarSearch(new FirstFail(model), valSel, allVars);
                break;
            default: // DOM_OVER_WDEG
                intStrategy = (IntStrategy) Search.domOverWDegSearch(allVars); // Uses Min domain by default
                break;
        }
        solver.setSearch(intStrategy);

        // Restarts
        if (restartType == RestartType.LUBY) {
            solver.setLubyRestart(restartBase, new FailCounter(model, 1), (int) restartFactor);
        } else if (restartType == RestartType.GEOMETRIC) {
            solver.setGeometricalRestart(restartBase, restartFactor, new FailCounter(model, 1), 10000);
        }

        // 4. Solve
        boolean status = solver.solve();

        if (status) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    grid.set(i, j, vars[i][j].getValue());
                }
            }
        }

        this.iterations = solver.getMeasures().getNodeCount();
        this.backtracks = solver.getMeasures().getBackTrackCount();
        finalizeResult(status);

        return result;
    }

    private IntVar[] flatten(IntVar[][] mat) {
        return java.util.Arrays.stream(mat)
                .flatMap(java.util.Arrays::stream)
                .toArray(IntVar[]::new);
    }
}
