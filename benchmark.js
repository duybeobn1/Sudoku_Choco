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

        this.data = lines.slice(1).map(line => {
            const values = line.split(',').map(v => v.trim());
            const obj = {};
            headers.forEach((h, i) => obj[h] = values[i]);
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
        const fastest = this.data.reduce((a, b) =>
            parseFloat(a.MeanTimeMs) < parseFloat(b.MeanTimeMs) ? a : b
        );
        const slowest = this.data.reduce((a, b) =>
            parseFloat(a.MeanTimeMs) > parseFloat(b.MeanTimeMs) ? a : b
        );

        document.getElementById('kpi-instances').textContent = instances;
        document.getElementById('kpi-runs').textContent = this.data.length;
        document.getElementById('kpi-success').textContent = successRate + '%';
        document.getElementById('kpi-fastest').textContent = fastest.Solver.replace('_', ' ').slice(0, 15);
        document.getElementById('kpi-slowest').textContent = slowest.Solver.replace('_', ' ').slice(0, 15);
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

            row.innerHTML = `
                <td>${r.Instance}</td>
                <td><span class="${diffClass}">${r.Difficulty}</span></td>
                <td>${r.Solver.replace(/_/g, ' ')}</td>
                <td><span class="badge ${typeClass}">${r.SolverType}</span></td>
                <td>${parseFloat(r.MeanTimeMs).toFixed(2)}</td>
                <td>${Math.round(r.MeanIterations)}</td>
                <td>${Math.round(r.MeanBacktracks)}</td>
                <td><span class="badge ${statusClass}">${r.Status}</span></td>
            `;
            tbody.appendChild(row);
        });
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