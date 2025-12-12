(() => {
    const body = document.body;
    const toggle = document.getElementById('themeToggle');
    const savedTheme = localStorage.getItem('k8s-dashboard-theme');

    if (savedTheme) {
        body.setAttribute('data-theme', savedTheme);
        if (toggle) {
            toggle.checked = savedTheme === 'dark';
        }
    }

    const applyTheme = (theme) => {
        body.setAttribute('data-theme', theme);
        localStorage.setItem('k8s-dashboard-theme', theme);
    };

    if (toggle) {
        toggle.addEventListener('change', () => {
            applyTheme(toggle.checked ? 'dark' : 'light');
        });
    }
})();
