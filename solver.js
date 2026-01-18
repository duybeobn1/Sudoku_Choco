// ==========================================
//  SOLVER.JS - Sudoku Solver Engine
// ==========================================

class SudokuSolverApp {
    constructor() {
        this.currentSize = 9;
        this.blockSize = 3;
        this.currentPuzzleData = null;
        this.PUZZLE_URLS = [
            "./benchmarks/sudoku_p0.dzn",
            "./benchmarks/sudoku_p36.dzn",
            "./benchmarks/sudoku_p89.dzn",
        ];
        // Mapping from grid size (e.g. 9,16,25) to recommended solver method
        // ('complete' or 'incomplete'), computed from benchmark_results.csv
        this.bestSolverBySize = {};
        this.init();
    }

    init() {
        this.populatePuzzleSelect();
        this.generateGrid(9);
        this.loadBenchmarkAdvice();
    }

    populatePuzzleSelect() {
        const select = document.getElementById('puzzleSelect');
        select.innerHTML = '<option value="">-- Select a Puzzle --</option>';
        this.PUZZLE_URLS.forEach(url => {
            const option = document.createElement('option');
            option.value = url;
            const label = url.split('/').pop();
            option.textContent = label;
            select.appendChild(option);
        });
    }

    manualResize() {
        const sizeSelect = document.getElementById('gridSize');
        const size = parseInt(sizeSelect.value);
        this.generateGrid(size);
    }

    clearAllGrid() {
        document.getElementById('puzzleSelect').value = "";
        this.currentPuzzleData = null;
        document.getElementById('btnReset').disabled = true;
        this.manualResize();
        this.updateStatus("Grid cleared.", "normal");
    }

    resetCurrentPuzzle() {
        if (!this.currentPuzzleData) {
            this.updateStatus("No puzzle loaded to reset.", "error");
            return;
        }
        this.fillGrid(this.currentPuzzleData);
        this.updateStatus("Puzzle reset to initial state.", "info");
    }

    generateGrid(size) {
        this.currentSize = size;
        this.blockSize = Math.sqrt(this.currentSize);

        const sizeSelect = document.getElementById('gridSize');
        let optionExists = false;
        for (let i = 0; i < sizeSelect.options.length; i++) {
            if (parseInt(sizeSelect.options[i].value) === size) optionExists = true;
        }
        if (!optionExists) {
            const opt = document.createElement('option');
            opt.value = size;
            opt.text = `${size} x ${size} (Custom)`;
            sizeSelect.add(opt);
        }
        sizeSelect.value = size;

        const board = document.getElementById('sudokuBoard');
        board.innerHTML = '';
        board.style.gridTemplateColumns = `repeat(${this.currentSize}, 1fr)`;

        let cellWidth = '40px';
        let fontSize = '16px';
        if (this.currentSize > 16) { cellWidth = '30px'; fontSize = '12px'; }
        if (this.currentSize > 25) { cellWidth = '25px'; fontSize = '10px'; }

        for (let i = 0; i < this.currentSize * this.currentSize; i++) {
            const input = document.createElement('input');
            input.type = 'text';
            input.className = 'cell';
            input.style.width = cellWidth;
            input.style.height = cellWidth;
            input.style.fontSize = fontSize;
            input.maxLength = this.currentSize > 9 ? 2 : 1;
            input.dataset.index = i;

            const row = Math.floor(i / this.currentSize);
            const col = i % this.currentSize;

            if (Number.isInteger(this.blockSize)) {
                if ((col + 1) % this.blockSize === 0 && (col + 1) !== this.currentSize) {
                    input.style.borderRight = "2px solid #333";
                }
                if ((row + 1) % this.blockSize === 0 && (row + 1) !== this.currentSize) {
                    input.style.borderBottom = "2px solid #333";
                }
            }

            input.addEventListener('input', () => {
                input.classList.add('user-input');
                input.classList.remove('solved');
            });

            board.appendChild(input);
        }
    }

