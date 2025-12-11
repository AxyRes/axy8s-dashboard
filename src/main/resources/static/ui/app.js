const API_BASE = "/api";

let authToken = null;
let currentUser = null;
let currentNamespace = null;
let currentSection = "workloads";
let currentResource = "pods";

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
        // các path cluster này anh kiểm tra lại với K8sClusterController của anh
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
        const text = await res.text();
        throw new Error(text || res.statusText);
    }

    const contentType = res.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return res.json();
    }
    return res.text();
}

// Login
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

            // cập nhật UI
            $("login-screen").classList.add("hidden");
            $("dashboard-shell").classList.remove("hidden");

            $("user-chip-name").textContent = currentUser.username;
            $("user-chip-role").textContent = currentUser.role;

            await loadNamespaces();
            activateDefaultNav();
        } catch (err) {
            errorBox.textContent = "Đăng nhập thất bại: " + err.message;
            errorBox.hidden = false;
        }
    });
}

// Sidebar toggle
function initSidebarToggle() {
    const toggle = $("sidebar-toggle");
    const sidebar = $("sidebar");

    toggle.addEventListener("click", () => {
        sidebar.classList.toggle("collapsed");
    });
}

// Namespace select
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

        // load lần đầu
        await reloadCurrentResource();
    } catch (err) {
        console.error(err);
    }
}

// Nav click
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

function applyFilter(rows, keyword) {
    if (!keyword) return rows;
    const lower = keyword.toLowerCase();
    return rows.filter(row => row.name.toLowerCase().includes(lower));
}

// Load resource table
async function reloadCurrentResource() {
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

    const loading = $("loading-indicator");
    const error = $("error-indicator");
    const tbody = $("resource-table-body");
    const summary = $("table-summary");

    loading.hidden = false;
    error.hidden = true;
    tbody.innerHTML = "";
    summary.textContent = "Loading...";

    try {
        const path = cfg.scope === "cluster"
            ? cfg.path()
            : cfg.path(currentNamespace);

        const data = await apiFetch(path);

        // đơn giản: map object k8s -> row {name, namespace, status, age}
        const rows = (data || []).map(item => {
            const meta = item.metadata || {};
            const status = item.status || {};
            return {
                name: meta.name || "",
                namespace: meta.namespace || "",
                status: status.phase || status.status || status.type || "",
                age: meta.creationTimestamp || ""
            };
        });

        const filterInput = $("search-input");
        const keyword = filterInput.value;
        const filtered = applyFilter(rows, keyword);

        tbody.innerHTML = "";
        filtered.forEach(r => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${r.name}</td>
                <td>${r.namespace}</td>
                <td>${r.status}</td>
                <td>${r.age}</td>
            `;
            tbody.appendChild(tr);
        });

        summary.textContent = `${filtered.length} items`;
    } catch (err) {
        console.error(err);
        error.textContent = "Không load được dữ liệu: " + err.message;
        error.hidden = false;
    } finally {
        loading.hidden = true;
    }
}

function initSearchFilter() {
    const input = $("search-input");
    input.addEventListener("input", () => {
        reloadCurrentResource();
    });
}

// Bootstrap
document.addEventListener("DOMContentLoaded", () => {
    initLogin();
    initSidebarToggle();
    initNav();
    initSearchFilter();
});
