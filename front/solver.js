document.addEventListener('DOMContentLoaded', () => {
  initGrid(3);
  setupSolverButtons();
});

let currentN = 3; // Block size (3 -> 9x9)
let currentSize = 9;
let currentGrid = [];
let initialGridState = null;

/* =========================
   SOLVER GRID LOGIC
========================= */
function initGrid(n = 3) {
  currentN = n;
  currentSize = n * n;
  
  // Re-init data model
  currentGrid = Array(currentSize).fill().map(() => Array(currentSize).fill(0));

  const gridContainer = document.getElementById('sudokuGrid');
  gridContainer.innerHTML = '';
  
  // Dynamic Grid Template
  gridContainer.style.gridTemplateColumns = `repeat(${currentSize}, 1fr)`;
  
  // Optional: adjust container width for large grids
  if (currentSize > 16) {
      gridContainer.style.maxWidth = '95vw';
  } else {
      gridContainer.style.maxWidth = 'fit-content';
  }

  for (let i = 0; i < currentSize; i++) {
    for (let j = 0; j < currentSize; j++) {
      const input = document.createElement('input');
      input.type = 'text';
      input.className = 'cell';
      input.maxLength = (currentSize > 9) ? 2 : 1; // Allow 2 digits for 16x16+
      input.dataset.row = i;
      input.dataset.col = j;

      // Dynamic sizing for large grids
      if (currentSize > 9) {
          input.style.width = '35px';
          input.style.height = '35px';
          input.style.fontSize = '14px';
      }

      // Dynamic Borders for Blocks
      // Right border if end of block (but not end of row)
      if ((j + 1) % currentN === 0 && j < currentSize - 1) {
          input.style.borderRight = '2px solid #333';
      }
      // Bottom border if end of block (but not end of col)
      if ((i + 1) % currentN === 0 && i < currentSize - 1) {
          input.style.borderBottom = '2px solid #333';
      }

      input.addEventListener('input', (e) => handleCellInput(e, i, j));
      
      gridContainer.appendChild(input);
    }
  }
  initialGridState = JSON.parse(JSON.stringify(currentGrid));
}

function handleCellInput(e, r, c) {
    const valStr = e.target.value;
    const val = parseInt(valStr, 10);
    
    // Validation: 1 to currentSize
    if (isNaN(val) || val < 1 || val > currentSize) {
        e.target.value = '';
        currentGrid[r][c] = 0;
        e.target.classList.remove('user-input');
    } else {
        currentGrid[r][c] = val;
        e.target.classList.add('user-input');
    }
}

function updateGridUI(solution) {
  const inputs = document.querySelectorAll('.cell');
  inputs.forEach(input => {
    const r = Number(input.dataset.row);
    const c = Number(input.dataset.col);
    const val = solution[r][c];
    input.value = val;

    if (!input.classList.contains('user-input')) {
      input.classList.add('solved');
    }
  });
}

function loadGridData(data) {
    // Determine size from data
    const size = data.length;
    const n = Math.sqrt(size);
    
    if (!Number.isInteger(n)) {
        alert("Invalid grid size loaded: " + size);
        return;
    }
    
    // Update Select if possible
    const sizeSelect = document.getElementById('gridSize');
    if (sizeSelect) sizeSelect.value = n;

    // Re-init grid
    initGrid(n);
    
    // Fill data
    currentGrid = JSON.parse(JSON.stringify(data)); 
    initialGridState = JSON.parse(JSON.stringify(data));
    const inputs = document.querySelectorAll('.cell');
    
    inputs.forEach(input => {
        const r = parseInt(input.dataset.row);
        const c = parseInt(input.dataset.col);
        const val = currentGrid[r][c];
        
        if (val !== 0) {
            input.value = val;
            input.classList.add('user-input');
        } else {
            input.value = '';
            input.classList.remove('user-input');
        }
        input.classList.remove('solved');
    });
    
    setStatus(`Loaded example grid (${size}x${size}).`, '');
}

function clearGrid() {
  initGrid(currentN); // Simply re-init
  setStatus('', '');
}

/* =========================
   HELPER & BUTTONS
========================= */
function setStatus(msg, type) {
  const el = document.getElementById('statusMessage');
  if (el) {
      el.textContent = msg;
      el.className = 'status-message ' + type;
  }
}

async function fetchExampleGrid(id) {
    // Assumes endpoint GET /api/example/{id} returns JSON: [[...], ...]
    const response = await fetch(`http://localhost:8080/api/example/${id}`);
    if (!response.ok) throw new Error(response.statusText);
    return await response.json();
}

function setupSolverButtons() {
  // 1. Grid Size Change
  const gridSizeEl = document.getElementById('gridSize');
  if (gridSizeEl) {
      gridSizeEl.addEventListener('change', (e) => {
          const n = parseInt(e.target.value);
          initGrid(n);
      });
  }

  // 2. Load Example
  const loadExampleEl = document.getElementById('loadExample');
  if (loadExampleEl) {
      loadExampleEl.addEventListener('change', async (e) => {
          const id = e.target.value;
          if (!id) return; // Custom selected
          
          setStatus("Loading example...", "processing");
          try {
              const gridData = await fetchExampleGrid(id);
              loadGridData(gridData);
          } catch (err) {
              setStatus("Error loading example: " + err.message, "error");
              e.target.value = ""; // Reset select
          }
      });
  }

  // 3. Solve
  const btnSolve = document.getElementById('btnSolve');
  if (btnSolve) {
      btnSolve.addEventListener('click', async () => {
        const type = document.getElementById('solverType').value;
        setStatus('Solving...', 'processing');

        try {
          const result = await solveSudokuApi(type, currentGrid);
          if (result.solved) {
            updateGridUI(result.solution);
            setStatus(`Solved in ${result.timeMs} ms!`, 'success');
          } else {
            setStatus('No solution found or timeout.', 'error');
          }
        } catch (e) {
          setStatus('Error connecting to server.', 'error');
        }
      });
  }

  const btnClear = document.getElementById('btnClear');
  if (btnClear) btnClear.addEventListener('click', clearGrid);

  const btnReset = document.getElementById('btnReset');
  if (btnReset) {
      btnReset.addEventListener('click', () => {
        if (initialGridState) {
            loadGridData(initialGridState);
            setStatus('Grid reset to initial state.', '');
        } else {
            const inputs = document.querySelectorAll('.cell');
            inputs.forEach(input => {
                if (!input.classList.contains('user-input')) {
                    input.value = '';
                    input.classList.remove('solved');
                    const r = input.dataset.row;
                    const c = input.dataset.col;
                    currentGrid[r][c] = 0; 
                }
            });
            setStatus('Grid cleared (solved cells removed).', '');
        }
    });
  }
}
