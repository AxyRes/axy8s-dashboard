(function () {
    const API_BASE = "/api";
    const TOKEN_KEY = "axy8s_token";
    const USER_KEY = "axy8s_user";

    function loadAuthFromStorage() {
        const token = localStorage.getItem(TOKEN_KEY);
        const userJson = localStorage.getItem(USER_KEY);

        let user = null;
        if (userJson) {
            try {
                user = JSON.parse(userJson);
            } catch {
                user = null;
            }
        }
        return { token, user };
    }

    function saveAuth(token, user) {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        }
        if (user) {
            localStorage.setItem(USER_KEY, JSON.stringify(user));
        }
    }

    function clearAuth() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    async function apiFetch(path, options = {}) {
        const { token } = loadAuthFromStorage();
        const headers = options.headers || {};
        headers["Content-Type"] = "application/json";
        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        const res = await fetch(API_BASE + path, {
            ...options,
            headers
        });

        if (!res.ok) {
            if (res.status === 404) {
                throw new Error("API cho resource này chưa được implement (404).");
            }
            const text = await res.text();
            throw new Error(text || res.statusText);
        }

        const contentType = res.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return res.json();
        }
        return res.text();
    }

    function requireLogin() {
        const { token } = loadAuthFromStorage();
        if (!token && window.location.pathname !== "/ui/login") {
            window.location.href = "/ui/login";
        }
    }

    function initLayoutBasics() {
        const { user } = loadAuthFromStorage();

        const sidebar = document.getElementById("sidebar");
        const toggle = document.getElementById("sidebar-toggle");
        if (toggle && sidebar) {
            toggle.addEventListener("click", () => {
                sidebar.classList.toggle("collapsed");
            });
        }

        const logoutBtn = document.getElementById("logout-btn");
        if (logoutBtn) {
            logoutBtn.addEventListener("click", () => {
                clearAuth();
                window.location.href = "/ui/login";
            });
        }

        // user chip + menu Users
        if (user) {
            const nameEl = document.getElementById("user-chip-name");
            const roleEl = document.getElementById("user-chip-role");
            if (nameEl) nameEl.textContent = user.username;
            if (roleEl) roleEl.textContent = user.role;
            const section = document.getElementById("user-management-section");
            if (section && (user.role === "ADMIN" || user.role === "SUPER_ADMIN")) {
                section.hidden = false;
            }
        }
    }

    function initDetailModal() {
        const modal = document.getElementById("detail-modal");
        if (!modal) return;
        const closeBtn = document.getElementById("detail-close");
        const backdrop = modal.querySelector(".modal-backdrop");

        function close() {
            modal.classList.add("hidden");
        }

        if (closeBtn) closeBtn.addEventListener("click", close);
        if (backdrop) backdrop.addEventListener("click", close);
    }

    function openDetailModal(title, obj) {
        const modal = document.getElementById("detail-modal");
        if (!modal) return;
        const titleEl = document.getElementById("detail-title");
        const pre = document.getElementById("detail-json");
        if (titleEl) titleEl.textContent = title;
        if (pre) pre.textContent = JSON.stringify(obj, null, 2);
        modal.classList.remove("hidden");
    }

    window.AxyCommon = {
        apiFetch,
        saveAuth,
        clearAuth,
        requireLogin,
        initLayoutBasics,
        loadAuthFromStorage,
        initDetailModal,
        openDetailModal
    };
})();
