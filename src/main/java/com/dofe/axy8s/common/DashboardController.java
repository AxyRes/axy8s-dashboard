package com.dofe.axy8s.common;

import com.dofe.axy8s.k8s.KubernetesService;
import io.fabric8.kubernetes.api.model.Namespace;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Controller
public class DashboardController {

    private final KubernetesService kubernetesService;

    public DashboardController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @GetMapping("/dashboard")
    public String overview(Model model) {
        List<Namespace> namespaces = safeList(kubernetesService::listNamespaces);
        String selectedNamespace = namespaces.stream()
                .map(ns -> ns.getMetadata() != null ? ns.getMetadata().getName() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("default");

        int deploymentCount = namespaces.stream()
                .mapToInt(ns -> safeList(() -> kubernetesService.listDeployments(ns.getMetadata().getName())).size())
                .sum();
        int podCount = namespaces.stream()
                .mapToInt(ns -> safeList(() -> kubernetesService.listPods(ns.getMetadata().getName())).size())
                .sum();
        int serviceCount = namespaces.stream()
                .mapToInt(ns -> safeList(() -> kubernetesService.listServices(ns.getMetadata().getName())).size())
                .sum();

        model.addAttribute("pageTitle", "Cluster overview");
        model.addAttribute("namespaces", namespaces);
        model.addAttribute("selectedNamespace", selectedNamespace);
        model.addAttribute("selectedResource", "deployments");
        model.addAttribute("navItems", sidebarNav(selectedNamespace, null));
        model.addAttribute("clusterNavItems", clusterNav(null));
        model.addAttribute("summary", Map.of(
                "namespaces", namespaces.size(),
                "deployments", deploymentCount,
                "pods", podCount,
                "services", serviceCount
        ));

        return "dashboard";
    }

    private List<Map<String, Object>> sidebarNav(String namespace, String activeResource) {
        return List.of(
                nav("Deployments", "bi-diagram-3", namespace, "deployments", activeResource),
                nav("Pods", "bi-hdd-stack", namespace, "pods", activeResource),
                nav("Services", "bi-diagram-2", namespace, "services", activeResource),
                nav("ConfigMaps", "bi-braces", namespace, "configmaps", activeResource),
                nav("Secrets", "bi-key", namespace, "secrets", activeResource),
                nav("CronJobs", "bi-clock-history", namespace, "cronjobs", activeResource),
                nav("Jobs", "bi-briefcase", namespace, "jobs", activeResource),
                nav("ReplicaSets", "bi-layers", namespace, "replicasets", activeResource),
                nav("StatefulSets", "bi-collection", namespace, "statefulsets", activeResource),
                nav("DaemonSets", "bi-cpu", namespace, "daemonsets", activeResource),
                nav("Ingresses", "bi-diagram-3-fill", namespace, "ingresses", activeResource),
                nav("HPA", "bi-graph-up", namespace, "hpa", activeResource),
                nav("Service Accounts", "bi-people", namespace, "serviceaccounts", activeResource)
        );
    }

    private List<Map<String, Object>> clusterNav(String activeResource) {
        return List.of(
                nav("Persistent Volumes", "bi-hdd-network", "cluster", "persistentvolume", activeResource),
                nav("Storage Classes", "bi-database", "cluster", "storageclass", activeResource)
        );
    }

    private Map<String, Object> nav(String label, String icon, String namespace, String resource, String activeResource) {
        String href = namespace != null && !"cluster".equals(namespace)
                ? "/k8s/" + namespace + "/" + resource
                : "/k8s/cluster/" + resource;

        return Map.of(
                "label", label,
                "icon", icon,
                "href", href,
                "active", Objects.equals(resource, activeResource)
        );
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier) {
        try {
            List<T> data = supplier.get();
            return data != null ? data : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
