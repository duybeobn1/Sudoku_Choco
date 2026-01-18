document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  initGrid(3);
  setupButtons();
  loadBenchmarkHistory();
});

let currentN = 3; // Block size (3 -> 9x9)
let currentSize = 9;
let currentGrid = [];
let initialGridState = null;

/* =========================
   TABS LOGIC
========================= */
function initTabs() {
    const buttons = document.querySelectorAll('.nav-btn');
    const modules = document.querySelectorAll('.module');

    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            // Deactivate all
            buttons.forEach(b => b.classList.remove('active'));
            modules.forEach(m => m.classList.remove('active'));

            // Activate clicked
            btn.classList.add('active');
            const tabId = btn.getAttribute('data-tab');
            const tabContent = document.getElementById(tabId);
            if (tabContent) {
                tabContent.classList.add('active');
            }

            // Auto-load benchmark data when opening the benchmark tab
            if (tabId === 'benchmark') {
                loadBenchmarkHistory();
            }
        });
    });
}

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
      
      // Handle navigation keys if needed? (Skipped for brevity)
      
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

async function loadBenchmarkHistory() {
    try {
        const response = await fetch('http://localhost:8080/api/benchmark/history');
        if (!response.ok) return;

        const text = await response.text();
        const lines = text.trim().split('\n');

        if (lines.length > 1) {
            // 1. Reset everything
            resetBenchmarkAggregates();
            ensureBenchmarkCharts();
            
            const tableBody = document.getElementById('benchmarkTableBody');
            if (tableBody) tableBody.innerHTML = ''; // Clear existing rows

            // 2. Process each line
            // Skip header (index 0)
            lines.slice(1).forEach(line => {
                const cols = line.split(',');
                if (cols.length >= 11) {
                    const [instance, diff, strategy, val, cons, restart, solverType, timeMs, iter, back, rawStatus] = cols;
                    
                    // FIX: Trim whitespace/newlines from status to ensure correct comparison
                    const status = rawStatus ? rawStatus.trim() : '';

                    const rowObj = { instance, diff, strategy, val, cons, restart, solverType, timeMs, iter, back, status };

                    // Update Charts & KPIs
                    updateBenchmarkAggAndCharts(rowObj);

                    // NEW: Add Row to Table
                    if (tableBody) {
                        const tr = document.createElement('tr');
                        
                        // Format Config column based on solver type
                        let configLabel = strategy;
                        if (solverType === 'Complete') {
                            configLabel = `${strategy}/${val}/${cons}/${restart}`;
                        }
                        
                        tr.innerHTML = `
                            <td>${instance}</td>
                            <td><span class="${getBadgeClass(diff)}">${diff}</span></td>
                            <td>${configLabel}</td>
                            <td>${solverType}</td>
                            <td>${timeMs}</td>
                            <td>${iter}</td>
                            <td>${back}</td>
                            <td><span class="badge ${status === 'SOLVED' ? 'badge-sat' : 'badge-timeout'}">${status}</span></td>
                        `;
                        tableBody.appendChild(tr);
                    }
                }
            });
        }
    } catch (e) {
        console.log("No history loaded:", e);
    }
}


/* =========================
   STATUS UI
========================= */
function setStatus(msg, type) {
  const el = document.getElementById('statusMessage');
  el.textContent = msg;
  el.className = 'status-message ' + type;
}

