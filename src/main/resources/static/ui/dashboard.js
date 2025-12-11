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
            path: () => `/users`
        }
    }
};

let currentSection = "workloads";
let currentResource = "pods";
let currentNamespace = null;
let currentItems = [];

document.addEventListener("DOMContentLoaded", () => {
    AxyCommon.requireLogin();
    AxyCommon.initLayoutBasics();
    AxyCommon.initDetailModal();

    initNav();
    initSearch();
    initNamespaceSelect().then(() => {
        activateDefaultNav();
    });
});

function initNav() {
    const navItems = document.querySelectorAll(".nav-item");
    navItems.forEach(item => {
        item.addEventListener("click", (e) => {
            e.preventDefault();
            navItems.forEach(i => i.classList.remove("active"));
            item.classList.add("active");

            currentSection = item.dataset.section;
            currentResource = item.dataset.resource;
            reloadCurrentResource();
        });
    });
}

function initSearch() {
    const input = document.getElementById("search-input");
    if (!input) return;
    input.addEventListener("input", () => {
        reloadCurrentResource();
    });
}

async function initNamespaceSelect() {
    const select = document.getElementById("namespace-select");
    if (!select) return;

    select.innerHTML = `<option disabled>Loading...</option>`;

    try {
        const namespaces = await AxyCommon.apiFetch("/k8s/namespaces");
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

        document.getElementById("chip-namespace").textContent = currentNamespace || "-";

        select.addEventListener("change", () => {
            currentNamespace = select.value;
            document.getElementById("chip-namespace").textContent = currentNamespace;
            reloadCurrentResource();
        });
    } catch (err) {
        console.error(err);
    }
}

function activateDefaultNav() {
    const current = document.querySelector(".nav-item[data-section='workloads'][data-resource='pods']");
    if (current) {
        current.click();
    }
}

async function reloadCurrentResource() {
    if (currentSection === "access" && currentResource === "users") {
        await reloadUsers();
        return;
    }

    const sectionCfg = RESOURCE_CONFIG[currentSection] || {};
    const cfg = sectionCfg[currentResource];
    if (!cfg) return;

    document.getElementById("resource-title").textContent = cfg.label;
    document.getElementById("resource-subtitle").textContent =
        cfg.scope === "cluster"
            ? `Cluster-scoped resource: ${cfg.label}`
            : `${cfg.label} trong namespace đã chọn.`;

    document.getElementById("chip-scope").textContent = cfg.scope;

    document.getElementById("create-user-btn").hidden = true;

    const loading = document.getElementById("loading-indicator");
    const error = document.getElementById("error-indicator");
    const tbody = document.getElementById("resource-table-body");
    const summary = document.getElementById("table-summary");
    const thead = document.getElementById("resource-table-head");

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
        const data = await AxyCommon.apiFetch(path);
        currentItems = data || [];

        const keyword = (document.getElementById("search-input").value || "").toLowerCase();
        let count = 0;

        currentItems.forEach((item, index) => {
            const meta = item.metadata || {};
            const st = item.status || {};
            const row = {
                name: meta.name || "",
                namespace: meta.namespace || "",
                status: st.phase || st.status || st.type || "",
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
            count++;
        });

        summary.textContent = `${count} items`;

        tbody.querySelectorAll("tr").forEach(tr => {
            tr.addEventListener("click", () => {
                const idx = Number(tr.dataset.index || "0");
                const item = currentItems[idx];
                const title = `${cfg.label} · ${(item.metadata && item.metadata.name) || ""}`;
                AxyCommon.openDetailModal(title, item);
            });
        });

    } catch (e) {
        error.textContent = e.message;
        error.hidden = false;
    } finally {
        loading.hidden = true;
    }
}

/** USERS PAGE đơn giản – vẫn chung file, sau này anh tách riêng users.js cũng được */
async function reloadUsers() {
    const title = document.getElementById("resource-title");
    const subtitle = document.getElementById("resource-subtitle");
    const chipScope = document.getElementById("chip-scope");
    const createBtn = document.getElementById("create-user-btn");
    const thead = document.getElementById("resource-table-head");
    const tbody = document.getElementById("resource-table-body");
    const loading = document.getElementById("loading-indicator");
    const error = document.getElementById("error-indicator");
    const summary = document.getElementById("table-summary");

    title.textContent = "Users";
    subtitle.textContent = "Quản lý user. Chỉ ADMIN / SUPER_ADMIN mới thấy trang này.";
    chipScope.textContent = "cluster";

    createBtn.hidden = false;

    thead.innerHTML = `
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Role</th>
            <th>Active</th>
        </tr>
    `;
    tbody.innerHTML = "";
    loading.hidden = false;
    error.hidden = true;

    try {
        const data = await AxyCommon.apiFetch("/users");
        currentItems = data || [];

        const keyword = (document.getElementById("search-input").value || "").toLowerCase();
        let count = 0;

        currentItems.forEach((u, index) => {
            const row = {
                id: u.id,
                username: u.username || "",
                role: u.role || "",
                active: u.active === false ? "false" : "true"
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
            count++;
        });

        summary.textContent = `${count} users`;

        // click user -> detail JSON (sau này anh đổi thành modal edit riêng)
        tbody.querySelectorAll("tr").forEach(tr => {
            tr.addEventListener("click", () => {
                const idx = Number(tr.dataset.index || "0");
                const user = currentItems[idx];
                AxyCommon.openDetailModal("User · " + (user.username || ""), user);
            });
        });

        // create user – sau này anh đổi sang modal edit
        createBtn.onclick = () => {
            alert("TODO: modal create/update user – backend cho anh sẵn thì em viết tiếp.");
        };

    } catch (e) {
        error.textContent = e.message;
        error.hidden = false;
    } finally {
        loading.hidden = true;
    }
}
