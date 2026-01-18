const API_BASE_URL = "http://localhost:8080/api/solve";

/**
 * Calls the Sudoku Solver API.
 * @param {string} solverType - 'complete', 'incomplete', or 'greedy'
 * @param {number[][]} grid - 9x9 grid
 * @returns {Promise<any>} - JSON response
 */
async function solveSudokuApi(solverType, grid) {
  const response = await fetch(`${API_BASE_URL}/${solverType}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(grid),
  });

  if (!response.ok) {
    throw new Error(`API Error: ${response.status}`);
  }

  return await response.json();
}
