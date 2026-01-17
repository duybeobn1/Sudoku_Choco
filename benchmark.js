// ==========================================
//  BENCHMARK.JS - Benchmark Dashboard
// ==========================================

class BenchmarkDashboard {
    constructor() {
        this.data = [];
        this.charts = {};
        this.init();
    }

    init() {
        this.loadBenchmarkData();
    }

    loadBenchmarkData() {
        fetch('./benchmarks/benchmark_results.csv')
            .then(r => r.text())
            .then(text => this.parseCSV(text))
            .catch(err => this.showError(err.message));
    }

    parseCSV(text) {
        const lines = text.trim().split('\n');
        const headers = lines[0].split(',').map(h => h.trim());

        // Robust CSV parsing: the Solver field itself may contain commas.
        // We know the header layout is:
        // Instance,Difficulty,SolverType,Size,Solver,MeanTimeMs,StdDevMs,MeanIterations,MeanBacktracks,SuccessRate,Status
        // So if a data line has more than headers.length columns, we join
        // the "middle" part back into the Solver string.
        this.data = lines.slice(1)
            .filter(line => line.trim().length > 0)
            .map(line => {
                const raw = line.split(',').map(v => v.trim());

                let values;
                if (raw.length > headers.length) {
                    const head = raw.slice(0, 4); // first 4 fixed columns
                    const tail = raw.slice(-6);   // last 6 metric/status columns
                    const solverParts = raw.slice(4, raw.length - 6);
                    const solver = solverParts.join(',');
                    values = [...head, solver, ...tail];
                } else {
                    values = raw;
                }

                const obj = {};
                headers.forEach((h, i) => {
                    obj[h] = values[i];
                });
                return obj;
            });

        if (this.data.length === 0) {
            this.showError('No data found in CSV');
            return;
        }

        this.render();
    }

    render() {
        document.getElementById('loading').style.display = 'none';
        document.getElementById('content').style.display = 'block';

        this.updateKPIs();
        this.createCharts();
        this.populateTable();
    }

    updateKPIs() {
        const instances = new Set(this.data.map(r => r.Instance)).size;
        const successCount = this.data.filter(r => r.Status === 'SAT').length;
        const successRate = Math.round((successCount / this.data.length) * 100);

        // Focus on best/worst COMPLETE configurations (for choosing a "best" CP config)
        const completeSat = this.data.filter(r => r.SolverType === 'Complete' && r.Status === 'SAT');
        let fastestComplete = null;
        let slowestComplete = null;
        if (completeSat.length > 0) {
            fastestComplete = completeSat.reduce((a, b) =>
                parseFloat(a.MeanTimeMs) < parseFloat(b.MeanTimeMs) ? a : b
            );
            slowestComplete = completeSat.reduce((a, b) =>
                parseFloat(a.MeanTimeMs) > parseFloat(b.MeanTimeMs) ? a : b
            );
        }

        const truncate = (s, maxLen = 28) => {
            if (!s) return '—';
            return s.length > maxLen ? s.slice(0, maxLen - 1) + '…' : s;
        };

        const fastestLabel = fastestComplete ? fastestComplete.Solver : '—';
        const slowestLabel = slowestComplete ? slowestComplete.Solver : '—';

        document.getElementById('kpi-instances').textContent = instances;
        document.getElementById('kpi-runs').textContent = this.data.length;
        document.getElementById('kpi-success').textContent = successRate + '%';

        const fastestEl = document.getElementById('kpi-fastest');
        fastestEl.textContent = truncate(fastestLabel);
        fastestEl.title = fastestLabel;

        const slowestEl = document.getElementById('kpi-slowest');
        slowestEl.textContent = truncate(slowestLabel);
        slowestEl.title = slowestLabel;
    }

