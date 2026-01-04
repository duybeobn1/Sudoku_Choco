// ==========================================
//  APP.JS - Navigation & Module Manager
// ==========================================

class AppManager {
    constructor() {
        this.currentModule = 'solver';
        this.setupNavigation();
    }

    setupNavigation() {
        const navButtons = document.querySelectorAll('.nav-btn');

        navButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const module = btn.dataset.module;
                this.switchModule(module);
            });
        });
    }

    switchModule(moduleName) {
        // Hide all modules
        document.querySelectorAll('.module').forEach(mod => {
            mod.classList.remove('active');
        });

        // Show selected module
        document.getElementById(moduleName).classList.add('active');

        // Update active nav button
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-module="${moduleName}"]`).classList.add('active');

        this.currentModule = moduleName;
    }
}

// Initialize when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
    window.appManager = new AppManager();
});