    async loadSelectedPuzzle() {
        const select = document.getElementById('puzzleSelect');
        const url = select.value;
        if (!url) return;

        this.showLoading("Fetching file...");

        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const text = await response.text();
            this.parseAndLoad(text);
        } catch (err) {
            console.error(err);
            this.updateStatus(`Error loading file: ${err.message}`, "error");
        }
    }

    parseAndLoad(content) {
        content = content.replace(/%.*$/gm, "");
        const arrayMatch = content.match(/\[([\s\S]*?)\]/);
        let dataToParse = arrayMatch ? arrayMatch[1] : content;
        dataToParse = dataToParse.replace(/_/g, "0");
        const numbers = dataToParse.match(/\d+/g);

        if (!numbers) {
            this.updateStatus("Error: No digits found.", "error");
            return;
        }

        const totalCells = numbers.length;
        const detectedSize = Math.sqrt(totalCells);

        if (Number.isInteger(detectedSize)) {
            this.currentPuzzleData = numbers;
            document.getElementById('btnReset').disabled = false;

            if (detectedSize !== this.currentSize) {
                this.generateGrid(detectedSize);
                setTimeout(() => this.fillGrid(numbers), 50);
            } else {
                this.fillGrid(numbers);
            }
        } else {
            this.updateStatus(`Error: ${totalCells} digits. Not a square grid.`, "error");
        }
    }

    fillGrid(numbers) {
        const inputs = document.querySelectorAll('.cell');
        inputs.forEach(inp => {
            inp.value = '';
            inp.classList.remove('user-input', 'solved');
        });
        numbers.forEach((num, index) => {
            if (index < inputs.length) {
                const val = parseInt(num);
                if (val !== 0) {
                    inputs[index].value = val;
                    inputs[index].classList.add('user-input');
                }
            }
        });
        this.updateStatus(`Loaded puzzle (${this.currentSize}x${this.currentSize}).`, "success");
    }

    async runSolver() {
        let method = document.getElementById('solverMethod').value;

        // Auto-select method based on offline benchmark results if available
        if (method === 'auto') {
            const recommended = this.bestSolverBySize[this.currentSize];
            if (recommended === 'complete' || recommended === 'incomplete') {
                method = recommended;
            } else {
                // Fallback heuristic if no benchmark info for this size
                method = this.currentSize <= 9 ? 'complete' : 'incomplete';
            }
        }

        const inputs = Array.from(document.querySelectorAll('.cell'));

        const board = [];
        for (let input of inputs) {
            const val = parseInt(input.value);
            board.push(isNaN(val) ? 0 : val);
        }

        if (!this.isValidBoard(board)) {
            this.updateStatus("Invalid Grid! Duplicates detected.", "error");
            return;
        }

        const btn = document.getElementById('btnSolve');
        btn.disabled = true;

        this.showLoading("Solving");
        await new Promise(r => setTimeout(r, 50));

        const startTime = performance.now();
        let solved = false;
        let limit = this.currentSize >= 16 ? 2000000 : 200000;

        if (method === 'complete') {
            solved = await this.solveBacktrackingMRV(board, Infinity);
        } else {
            solved = await this.solveBacktrackingMRV(board, limit);
        }

        const endTime = performance.now();
        const timeTaken = (endTime - startTime).toFixed(2);

        btn.disabled = false;

        if (solved) {
            inputs.forEach((input, index) => {
                input.value = board[index];
                if (!input.classList.contains('user-input')) {
                    input.classList.add('solved');
                }
            });
            this.updateStatus(`Solved in ${timeTaken} ms!`, "success");
        } else {
            this.updateStatus(method === 'incomplete' ? "No solution found (limit reached)." : "No solution exists.", "error");
        }
    }

    async solveBacktrackingMRV(board, maxIterations) {
        const ctx = { iterations: 0, maxIterations: maxIterations };
        return await this.solveRecursive(board, ctx);
    }

    async solveRecursive(board, ctx) {
        ctx.iterations++;

        if (ctx.iterations % 2000 === 0) {
            if (ctx.iterations > ctx.maxIterations) return false;
            await new Promise(resolve => setTimeout(resolve, 0));
        }

        let bestIndex = -1;
        let minOptionsCount = this.currentSize + 1;
        let bestOptions = [];
        let emptyFound = false;

        for (let i = 0; i < this.currentSize * this.currentSize; i++) {
            if (board[i] === 0) {
                emptyFound = true;
                const opts = this.getValidOptions(board, i);

                if (opts.length < minOptionsCount) {
                    minOptionsCount = opts.length;
                    bestIndex = i;
                    bestOptions = opts;
                    if (minOptionsCount === 0) return false;
                    if (minOptionsCount === 1) break;
                }
            }
        }

        if (!emptyFound) return true;

        for (let val of bestOptions) {
            board[bestIndex] = val;
            const result = await this.solveRecursive(board, ctx);
            if (result) return true;
            board[bestIndex] = 0;
        }
        return false;
    }

    getValidOptions(board, index) {
        const options = [];
        const row = Math.floor(index / this.currentSize);
        const col = index % this.currentSize;
        const used = new Array(this.currentSize + 1).fill(false);

        for (let i = 0; i < this.currentSize; i++) {
            const rVal = board[row * this.currentSize + i];
            if (rVal !== 0) used[rVal] = true;
            const cVal = board[i * this.currentSize + col];
            if (cVal !== 0) used[cVal] = true;
        }

        if (Number.isInteger(this.blockSize)) {
            const startRow = Math.floor(row / this.blockSize) * this.blockSize;
            const startCol = Math.floor(col / this.blockSize) * this.blockSize;
            for (let r = 0; r < this.blockSize; r++) {
                for (let c = 0; c < this.blockSize; c++) {
                    const bVal = board[(startRow + r) * this.currentSize + (startCol + c)];
                    if (bVal !== 0) used[bVal] = true;
                }
            }
        }

        for (let num = 1; num <= this.currentSize; num++) {
            if (!used[num]) options.push(num);
        }
        return options;
    }

    isValidBoard(board) {
        for (let i = 0; i < this.currentSize * this.currentSize; i++) {
            if (board[i] !== 0) {
                const val = board[i];
                board[i] = 0;
                const opts = this.getValidOptions(board, i);
                board[i] = val;
                if (!opts.includes(val)) return false;
            }
        }
        return true;
    }

    // ==========================
    // Benchmark-based advice
    // ==========================

    async loadBenchmarkAdvice() {
        try {
            const resp = await fetch('./benchmarks/benchmark_results.csv');
            if (!resp.ok) return;
            const text = await resp.text();
            this.computeBestSolverFromCSV(text);
        } catch (e) {
            console.warn('Could not load benchmark_results.csv', e);
        }
    }

    computeBestSolverFromCSV(text) {
        const lines = text.trim().split('\n');
        if (lines.length <= 1) return;
        const headers = lines[0].split(',').map(h => h.trim());

        const idx = name => headers.indexOf(name);
        const iSize = idx('Size');
        const iType = idx('SolverType');
        const iTime = idx('MeanTimeMs');
        const iStatus = idx('Status');
        if (iSize < 0 || iType < 0 || iTime < 0 || iStatus < 0) return;

        // best[gridSize][solverType] = min time
        const best = {};

        lines.slice(1)
            .filter(line => line.trim().length > 0)
            .forEach(line => {
                const raw = line.split(',').map(v => v.trim());
                let values;
                if (raw.length > headers.length) {
                    // Reconstruct Solver field that may contain commas
                    const head = raw.slice(0, 4);
                    const tail = raw.slice(-6);
                    const solverParts = raw.slice(4, raw.length - 6);
                    const solver = solverParts.join(',');
                    values = [...head, solver, ...tail];
                } else {
                    values = raw;
                }

                const status = values[iStatus];
                if (status !== 'SAT') return; // only consider successful runs

                const size = parseInt(values[iSize], 10);
                const type = values[iType];   // "Complete" or "Incomplete"
                const time = parseFloat(values[iTime]);
                if (Number.isNaN(size) || Number.isNaN(time)) return;

                if (!best[size]) best[size] = {};
                if (!best[size][type] || time < best[size][type]) {
                    best[size][type] = time;
                }
            });

        // Decide global best type per size
        this.bestSolverBySize = {};
        Object.keys(best).forEach(sizeStr => {
            const size = parseInt(sizeStr, 10);
            const entry = best[size];
            let chosenType = null;
            let bestTime = Infinity;
            ['Complete', 'Incomplete'].forEach(t => {
                if (entry[t] !== undefined && entry[t] < bestTime) {
                    bestTime = entry[t];
                    chosenType = t;
                }
            });
            if (chosenType) {
                // Map CSV type to internal method name
                this.bestSolverBySize[size] = (chosenType === 'Complete') ? 'complete' : 'incomplete';
            }
        });

        console.log('Best solver by size (from benchmark):', this.bestSolverBySize);
    }

    showLoading(text) {
        const el = document.getElementById('status');
        el.className = 'status-message processing';
        el.innerHTML = `
            ${text}
            <div class="loading-dots">
                <div></div><div></div><div></div>
            </div>
        `;
    }

    updateStatus(msg, type) {
        const el = document.getElementById('status');
        el.textContent = msg;
        el.className = 'status-message ' + type;
    }
}

// Initialize when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
    window.solverApp = new SudokuSolverApp();
});