    createCharts() {
        // Group by solver
        const solvers = {};
        this.data.forEach(r => {
            if (!solvers[r.Solver]) solvers[r.Solver] = [];
            solvers[r.Solver].push(parseFloat(r.MeanTimeMs));
        });

        // Chart 1: Time by Solver
        const solverLabels = Object.keys(solvers).map(s => s.replace(/_/g, '\n').slice(0, 20));
        const solverTimes = Object.values(solvers).map(times =>
            (times.reduce((a, b) => a + b) / times.length).toFixed(2)
        );

        // Destroy existing chart if it exists
        if (this.charts.time) this.charts.time.destroy();

        this.charts.time = new Chart(document.getElementById('timeChart'), {
            type: 'bar',
            data: {
                labels: solverLabels,
                datasets: [{
                    label: 'Avg Time (ms)',
                    data: solverTimes,
                    backgroundColor: '#208080'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });

        // Chart 2: Success by Difficulty
        const difficulties = {};
        this.data.forEach(r => {
            if (!difficulties[r.Difficulty]) difficulties[r.Difficulty] = { total: 0, success: 0 };
            difficulties[r.Difficulty].total++;
            if (r.Status === 'SAT') difficulties[r.Difficulty].success++;
        });

        const diffLabels = Object.keys(difficulties);
        const diffSuccess = diffLabels.map(d =>
            Math.round((difficulties[d].success / difficulties[d].total) * 100)
        );

        if (this.charts.success) this.charts.success.destroy();

        this.charts.success = new Chart(document.getElementById('successChart'), {
            type: 'doughnut',
            data: {
                labels: diffLabels,
                datasets: [{
                    data: diffSuccess,
                    backgroundColor: ['#22c55e', '#f59e0b', '#ef4444', '#8b5cf6']
                }]
            },
            options: { responsive: true }
        });

        // Chart 3: Complete vs Incomplete
        const types = {};
        this.data.forEach(r => {
            if (!types[r.SolverType]) types[r.SolverType] = [];
            types[r.SolverType].push(parseFloat(r.MeanTimeMs));
        });

        const typeLabels = Object.keys(types);
        const typeTimes = typeLabels.map(t =>
            (types[t].reduce((a, b) => a + b) / types[t].length).toFixed(2)
        );

        if (this.charts.type) this.charts.type.destroy();

        this.charts.type = new Chart(document.getElementById('typeChart'), {
            type: 'bar',
            data: {
                labels: typeLabels,
                datasets: [{
                    label: 'Avg Time (ms)',
                    data: typeTimes,
                    backgroundColor: ['#3b82f6', '#ef4444']
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });

        // Chart 4: Iterations by Solver
        const solverIter = {};
        this.data.forEach(r => {
            if (!solverIter[r.Solver]) solverIter[r.Solver] = [];
            solverIter[r.Solver].push(parseFloat(r.MeanIterations));
        });

        const iterLabels = Object.keys(solverIter).map(s => s.replace(/_/g, '\n').slice(0, 20));
        const iterData = Object.values(solverIter).map(iters =>
            (iters.reduce((a, b) => a + b) / iters.length).toFixed(0)
        );

        if (this.charts.iter) this.charts.iter.destroy();

        this.charts.iter = new Chart(document.getElementById('iterChart'), {
            type: 'bar',
            data: {
                labels: iterLabels,
                datasets: [{
                    label: 'Avg Iterations',
                    data: iterData,
                    backgroundColor: '#5e5240'
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    populateTable() {
        const tbody = document.getElementById('tbody');
        tbody.innerHTML = '';

        this.data.forEach(r => {
            const row = document.createElement('tr');
            const diffClass = `${r.Difficulty.toLowerCase()}`;
            const typeClass = r.SolverType === 'Complete' ? 'badge-complete' : 'badge-incomplete';
            const statusClass = r.Status === 'SAT' ? 'badge-sat' : 'badge-timeout';

            const details = this.parseSolverDetails(r);

            row.innerHTML = `
                <td>${r.Instance}</td>
                <td><span class="${diffClass}">${r.Difficulty}</span></td>
                <td>${details.strategy}</td>
                <td>${details.val}</td>
                <td>${details.cons}</td>
                <td>${details.restart}</td>
                <td>${details.order}</td>
                <td><span class="badge ${typeClass}">${r.SolverType}</span></td>
                <td>${parseFloat(r.MeanTimeMs).toFixed(2)}</td>
                <td>${Math.round(r.MeanIterations)}</td>
                <td>${Math.round(r.MeanBacktracks)}</td>
                <td><span class="badge ${statusClass}">${r.Status}</span></td>
            `;
            tbody.appendChild(row);
        });
    }

    // Parse solver configuration string like
    // "Complete[DOM OVER WDEG,Val=MAX,Cons=AC,Luby(...),DeterministicOrder]"
    // into separate fields for the table.
    parseSolverDetails(r) {
        const def = { strategy: r.Solver || '—', val: '—', cons: '—', restart: '—', order: '—' };
        if (r.SolverType !== 'Complete' || !r.Solver) return def;

        let s = r.Solver;
        const lb = s.indexOf('[');
        const rb = s.lastIndexOf(']');
        if (lb === -1 || rb === -1 || rb <= lb + 1) return def;

        let inner = s.slice(lb + 1, rb).trim();
        // Normalize separators so we can split reliably
        inner = inner.replace(/\s*\|\s*/g, ',');
        const parts = inner.split(',').map(p => p.trim()).filter(Boolean);
        if (parts.length === 0) return def;

        const out = { strategy: parts[0], val: '—', cons: '—', restart: '—', order: '—' };
        parts.slice(1).forEach(p => {
            if (p.startsWith('Val=')) {
                out.val = p.substring(4);
            } else if (p.startsWith('Cons=')) {
                out.cons = p.substring(5);
            } else if (p.startsWith('Luby(') || p.startsWith('Geom(')) {
                out.restart = p;
            } else if (p.length > 0) {
                out.order = p;
            }
        });
        return out;
    }

    showError(msg) {
        document.getElementById('loading').style.display = 'none';
        document.getElementById('error').style.display = 'block';
        document.getElementById('error').textContent = '❌ Error: ' + msg;
    }
}

// Initialize when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
    window.benchmarkDash = new BenchmarkDashboard();
});