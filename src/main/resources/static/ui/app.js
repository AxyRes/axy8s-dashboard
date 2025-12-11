const API_BASE = "/api";

let authToken = null;
let currentUser = null;
let currentNamespace = null;
let currentSection = "workloads";
let currentResource = "pods";
let currentItems = []; // giữ raw data cho popup detail

const RESOURCE_CONFIG = {
    workloads: {
        pods: {
            label: "Pods",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/pods`
        },
        deployments: {
            label: "Deployments",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/deployments`
        },
        statefulsets: {
            label: "StatefulSets",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/statefulsets`
        },
        daemonsets: {
            label: "DaemonSets",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/daemonsets`
        },
        jobs: {
            label: "Jobs",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/jobs`
        },
        cronjobs: {
            label: "CronJobs",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/cronjobs`
        }
    },
    networking: {
        services: {
            label: "Services",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/services`
        },
        ingresses: {
            label: "Ingresses",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/ingresses`
        },
        serviceaccounts: {
            label: "ServiceAccounts",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/serviceaccounts`
        },
        networkpolicies: {
            label: "NetworkPolicies",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/networkpolicies`
        }
    },
    storage: {
        persistentvolumeclaims: {
            label: "PersistentVolumeClaims",
            scope: "namespace",
            path: (ns) => `/k8s/namespaces/${ns}/persistentvolumeclaims`
        }
    },
    cluster: {
        persistentvolumes: {
            label: "PersistentVolumes",
            scope: "cluster",
            path: () => `/k8s/cluster/persistent-volumes`
        },
        storageclasses: {
            label: "StorageClasses",
            scope: "cluster",
            path: () => `/k8s/cluster/storageclasses`
        },
        namespaces: {
            label: "Namespaces",
            scope: "cluster",
            path: () => `/k8s/namespaces`
        },
        nodes: {
            label: "Nodes",
            scope: "cluster",
            path: () => `/k8s/cluster/nodes`
        }
    },
    access: {
        users: {
            label: "Users",
            scope: "cluster",
            path: () => `/users` // anh map API user theo path này
        }
    }
};

function $(id) {
    return document.getElementById(id);
}

async function apiFetch(path, options = {}) {
    const headers = options.headers || {};
    headers["Content-Type"] = "application/json";
    if (authToken) {
        headers["Authorization"] = `Bearer ${authToken}`;
    }

    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers
    });

    if (!res.ok) {
        // 404 -> coi như API chưa implement, không bắt login lại
        if (res.status === 404) {
            throw new Error("API cho resource này chưa được implement (404).");
        }
        if (res.status === 401 || res.status === 403) {
            throw new Error("Phiên đăng nhập hết hạn hoặc không đủ quyền.");
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

/* LOGIN */

function initLogin() {
    const form = $("login-form");
    const errorBox = $("login-error");

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        errorBox.hidden = true;

        const username = $("login-username").value.trim();
        const password = $("login-password").value;

        if (!username || !password) {
            errorBox.textContent = "Vui lòng nhập username/password.";
            errorBox.hidden = false;
            return;
        }

        try {
            const data = await apiFetch("/auth/login", {
                method: "POST",
                body: JSON.stringify({ username, password })
            });

            authToken = data.token;
            currentUser = {
                username: data.username,
                role: data.role
            };

            $("login-screen").classList.add("hidden");
            $("dashboard-shell").classList.remove("hidden");

            $("user-chip-name").textContent = currentUser.username;
            $("user-chip-role").textContent = currentUser.role;

            // chỉ SUPER_ADMIN / ADMIN mới thấy menu Users
            if (currentUser.role === "SUPER_ADMIN" || currentUser.role === "ADMIN") {
                $("user-management-section").hidden = false;
            }

            await loadNamespaces();
            activateDefaultNav();
        } catch (err) {
            errorBox.textContent = "Đăng nhập thất bại: " + err.message;
            errorBox.hidden = false;
        }
    });
}

/* LOGOUT */

function initLogout() {
    const btn = $("logout-btn");
    btn.addEventListener("click", () => {
        authToken = null;
        currentUser = null;
        currentNamespace = null;

        $("dashboard-shell").classList.add("hidden");
        $("login-screen").classList.remove("hidden");
        $("login-form").reset();
        $("login-error").hidden = true;
    });
}

/* SIDEBAR */

function initSidebarToggle() {
    const toggle = $("sidebar-toggle");
    const sidebar = $("sidebar");

    toggle.addEventListener("click", () => {
        sidebar.classList.toggle("collapsed");
    });
}

