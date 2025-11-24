***

## Modeling the Sudoku Problem

### Variables

- Define a set of variables representing the cells of the Sudoku grid.
- Typically, a 9x9 matrix of integer variables $$X_{i,j}$$ where $$i,j \in [1..9]$$.
- Domain of each variable: integers 1 to 9 (possible digit values).

### Constraints

1. **Row constraints:**  
   For each row $$i$$, all variables $$\{X_{i,j} | j=1..9\}$$ must take distinct values (allDifferent).

2. **Column constraints:**  
   For each column $$j$$, all variables $$\{X_{i,j} | i=1..9\}$$ must take distinct values (allDifferent).

3. **Block constraints:**  
   For each 3x3 block, the 9 variables inside must take distinct values (allDifferent). Blocks are defined as:

$$
X_{r,c} \quad|\quad 
r = 3m + u, \; c = 3n + v, \;
m,n \in \{0,1,2\}, \;
u,v \in \{1,2,3\}
$$

4. **Known cells:**  
   Some cells are fixed by the puzzle clues. Set those variables to their known value (equality constraint).

### Objective

- The problem is CSP: find a complete assignment to all variables satisfying all constraints.
- No optimization objective (just feasibility and full assignment).

***

## Key Choco modeling elements for Sudoku

- Use `IntVar[][] grid = model.intVarMatrix("X", 9, 9, 1, 9);`
- For rows/columns/blocks: use `model.allDifferent()` constraint.
- For clues: use `grid[i][j].eq(clueValue).post();`

## Variables

- Une matrice 9x9 de variables entières $$X_{i,j}$$, $$i,j \in [1..9]$$
- Domaine de chaque variable : valeurs entières de 1 à 9

## Contraintes

- Chaque ligne contient des valeurs toutes différentes : $$\text{allDifferent}(X_{i,*})$$
- Chaque colonne contient des valeurs toutes différentes : $$\text{allDifferent}(X_{*,j})$$
- Chaque zone 3x3 contient des valeurs toutes différentes : $$\text{allDifferent}(\text{bloc}_{3\times3})$$
- Les cellules déjà données dans la grille sont fixées à leur valeur

## Objectif

- Trouver une affectation complète des variables satisfaisant toutes les contraintes


## Link for benchmark
- https://www.hakank.org/minizinc/sudoku_problems2/index.html