/* =========================
   BUTTONS + API
========================= */
function setupButtons() {
    
  // 1. Grid Size Change
  document.getElementById('gridSize').addEventListener('change', (e) => {
      const n = parseInt(e.target.value);
      initGrid(n);
  });

  // 2. Load Example (API Call)
  document.getElementById('loadExample').addEventListener('change', async (e) => {
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

  // 3. Solve
  document.getElementById('btnSolve').addEventListener('click', async () => {
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

  document.getElementById('btnClear').addEventListener('click', clearGrid);

    document.getElementById('btnReset').addEventListener('click', () => {
        if (initialGridState) {
            // Recharger les données initiales
            loadGridData(initialGridState);
            setStatus('Grid reset to initial state.', '');
        } else {
            // Fallback si pas d'état (ex: grille vide manuelle)
            const inputs = document.querySelectorAll('.cell');
            inputs.forEach(input => {
                if (!input.classList.contains('user-input')) {
                    input.value = '';
                    input.classList.remove('solved');
                    const r = input.dataset.row;
                    const c = input.dataset.col;
                    currentGrid[r][c] = 0; // Important: sync le modèle !
                }
            });
            setStatus('Grid cleared (solved cells removed).', '');
        }
    });


  document.getElementById('btnStartBenchmark').addEventListener('click', startBenchmark);
}

// Helper to fetch example
async function fetchExampleGrid(id) {
    // Assumes endpoint GET /api/example/{id} returns JSON: [[...], ...]
    const response = await fetch(`http://localhost:8080/api/example/${id}`);
    if (!response.ok) throw new Error(response.statusText);
    return await response.json();
}

function getBadgeClass(difficulty) {
  const d = String(difficulty || '').toLowerCase();
  if (d === 'easy') return 'badge-easy';
  if (d === 'medium') return 'badge-medium';
  if (d === 'hard') return 'badge-hard';
  return 'badge-secondary';
}

/* =========================
   BENCHMARK LOGIC
   (Same as before, kept for completeness)
========================= */
let timeChart = null;
let successChart = null;
let methodTimeChart = null;
let methodSuccessChart = null;

let solverAgg = new Map(); 
let diffAgg = {
  Easy: { total: 0, solved: 0 },
  Medium: { total: 0, solved: 0 },
  Hard: { total: 0, solved: 0 },
};
let globalAgg = { total: 0, solved: 0, sumTime: 0 };
let methodAgg = {
  Complete: { total: 0, solved: 0, sumTime: 0 },
  Incomplete: { total: 0, solved: 0, sumTime: 0 },
  Greedy: { total: 0, solved: 0, sumTime: 0 },
};

function resetBenchmarkAggregates() {
  solverAgg = new Map();
  diffAgg = {
    Easy: { total: 0, solved: 0 },
    Medium: { total: 0, solved: 0 },
    Hard: { total: 0, solved: 0 },
  };
  globalAgg = { total: 0, solved: 0, sumTime: 0 };
  methodAgg = {
    Complete: { total: 0, solved: 0, sumTime: 0 },
    Incomplete: { total: 0, solved: 0, sumTime: 0 },
    Greedy: { total: 0, solved: 0, sumTime: 0 },
  };
}

function benchmarkLabelFromRow({ solverType, strategy, val, cons, restart }) {
  if (solverType === 'Complete') return `${strategy}/${val}/${cons}/${restart}`;
  return solverType;
}

function methodFromSolverType(solverType) {
  if (solverType === 'Complete') return 'Complete';
  if (String(solverType).toLowerCase().includes('greedy')) return 'Greedy';
  return 'Incomplete';
}

function ensureBenchmarkCharts() {
  const timeCanvas = document.getElementById('timeChart');
  const successCanvas = document.getElementById('successChart');
  const methodTimeCanvas = document.getElementById('methodTimeChart');
  const methodSuccessCanvas = document.getElementById('methodSuccessChart');

  if (timeCanvas && !timeChart) {
    timeChart = new Chart(timeCanvas.getContext('2d'), {
      type: 'bar',
      data: { labels: [], datasets: [{ label: 'Avg Time (ms)', data: [] }] },
      options: { responsive: true, animation: false, scales: { y: { beginAtZero: true, title: { display: true, text: 'ms' } } } },
    });
  }

  if (successCanvas && !successChart) {
    successChart = new Chart(successCanvas.getContext('2d'), {
      type: 'bar',
      data: {
        labels: ['Easy', 'Medium', 'Hard'],
        datasets: [{ label: 'Solved', data: [0, 0, 0] }, { label: 'Failed/Timeout', data: [0, 0, 0] }],
      },
      options: { responsive: true, animation: false, scales: { y: { beginAtZero: true, title: { display: true, text: 'Runs' } } } },
    });
  }
  
  if (methodTimeCanvas && !methodTimeChart) {
    methodTimeChart = new Chart(methodTimeCanvas.getContext('2d'), {
      type: 'bar',
      data: {
        labels: ['Complete', 'Incomplete', 'Greedy'],
        datasets: [{ label: 'Avg Time (ms)', data: [0, 0, 0], backgroundColor: ['#36a2eb', '#ffcd56', '#ff6384'] }],
      },
      options: { responsive: true, animation: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
    });
  }

  if (methodSuccessCanvas && !methodSuccessChart) {
    methodSuccessChart = new Chart(methodSuccessCanvas.getContext('2d'), {
      type: 'bar',
      data: {
        labels: ['Complete', 'Incomplete', 'Greedy'],
        datasets: [{ label: 'Success Rate (%)', data: [0, 0, 0], backgroundColor: ['#36a2eb', '#ffcd56', '#ff6384'] }],
      },
      options: { responsive: true, animation: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } },
    });
  }
  
  // Reset Data
  if(timeChart) { timeChart.data.labels = []; timeChart.data.datasets[0].data = []; timeChart.update('none'); }
  if(successChart) { successChart.data.datasets[0].data = [0,0,0]; successChart.data.datasets[1].data = [0,0,0]; successChart.update('none'); }
  if(methodTimeChart) { methodTimeChart.data.datasets[0].data = [0,0,0]; methodTimeChart.update('none'); }
  if(methodSuccessChart) { methodSuccessChart.data.datasets[0].data = [0,0,0]; methodSuccessChart.update('none'); }
}

function updateKPIs() {
  const totalRunsEl = document.getElementById('kpiTotalRuns');
  const successRateEl = document.getElementById('kpiSuccessRate');
  const avgTimeEl = document.getElementById('kpiAvgTime');

  if (totalRunsEl) totalRunsEl.textContent = String(globalAgg.total);
  if (successRateEl) {
    const rate = globalAgg.total > 0 ? Math.round((globalAgg.solved / globalAgg.total) * 100) : 0;
    successRateEl.textContent = `${rate}%`;
  }
  if (avgTimeEl) {
    const avg = globalAgg.total > 0 ? (globalAgg.sumTime / globalAgg.total) : 0;
    avgTimeEl.textContent = `${avg.toFixed(2)} ms`;
  }
}

function updateCharts() {
  if (timeChart) {
    const labels = Array.from(solverAgg.keys());
    timeChart.data.labels = labels;
    timeChart.data.datasets[0].data = labels.map(k => solverAgg.get(k).sumTime / solverAgg.get(k).count);
    timeChart.update('none');
  }
  if (successChart) {
    const diffs = ['Easy', 'Medium', 'Hard'];
    successChart.data.datasets[0].data = diffs.map(d => diffAgg[d]?.solved ?? 0);
    successChart.data.datasets[1].data = diffs.map(d => (diffAgg[d]?.total ?? 0) - (diffAgg[d]?.solved ?? 0));
    successChart.update('none');
  }
  if (methodTimeChart) {
    const methods = ['Complete', 'Incomplete', 'Greedy'];
    methodTimeChart.data.datasets[0].data = methods.map(m => {
        const d = methodAgg[m]; return d.total > 0 ? (d.sumTime/d.total) : 0;
    });
    methodTimeChart.update('none');
  }
  if (methodSuccessChart) {
    const methods = ['Complete', 'Incomplete', 'Greedy'];
    methodSuccessChart.data.datasets[0].data = methods.map(m => {
        const d = methodAgg[m]; return d.total > 0 ? ((d.solved/d.total)*100) : 0;
    });
    methodSuccessChart.update('none');
  }
}

function updateBenchmarkAggAndCharts(row) {
  const time = Number(row.timeMs);
  const solved = row.status === 'SOLVED';

  globalAgg.total += 1;
  if (Number.isFinite(time)) globalAgg.sumTime += time;
  if (solved) globalAgg.solved += 1;

  if (!diffAgg[row.diff]) diffAgg[row.diff] = { total: 0, solved: 0 };
  diffAgg[row.diff].total += 1;
  if (solved) diffAgg[row.diff].solved += 1;

  const label = benchmarkLabelFromRow(row);
  const prev = solverAgg.get(label) || { count: 0, sumTime: 0 };
  solverAgg.set(label, { count: prev.count + 1, sumTime: prev.sumTime + (Number.isFinite(time) ? time : 0) });

  const method = methodFromSolverType(row.solverType);
  methodAgg[method].total += 1;
  if (solved) methodAgg[method].solved += 1;
  if (Number.isFinite(time)) methodAgg[method].sumTime += time;

  updateKPIs();
  updateCharts();
}

function startBenchmark(e) {
    // FIX: Prevent default action (page reload/tab switch)
    if (e && e.preventDefault) e.preventDefault();
    
    const btn = document.getElementById('btnStartBenchmark');
    const tableBody = document.getElementById('benchmarkTableBody');
    
    if (btn) {
        btn.disabled = true;
        btn.textContent = "Running...";
    }
    
    if (tableBody) tableBody.innerHTML = '';
    
    resetBenchmarkAggregates();
    ensureBenchmarkCharts();

    const placeholder = document.createElement('tr');
    placeholder.innerHTML = `<td colspan="8" class="loading">Streaming benchmark data...</td>`;
    tableBody.appendChild(placeholder);

    console.log("Connecting to EventSource...");
    const eventSource = new EventSource('http://localhost:8080/api/benchmark');

    eventSource.onopen = function () {
        console.log("Connection opened!");
    };

    eventSource.onmessage = function (event) {
        const line = event.data;

        if (line === 'end') {
        eventSource.close();
        btn.disabled = false;
        btn.textContent = "Start Global Benchmark";
        setStatus("Benchmark completed!", "success");
        return;
        }

        if (line.startsWith('Instance')) return;

        const cols = line.split(',');
        if (cols.length < 11) return;

        const [instance, diff, strategy, val, cons, restart, solverType, timeMs, iter, back, status] = cols;

        if (tableBody.querySelector('.loading')) tableBody.innerHTML = '';

        const rowEl = document.createElement('tr');
        rowEl.style.animation = "fadeIn 0.5s";
        rowEl.innerHTML = `
        <td>${instance}</td>
        <td><span class="badge ${getBadgeClass(diff)}">${diff}</span></td>
        <td style="font-size: 11px;">${solverType === 'Complete' ? `<b>${strategy}</b><br>${restart}` : '-'}</td>
        <td>${solverType}</td>
        <td>${Number(timeMs).toFixed(2)}</td>
        <td>${iter}</td>
        <td>${back}</td>
        <td><span class="badge ${status === 'SOLVED' ? 'badge-sat' : 'badge-timeout'}">${status}</span></td>
        `;
        tableBody.appendChild(rowEl);

        updateBenchmarkAggAndCharts({
            instance, diff, strategy, val, cons, restart, solverType, timeMs, iter, back, status
        });
  };

  eventSource.onerror = function (e) {
    console.error("EventSource failed:", e);
    eventSource.close();
    btn.disabled = false;
    btn.textContent = "Start Global Benchmark";
    setStatus("Benchmark stream stopped (error or closed).", "error");
  };
}