/* NAMESPACE */

async function loadNamespaces() {
    const select = $("namespace-select");
    select.innerHTML = `<option disabled>Loading...</option>`;

    try {
        const namespaces = await apiFetch("/k8s/namespaces");

        select.innerHTML = "";
        namespaces.forEach(ns => {
            const name = ns.metadata && ns.metadata.name ? ns.metadata.name : "(unknown)";
            const opt = document.createElement("option");
            opt.value = name;
            opt.textContent = name;
            select.appendChild(opt);
        });

        if (namespaces.length > 0) {
            currentNamespace = namespaces[0].metadata.name;
            select.value = currentNamespace;
        }

        $("chip-namespace").textContent = currentNamespace || "-";
        select.addEventListener("change", () => {
            currentNamespace = select.value;
            $("chip-namespace").textContent = currentNamespace;
            reloadCurrentResource();
        });

        await reloadCurrentResource();
    } catch (err) {
        console.error(err);
    }
}

/* NAV */

function initNav() {
    const items = document.querySelectorAll(".nav-item");

    items.forEach(btn => {
        btn.addEventListener("click", () => {
            items.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            currentSection = btn.dataset.section;
            currentResource = btn.dataset.resource;

            reloadCurrentResource();
        });
    });
}

function activateDefaultNav() {
    const firstActive = document.querySelector(".nav-item.active") || document.querySelector(".nav-item");
    if (firstActive) {
        firstActive.click();
    }
}

/* FILTER */

function initSearchFilter() {
    const input = $("search-input");
    input.addEventListener("input", () => {
        reloadCurrentResource();
    });
}

/* MODAL DETAIL */

function initDetailModal() {
    const modal = $("detail-modal");
    const closeBtn = $("detail-close");
    const backdrop = modal.querySelector(".modal-backdrop");

    function close() {
        modal.classList.add("hidden");
    }

    closeBtn.addEventListener("click", close);
    backdrop.addEventListener("click", close);
}

function openDetailModal(title, item) {
    const modal = $("detail-modal");
    $("detail-title").textContent = title;
    $("detail-json").textContent = JSON.stringify(item, null, 2);
    modal.classList.remove("hidden");
}

/* USER MODAL */

function initUserModal() {
    const modal = $("user-modal");
    const closeBtn = $("user-modal-close");
    const backdrop = modal.querySelector(".modal-backdrop");
    const form = $("user-form");
    const errorBox = $("user-modal-error");

    function close() {
        modal.classList.add("hidden");
        errorBox.hidden = true;
        form.reset();
        $("user-id").value = "";
    }

    closeBtn.addEventListener("click", close);
    backdrop.addEventListener("click", close);

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        errorBox.hidden = true;

        const id = $("user-id").value;
        const username = $("user-username").value.trim();
        const password = $("user-password").value;
        const role = $("user-role").value;

        if (!username) {
            errorBox.textContent = "Username không được trống.";
            errorBox.hidden = false;
            return;
        }

        try {
            const payload = { username, role };
            if (password) {
                payload.password = password;
            }

            if (id) {
                await apiFetch(`/users/${id}`, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
            } else {
                await apiFetch("/users", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
            }

            close();
            await reloadUsers();
        } catch (err) {
            errorBox.textContent = "Lỗi khi lưu user: " + err.message;
            errorBox.hidden = false;
        }
    });
}

function openUserModal(user) {
    const modal = $("user-modal");
    const title = $("user-modal-title");

    if (user) {
        title.textContent = `Update user: ${user.username}`;
        $("user-id").value = user.id;
        $("user-username").value = user.username;
        $("user-role").value = (user.role || "USER");
    } else {
        title.textContent = "Create new user";
        $("user-id").value = "";
    }

    $("user-modal-error").hidden = true;
    modal.classList.remove("hidden");
}

/* LOAD RESOURCE TABLE */

