from z3 import *
import random
import time
import os

# --- Helper Function to Save to .dzn ---
def save_to_dzn(instance, filename):
    """
    Saves the grid to a .dzn file in MiniZinc format.
    Matches the style: x = array2d(1..n, 1..n, [ ... ]);
    """
    n = len(instance)
    
    with open(filename, 'w') as f:
        # Header comments
        f.write(f"%\n% Generated Sudoku {n}x{n}\n")
        f.write(f"% Density approximation: {sum(1 for r in instance for c in r if c != 0)/(n*n):.2f}\n%\n")
        
        # Start of array definition
        f.write(f"x = array2d(1..n, 1..n, [\n")
        
        # Iterate through rows
        for r in range(n):
            row_str_list = []
            for c in range(n):
                val = instance[r][c]
                # Format: Numbers are right-aligned (width 3), 0 becomes '_'
                if val == 0:
                    str_val = "  _"
                else:
                    str_val = f"{val:3d}"
                row_str_list.append(str_val)
            
            # Join row with commas
            line = ", ".join(row_str_list)
            
            # Add comma at end of line if it's not the very last value of the file
            if r < n - 1:
                line += ","
            
            f.write(f" {line}\n")
            
        # Closing bracket and parameter n
        f.write(f"]);\n\n")
        f.write(f"n = {n};\n")
    
    print(f"   > File saved: {filename}")


class LargeSudokuGenerator:
    def __init__(self, block_size):
        self.b = block_size
        self.n = block_size * block_size
        
        print(f"[{time.strftime('%H:%M:%S')}] Initializing Generator for {self.n}x{self.n} grid...")
        
        self.solver = Solver()
        
        # 1. Initialize Variables
        print(f"   > Creating {self.n*self.n} variables...")
        self.cells = [[Int(f'c_{r}_{c}') for c in range(self.n)] for r in range(self.n)]
        
        # 2. Add Constraints
        print(f"   > Adding constraints (Rows, Cols, Blocks)...")
        self._add_sudoku_constraints()
        print(f"   > Initialization complete.\n")

    def _add_sudoku_constraints(self):
        # Domain (1 to N)
        for r in range(self.n):
            for c in range(self.n):
                self.solver.add(self.cells[r][c] >= 1)
                self.solver.add(self.cells[r][c] <= self.n)

        # Rows
        for r in range(self.n):
            self.solver.add(Distinct(self.cells[r]))

        # Cols
        for c in range(self.n):
            col = [self.cells[r][c] for r in range(self.n)]
            self.solver.add(Distinct(col))

        # Blocks
        for i in range(self.b):
            for j in range(self.b):
                block = []
                for r in range(self.b):
                    for c in range(self.b):
                        block.append(self.cells[i * self.b + r][j * self.b + c])
                self.solver.add(Distinct(block))

    def generate_full_grid(self):
        print(f"[{time.strftime('%H:%M:%S')}] requesting Z3 solve...")
        
        # --- FIX: Use Z3 internal randomization instead of random constraints ---
        seed = random.randint(0, 1000000)
        self.solver.set("random_seed", seed)
        self.solver.set("smt.random_seed", seed)  # redundancy for different Z3 versions
        
        print(f"   > Search started (Seed: {seed})... this might take a few seconds.")
        t0 = time.time()
        
        result = self.solver.check()
        dt = time.time() - t0
        
        if result == sat:
            print(f"   > Solution found in {dt:.2f} seconds!")
            model = self.solver.model()
            # Extract values
            grid = [[model.evaluate(self.cells[r][c]).as_long() for c in range(self.n)] for r in range(self.n)]
            return grid
        else:
            print(f"   > UNSAT found (Unexpected). Retrying...")
            return self.generate_full_grid()

    def create_instance(self, density=0.4):
        print(f"[{time.strftime('%H:%M:%S')}] Creating Instance (Density: {density})")
        
        # 1. Get the solution
        full_grid = self.generate_full_grid()
        
        # 2. Punch holes
        print(f"   > Punching holes to create puzzle...")
        instance = [row[:] for row in full_grid]
        total_cells = self.n * self.n
        to_remove = int(total_cells * (1 - density))
        
        indices = [(r, c) for r in range(self.n) for c in range(self.n)]
        random.shuffle(indices)
        
        for r, c in indices[:to_remove]:
            instance[r][c] = 0
            
        print(f"   > Instance created successfully.\n")
        return instance

def print_simple(grid):
    for row in grid:
        # Print numbers formatted nicely, 0 becomes '__'
        print(" ".join(f"{x:02}" if x != 0 else "__" for x in row))

# --- MAIN ---
if __name__ == "__main__":
    
    # Create output directory
    output_dir = "generated_instances"
    os.makedirs(output_dir, exist_ok=True)
    print(f"Files will be saved to: {os.path.abspath(output_dir)}\n")

    # The densities requested: 0.2, 0.3, 0.4, 0.6, 0.8
    # 2 instances per density
    densities = [0.2, 0.3, 0.4, 0.6, 0.8]
    
    # Define tasks: (Label, Block Size)
    tasks = [
        ("16x16", 4),
        ("25x25", 5)
    ]

    for label, b_size in tasks:
        print("="*60)
        print(f">>> STARTING BATCH: {label} (Block Size {b_size})")
        print("="*60)
        
        # Initialize generator ONCE per size to save time
        gen = LargeSudokuGenerator(block_size=b_size)
        
        # Iterate through densities
        for d in densities:
            # Generate 2 instances per density
            for i in range(1, 3):
                print(f"\n--- Generating {label} Instance (Density {d}) #{i} ---")
                
                instance = gen.create_instance(density=d)
                
                # Naming convention: sudoku_16x16_d02_1.dzn
                # d02 represents density 0.2
                filename = f"{output_dir}/sudoku_{label}_d{int(d*10):02d}_{i}.dzn"
                save_to_dzn(instance, filename)

    print("\n" + "="*60)
    print("ALL DONE! Check the folder:", output_dir)