async function reloadCurrentResource() {
    if (currentSection === "access" && currentResource === "users") {
        await reloadUsers();
        return;
    }

    const configSection = RESOURCE_CONFIG[currentSection] || {};
    const cfg = configSection[currentResource];

    if (!cfg) {
        console.warn("Config not found for", currentSection, currentResource);
        return;
    }

    $("resource-title").textContent = cfg.label;
    $("resource-subtitle").textContent =
        cfg.scope === "cluster"
            ? `Cluster-scoped resource: ${cfg.label}`
            : `${cfg.label} trong namespace đã chọn.`;

    $("chip-scope").textContent = cfg.scope;

    $("create-user-btn").hidden = true; // chỉ bật ở trang Users

    const loading = $("loading-indicator");
    const error = $("error-indicator");
    const tbody = $("resource-table-body");
    const summary = $("table-summary");

    const thead = $("resource-table-head");
    thead.innerHTML = `
        <tr>
            <th>Name</th>
            <th>Namespace</th>
            <th>Status</th>
            <th>Age</th>
        </tr>
    `;

    loading.hidden = false;
    error.hidden = true;
    tbody.innerHTML = "";
    summary.textContent = "Loading...";

    try {
        const path = cfg.scope === "cluster" ? cfg.path() : cfg.path(currentNamespace);
        const data = await apiFetch(path);

        currentItems = data || [];

        const keyword = $("search-input").value.toLowerCase();
        let visibleCount = 0;

        currentItems.forEach((item, index) => {
            const meta = item.metadata || {};
            const status = item.status || {};
            const row = {
                name: meta.name || "",
                namespace: meta.namespace || "",
                status: status.phase || status.status || status.type || "",
                age: meta.creationTimestamp || ""
            };

            if (keyword && !row.name.toLowerCase().includes(keyword)) {
                return;
            }

            const tr = document.createElement("tr");
            tr.dataset.index = String(index);
            tr.innerHTML = `
                <td>${row.name}</td>
                <td>${row.namespace}</td>
                <td>${row.status}</td>
                <td>${row.age}</td>
            `;
            tbody.appendChild(tr);
            visibleCount++;
        });

        summary.textContent = `${visibleCount} items`;

        // click -> detail modal
        tbody.querySelectorAll("tr").forEach(tr => {
            tr.addEventListener("click", () => {
                const idx = Number(tr.dataset.index || "0");
                const item = currentItems[idx];
                const cfgSection = RESOURCE_CONFIG[currentSection][currentResource];
                const title = `${cfgSection.label} · ${(item.metadata && item.metadata.name) || ""}`;
                openDetailModal(title, item);
            });
        });

    } catch (err) {
        console.error(err);
        error.textContent = err.message;
        error.hidden = false;
    } finally {
        loading.hidden = true;
    }
}

/* USERS PAGE */

async function reloadUsers() {
    $("resource-title").textContent = "Users";
    $("resource-subtitle").textContent = "Quản lý user. Chỉ SUPER_ADMIN / ADMIN mới vào được trang này.";
    $("chip-scope").textContent = "cluster";

    const thead = $("resource-table-head");
    thead.innerHTML = `
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Role</th>
            <th>Active</th>
        </tr>
    `;

    $("create-user-btn").hidden = false;

    const loading = $("loading-indicator");
    const error = $("error-indicator");
    const tbody = $("resource-table-body");
    const summary = $("table-summary");

    loading.hidden = false;
    error.hidden = true;
    tbody.innerHTML = "";
    summary.textContent = "Loading...";

    try {
        const data = await apiFetch("/users");
        currentItems = data || [];

        const keyword = $("search-input").value.toLowerCase();
        let visibleCount = 0;

        currentItems.forEach((user, index) => {
            const row = {
                id: user.id,
                username: user.username || "",
                role: user.role || (user.roles && user.roles.join(",")) || "",
                active: user.active === false ? "false" : "true"
            };

            if (keyword && !row.username.toLowerCase().includes(keyword)) {
                return;
            }

            const tr = document.createElement("tr");
            tr.dataset.index = String(index);
            tr.innerHTML = `
                <td>${row.id ?? ""}</td>
                <td>${row.username}</td>
                <td>${row.role}</td>
                <td>${row.active}</td>
            `;
            tbody.appendChild(tr);
            visibleCount++;
        });

        summary.textContent = `${visibleCount} users`;

        tbody.querySelectorAll("tr").forEach(tr => {
            tr.addEventListener("click", () => {
                const idx = Number(tr.dataset.index || "0");
                const user = currentItems[idx];
                openUserModal(user);
            });
        });

        $("create-user-btn").onclick = () => openUserModal(null);

    } catch (err) {
        error.textContent = err.message;
        error.hidden = false;
    } finally {
        loading.hidden = true;
    }
}

/* BOOTSTRAP */

document.addEventListener("DOMContentLoaded", () => {
    initLogin();
    initLogout();
    initSidebarToggle();
    initNav();
    initSearchFilter();
    initDetailModal();
    initUserModal();
});
