package com.dofe.axy8s.k8s;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/k8s")
public class K8sDashboardController {

    private final KubernetesService kubernetesService;

    public K8sDashboardController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @GetMapping
    public String redirectToFirstNamespace() {
        String targetNamespace = safeList(kubernetesService::listNamespaces).stream()
                .map(ns -> ns.getMetadata() != null ? ns.getMetadata().getName() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("default");

        return "redirect:/k8s/" + targetNamespace + "/deployments";
    }

    @GetMapping("/{namespace}/{resource}")
    public String namespaceResource(@PathVariable String namespace,
                                    @PathVariable String resource,
                                    Model model) {
        String normalized = normalizeResource(resource);
        List<Namespace> namespaces = safeList(kubernetesService::listNamespaces);
        ResourceDataset dataset = buildNamespaceDataset(namespace, normalized);

        model.addAttribute("pageTitle", dataset.title());
        model.addAttribute("selectedNamespace", namespace);
        model.addAttribute("namespaces", namespaces);
        model.addAttribute("navItems", namespaceNavItems(namespace, normalized));
        model.addAttribute("clusterNavItems", clusterNavItems(null));
        model.addAttribute("tableColumns", dataset.columns());
        model.addAttribute("tableRows", dataset.rows());
        model.addAttribute("summary", dataset.summary());
        model.addAttribute("selectedResource", normalized);

        return "k8s/resource-list";
    }

    @GetMapping("/{namespace}/{resource}/{name}")
    public String namespaceResourceDetail(@PathVariable String namespace,
                                          @PathVariable String resource,
                                          @PathVariable String name,
                                          Model model) {
        String normalized = normalizeResource(resource);
        ResourceDetail detail = buildNamespaceDetail(namespace, normalized, name);

        model.addAttribute("pageTitle", detail.title());
        model.addAttribute("selectedNamespace", namespace);
        model.addAttribute("namespaces", safeList(kubernetesService::listNamespaces));
        model.addAttribute("navItems", namespaceNavItems(namespace, normalized));
        model.addAttribute("clusterNavItems", clusterNavItems(null));
        model.addAttribute("selectedResource", normalized);
        model.addAttribute("detail", detail);

        return "k8s/resource-detail";
    }

    @GetMapping("/cluster/{resource}")
    public String clusterResource(@PathVariable String resource, Model model) {
        ResourceDataset dataset = buildClusterDataset(resource.toLowerCase());

        model.addAttribute("pageTitle", dataset.title());
        model.addAttribute("namespaces", safeList(kubernetesService::listNamespaces));
        model.addAttribute("navItems", namespaceNavItems(null, null));
        model.addAttribute("clusterNavItems", clusterNavItems(resource.toLowerCase()));
        model.addAttribute("tableColumns", dataset.columns());
        model.addAttribute("tableRows", dataset.rows());
        model.addAttribute("summary", dataset.summary());
        model.addAttribute("selectedResource", resource.toLowerCase());

        return "k8s/cluster-resource-list";
    }

    private ResourceDataset buildNamespaceDataset(String namespace, String resource) {
        return switch (resource) {
            case "deployments" -> deploymentsDataset(namespace);
            case "pods" -> podsDataset(namespace);
            case "services" -> servicesDataset(namespace);
            case "configmaps" -> configMapsDataset(namespace);
            case "secrets" -> secretsDataset(namespace);
            case "cronjobs" -> cronJobsDataset(namespace);
            case "jobs" -> jobsDataset(namespace);
            case "replicasets" -> replicaSetsDataset(namespace);
            case "statefulsets" -> statefulSetsDataset(namespace);
            case "daemonsets" -> daemonSetsDataset(namespace);
            case "ingresses" -> ingressDataset(namespace);
            case "hpa" -> hpaDataset(namespace);
            case "serviceaccounts" -> serviceAccountDataset(namespace);
            default -> deploymentsDataset(namespace);
        };
    }

    private ResourceDataset buildClusterDataset(String resource) {
        return switch (resource) {
            case "persistentvolume" -> persistentVolumeDataset();
            case "storageclass" -> storageClassDataset();
            default -> persistentVolumeDataset();
        };
    }

    private ResourceDataset deploymentsDataset(String namespace) {
        List<Deployment> deployments = safeList(() -> kubernetesService.listDeployments(namespace));
        List<ResourceRow> rows = deployments.stream()
                .map(dep -> {
                    String name = dep.getMetadata() != null ? dep.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            readiness(dep.getStatus() != null ? dep.getStatus().getReadyReplicas() : null,
                                    dep.getStatus() != null ? dep.getStatus().getReplicas() : null),
                            dep.getSpec() != null && dep.getSpec().getTemplate() != null && dep.getSpec().getTemplate().getSpec() != null
                                    && dep.getSpec().getTemplate().getSpec().getContainers() != null
                                    ? dep.getSpec().getTemplate().getSpec().getContainers().stream()
                                    .map(container -> value(container.getImage()))
                                    .collect(Collectors.joining(", "))
                                    : "-",
                            age(dep.getMetadata() != null ? dep.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "deployments", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();

        Map<String, WorkloadCard> summary = Map.of(
                "deployments", new WorkloadCard("Deployments", deployments.size(), readyDeployments(deployments)),
                "pods", new WorkloadCard("Pods", totalPods(namespace), readyPodsCount(namespace)),
                "statefulsets", new WorkloadCard("StatefulSets", safeList(() -> kubernetesService.listStatefulSets(namespace)).size(), readyStatefulSets(namespace)),
                "daemonsets", new WorkloadCard("DaemonSets", safeList(() -> kubernetesService.listDaemonSets(namespace)).size(), readyDaemonSets(namespace))
        );

        return new ResourceDataset("Deployments", tableColumnsWithNamespace(), rows, summary);
    }

    private ResourceDataset podsDataset(String namespace) {
        List<Pod> pods = safeList(() -> kubernetesService.listPods(namespace));
        List<ResourceRow> rows = pods.stream()
                .map(pod -> {
                    String name = pod.getMetadata() != null ? pod.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            pod.getStatus() != null ? value(pod.getStatus().getPhase()) : "-",
                            pod.getSpec() != null && pod.getSpec().getContainers() != null
                                    ? pod.getSpec().getContainers().stream()
                                    .map(container -> value(container.getName()))
                                    .collect(Collectors.joining(", ")) : "-",
                            age(pod.getMetadata() != null ? pod.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "pods", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();

        return new ResourceDataset("Pods", tableColumnsWithNamespace(), rows, Map.of(
                "pods", new WorkloadCard("Pods", pods.size(), readyPods(pods))
        ));
    }

    private ResourceDataset servicesDataset(String namespace) {
        List<Service> services = safeList(() -> kubernetesService.listServices(namespace));
        List<ResourceRow> rows = services.stream()
                .map(service -> {
                    String name = service.getMetadata() != null ? service.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            service.getSpec() != null ? value(service.getSpec().getType()) : "-",
                            service.getSpec() != null ? value(service.getSpec().getClusterIP()) : "-",
                            age(service.getMetadata() != null ? service.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "services", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Services", tableColumnsWithNamespace(), rows, Map.of(
                "services", new WorkloadCard("Services", services.size(), services.size())
        ));
    }

    private ResourceDataset configMapsDataset(String namespace) {
        List<ConfigMap> configMaps = safeList(() -> kubernetesService.listConfigMaps(namespace));
        List<ResourceRow> rows = configMaps.stream()
                .map(cm -> {
                    String name = cm.getMetadata() != null ? cm.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            "Data entries: " + (cm.getData() != null ? cm.getData().size() : 0),
                            cm.getImmutable() != null && cm.getImmutable() ? "Immutable" : "Mutable",
                            age(cm.getMetadata() != null ? cm.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "configmaps", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("ConfigMaps", tableColumnsWithNamespace(), rows, Map.of(
                "configmaps", new WorkloadCard("ConfigMaps", configMaps.size(), configMaps.size())
        ));
    }

    private ResourceDataset secretsDataset(String namespace) {
        List<Secret> secrets = safeList(() -> kubernetesService.listSecrets(namespace));
        List<ResourceRow> rows = secrets.stream()
                .map(secret -> {
                    String name = secret.getMetadata() != null ? secret.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            value(secret.getType()),
                            "Keys: " + (secret.getData() != null ? secret.getData().size() : 0),
                            age(secret.getMetadata() != null ? secret.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "secrets", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Secrets", tableColumnsWithNamespace(), rows, Map.of(
                "secrets", new WorkloadCard("Secrets", secrets.size(), secrets.size())
        ));
    }

    private ResourceDataset cronJobsDataset(String namespace) {
        List<CronJob> cronJobs = safeList(() -> kubernetesService.listCronJobs(namespace));
        List<ResourceRow> rows = cronJobs.stream()
                .map(cj -> {
                    String name = cj.getMetadata() != null ? cj.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            cj.getSpec() != null ? value(cj.getSpec().getSchedule()) : "-",
                            cj.getSpec() != null && Boolean.TRUE.equals(cj.getSpec().getSuspend()) ? "Suspended" : "Active",
                            age(cj.getMetadata() != null ? cj.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "cronjobs", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("CronJobs", tableColumnsWithNamespace(), rows, Map.of(
                "cronjobs", new WorkloadCard("CronJobs", cronJobs.size(), cronJobs.size())
        ));
    }

    private ResourceDataset jobsDataset(String namespace) {
        List<Job> jobs = safeList(() -> kubernetesService.listJobs(namespace));
        List<ResourceRow> rows = jobs.stream()
                .map(job -> {
                    String name = job.getMetadata() != null ? job.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            job.getStatus() != null ? value(job.getStatus().getSucceeded()) + " succeeded" : "-",
                            job.getStatus() != null ? value(job.getStatus().getActive()) + " active" : "-",
                            age(job.getMetadata() != null ? job.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "jobs", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Jobs", tableColumnsWithNamespace(), rows, Map.of(
                "jobs", new WorkloadCard("Jobs", jobs.size(), jobs.size())
        ));
    }

    private ResourceDataset replicaSetsDataset(String namespace) {
        List<ReplicaSet> replicaSets = safeList(() -> kubernetesService.listReplicaSets(namespace));
        List<ResourceRow> rows = replicaSets.stream()
                .map(rs -> {
                    String name = rs.getMetadata() != null ? rs.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            readiness(rs.getStatus() != null ? rs.getStatus().getReadyReplicas() : null,
                                    rs.getStatus() != null ? rs.getStatus().getReplicas() : null),
                            rs.getSpec() != null ? "Replicas: " + value(rs.getSpec().getReplicas()) : "-",
                            age(rs.getMetadata() != null ? rs.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "replicasets", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("ReplicaSets", tableColumnsWithNamespace(), rows, Map.of(
                "replicasets", new WorkloadCard("ReplicaSets", replicaSets.size(), readyReplicaSets(replicaSets))
        ));
    }

    private ResourceDataset statefulSetsDataset(String namespace) {
        List<StatefulSet> statefulSets = safeList(() -> kubernetesService.listStatefulSets(namespace));
        List<ResourceRow> rows = statefulSets.stream()
                .map(sts -> {
                    String name = sts.getMetadata() != null ? sts.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            readiness(sts.getStatus() != null ? sts.getStatus().getReadyReplicas() : null,
                                    sts.getStatus() != null ? sts.getStatus().getReplicas() : null),
                            sts.getSpec() != null ? "Replicas: " + value(sts.getSpec().getReplicas()) : "-",
                            age(sts.getMetadata() != null ? sts.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "statefulsets", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("StatefulSets", tableColumnsWithNamespace(), rows, Map.of(
                "statefulsets", new WorkloadCard("StatefulSets", statefulSets.size(), readyStatefulSets(statefulSets))
        ));
    }

    private ResourceDataset daemonSetsDataset(String namespace) {
        List<DaemonSet> daemonSets = safeList(() -> kubernetesService.listDaemonSets(namespace));
        List<ResourceRow> rows = daemonSets.stream()
                .map(ds -> {
                    String name = ds.getMetadata() != null ? ds.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            readiness(ds.getStatus() != null ? ds.getStatus().getNumberReady() : null,
                                    ds.getStatus() != null ? ds.getStatus().getDesiredNumberScheduled() : null),
                            ds.getStatus() != null ? "Scheduled: " + value(ds.getStatus().getCurrentNumberScheduled()) : "-",
                            age(ds.getMetadata() != null ? ds.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "daemonsets", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("DaemonSets", tableColumnsWithNamespace(), rows, Map.of(
                "daemonsets", new WorkloadCard("DaemonSets", daemonSets.size(), readyDaemonSets(daemonSets))
        ));
    }

    private ResourceDataset ingressDataset(String namespace) {
        List<Ingress> ingresses = safeList(() -> kubernetesService.listIngresses(namespace));
        List<ResourceRow> rows = ingresses.stream()
                .map(ingress -> {
                    String name = ingress.getMetadata() != null ? ingress.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            ingress.getStatus() != null && ingress.getStatus().getLoadBalancer() != null
                                    ? ingress.getStatus().getLoadBalancer().getIngress().toString() : "-",
                            ingress.getSpec() != null && ingress.getSpec().getRules() != null
                                    ? ingress.getSpec().getRules().stream()
                                    .map(rule -> value(rule.getHost()))
                                    .collect(Collectors.joining(", ")) : "-",
                            age(ingress.getMetadata() != null ? ingress.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "ingresses", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Ingresses", tableColumnsWithNamespace(), rows, Map.of(
                "ingress", new WorkloadCard("Ingress", ingresses.size(), ingresses.size())
        ));
    }

    private ResourceDataset hpaDataset(String namespace) {
        List<HorizontalPodAutoscaler> hpas = safeList(() -> kubernetesService.listHorizontalPodAutoscalers(namespace));
        List<ResourceRow> rows = hpas.stream()
                .map(hpa -> {
                    String name = hpa.getMetadata() != null ? hpa.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            hpa.getStatus() != null ? value(hpa.getStatus().getCurrentReplicas()) + " current" : "-",
                            hpa.getSpec() != null ? "Target: " + value(hpa.getSpec().getMaxReplicas()) : "-",
                            age(hpa.getMetadata() != null ? hpa.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "hpa", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Horizontal Pod Autoscalers", tableColumnsWithNamespace(), rows, Map.of(
                "hpa", new WorkloadCard("HPA", hpas.size(), hpas.size())
        ));
    }

    private ResourceDataset serviceAccountDataset(String namespace) {
        List<ServiceAccount> sas = safeList(() -> kubernetesService.listServiceAccounts(namespace));
        List<ResourceRow> rows = sas.stream()
                .map(sa -> {
                    String name = sa.getMetadata() != null ? sa.getMetadata().getName() : null;
                    return new ResourceRow(
                            value(name),
                            namespace,
                            "Secrets: " + (sa.getSecrets() != null ? sa.getSecrets().size() : 0),
                            sa.getAutomountServiceAccountToken() != null && sa.getAutomountServiceAccountToken()
                                    ? "Auto mount" : "Manual",
                            age(sa.getMetadata() != null ? sa.getMetadata().getCreationTimestamp() : null),
                            rowHref(namespace, "serviceaccounts", name)
                    );
                })
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Service Accounts", tableColumnsWithNamespace(), rows, Map.of(
                "serviceaccounts", new WorkloadCard("ServiceAccounts", sas.size(), sas.size())
        ));
    }

    private ResourceDataset persistentVolumeDataset() {
        List<PersistentVolume> pvs = safeList(kubernetesService::listPersistentVolumes);
        List<ResourceRow> rows = pvs.stream()
                .map(pv -> new ResourceRow(
                        value(pv.getMetadata() != null ? pv.getMetadata().getName() : null),
                        null,
                        pv.getStatus() != null ? value(pv.getStatus().getPhase()) : "-",
                        pv.getSpec() != null && pv.getSpec().getCapacity() != null
                                ? pv.getSpec().getCapacity().toString() : "-",
                        age(pv.getMetadata() != null ? pv.getMetadata().getCreationTimestamp() : null),
                        null
                ))
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Persistent Volumes", tableColumnsCluster(), rows, Map.of(
                "persistentvolumes", new WorkloadCard("PersistentVolumes", pvs.size(), pvs.size())
        ));
    }

    private ResourceDataset storageClassDataset() {
        List<StorageClass> storageClasses = safeList(kubernetesService::listStorageClasses);
        List<ResourceRow> rows = storageClasses.stream()
                .map(sc -> new ResourceRow(
                        value(sc.getMetadata() != null ? sc.getMetadata().getName() : null),
                        null,
                        value(sc.getProvisioner()),
                        sc.getReclaimPolicy() != null ? value(sc.getReclaimPolicy()) : "-",
                        age(sc.getMetadata() != null ? sc.getMetadata().getCreationTimestamp() : null),
                        null
                ))
                .sorted(Comparator.comparing(ResourceRow::name))
                .toList();
        return new ResourceDataset("Storage Classes", tableColumnsCluster(), rows, Map.of(
                "storageclasses", new WorkloadCard("StorageClasses", storageClasses.size(), storageClasses.size())
        ));
    }

    private List<TableColumn> tableColumnsWithNamespace() {
        return List.of(
                new TableColumn("Name"),
                new TableColumn("Namespace"),
                new TableColumn("Status"),
                new TableColumn("Details"),
                new TableColumn("Age")
        );
    }

    private List<TableColumn> tableColumnsCluster() {
        return List.of(
                new TableColumn("Name"),
                new TableColumn("Status"),
                new TableColumn("Details"),
                new TableColumn("Age")
        );
    }

    private List<NavItem> namespaceNavItems(String namespace, String activeResource) {
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

    private List<NavItem> clusterNavItems(String activeResource) {
        return List.of(
                clusterNav("Persistent Volumes", "bi-hdd-network", "persistentvolume", activeResource),
                clusterNav("Storage Classes", "bi-database", "storageclass", activeResource)
        );
    }

    private NavItem nav(String label, String icon, String namespace, String resource, String activeResource) {
        String href = namespace != null ? "/k8s/" + namespace + "/" + resource : "#";
        return new NavItem(label, icon, href, Objects.equals(resource, activeResource));
    }

    private NavItem clusterNav(String label, String icon, String resource, String activeResource) {
        return new NavItem(label, icon, "/k8s/cluster/" + resource, Objects.equals(resource, activeResource));
    }

    private String normalizeResource(String resource) {
        if (resource == null) {
            return "deployments";
        }
        String lower = resource.toLowerCase();
        return switch (lower) {
            case "deployment", "deployments" -> "deployments";
            case "pod", "pods" -> "pods";
            case "service", "services" -> "services";
            case "configmap", "configmaps" -> "configmaps";
            case "secret", "secrets" -> "secrets";
            case "cronjob", "cronjobs" -> "cronjobs";
            case "job", "jobs" -> "jobs";
            case "replicaset", "replicasets" -> "replicasets";
            case "statefulset", "statefulsets" -> "statefulsets";
            case "daemonset", "daemonsets" -> "daemonsets";
            case "ingress", "ingresses" -> "ingresses";
            case "hpa", "horizontalpodautoscalers" -> "hpa";
            case "serviceaccount", "serviceaccounts" -> "serviceaccounts";
            default -> "deployments";
        };
    }

    private String rowHref(String namespace, String resource, String name) {
        if (namespace == null || name == null || name.isBlank()) {
            return null;
        }
        return "/k8s/" + namespace + "/" + resource + "/" + name;
    }

    private String readiness(Integer ready, Integer total) {
        int readyVal = ready != null ? ready : 0;
        int totalVal = total != null ? total : 0;
        return readyVal + "/" + totalVal;
    }

    private int readyPodsCount(String namespace) {
        return readyPods(safeList(() -> kubernetesService.listPods(namespace)));
    }

    private int totalPods(String namespace) {
        return safeList(() -> kubernetesService.listPods(namespace)).size();
    }

    private int readyStatefulSets(String namespace) {
        return readyStatefulSets(safeList(() -> kubernetesService.listStatefulSets(namespace)));
    }

    private int readyDaemonSets(String namespace) {
        return readyDaemonSets(safeList(() -> kubernetesService.listDaemonSets(namespace)));
    }

    private String age(String creationTimestamp) {
        if (creationTimestamp == null) {
            return "-";
        }
        try {
            OffsetDateTime created = OffsetDateTime.parse(creationTimestamp);
            Duration duration = Duration.between(created, OffsetDateTime.now(ZoneOffset.UTC));
            long days = duration.toDays();
            if (days > 0) {
                return days + "d";
            }
            long hours = duration.toHours();
            if (hours > 0) {
                return hours + "h";
            }
            long minutes = duration.toMinutes();
            return minutes + "m";
        } catch (Exception ex) {
            return "-";
        }
    }

    private String value(Object obj) {
        return obj != null ? obj.toString() : "-";
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier) {
        try {
            List<T> result = supplier.get();
            return result != null ? result : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private int readyDeployments(List<Deployment> deployments) {
        return (int) deployments.stream()
                .filter(dep -> dep.getStatus() != null)
                .filter(dep -> Objects.equals(dep.getStatus().getReadyReplicas(), dep.getStatus().getReplicas()))
                .count();
    }

    private int readyPods(List<Pod> pods) {
        return (int) pods.stream()
                .filter(pod -> pod.getStatus() != null)
                .filter(pod -> "Running".equalsIgnoreCase(pod.getStatus().getPhase()))
                .count();
    }

    private int readyReplicaSets(List<ReplicaSet> replicaSets) {
        return (int) replicaSets.stream()
                .filter(rs -> rs.getStatus() != null)
                .filter(rs -> Objects.equals(rs.getStatus().getReadyReplicas(), rs.getStatus().getReplicas()))
                .count();
    }

    private int readyDaemonSets(List<DaemonSet> daemonSets) {
        return (int) daemonSets.stream()
                .filter(ds -> ds.getStatus() != null)
                .filter(ds -> Objects.equals(ds.getStatus().getNumberReady(), ds.getStatus().getDesiredNumberScheduled()))
                .count();
    }

    private int readyStatefulSets(List<StatefulSet> statefulSets) {
        return (int) statefulSets.stream()
                .filter(sts -> sts.getStatus() != null)
                .filter(sts -> Objects.equals(sts.getStatus().getReadyReplicas(), sts.getStatus().getReplicas()))
                .count();
    }

    private ResourceDetail buildNamespaceDetail(String namespace, String resource, String name) {
        return switch (resource) {
            case "deployments" -> deploymentDetail(namespace, name);
            case "pods" -> podDetail(namespace, name);
            case "services" -> serviceDetail(namespace, name);
            case "configmaps" -> configMapDetail(namespace, name);
            case "secrets" -> secretDetail(namespace, name);
            case "cronjobs" -> cronJobDetail(namespace, name);
            case "jobs" -> jobDetail(namespace, name);
            case "replicasets" -> replicaSetDetail(namespace, name);
            case "statefulsets" -> statefulSetDetail(namespace, name);
            case "daemonsets" -> daemonSetDetail(namespace, name);
            case "ingresses" -> ingressDetail(namespace, name);
            case "hpa" -> hpaDetail(namespace, name);
            case "serviceaccounts" -> serviceAccountDetail(namespace, name);
            default -> deploymentDetail(namespace, name);
        };
    }

    private ResourceDetail deploymentDetail(String namespace, String name) {
        Deployment dep = kubernetesService.getDeployment(namespace, name);
        if (dep == null) {
            return missingDetail("Deployment", namespace, name);
        }
        String ready = readiness(dep.getStatus() != null ? dep.getStatus().getReadyReplicas() : null,
                dep.getStatus() != null ? dep.getStatus().getReplicas() : null);

        Map<String, String> overview = Map.of(
                "Namespace", namespace,
                "Ready", ready,
                "Updated", dep.getStatus() != null ? value(dep.getStatus().getUpdatedReplicas()) : "-",
                "Strategy", dep.getSpec() != null && dep.getSpec().getStrategy() != null
                        ? value(dep.getSpec().getStrategy().getType()) : "-",
                "Age", age(dep.getMetadata() != null ? dep.getMetadata().getCreationTimestamp() : null)
        );

        Map<String, String> spec = Map.of(
                "Selector", dep.getSpec() != null && dep.getSpec().getSelector() != null && dep.getSpec().getSelector().getMatchLabels() != null
                        ? dep.getSpec().getSelector().getMatchLabels().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")) : "-",
                "Replicas", dep.getSpec() != null ? value(dep.getSpec().getReplicas()) : "-",
                "Min Ready Seconds", dep.getSpec() != null ? value(dep.getSpec().getMinReadySeconds()) : "-"
        );

        Map<String, String> status = Map.of(
                "Available", dep.getStatus() != null ? value(dep.getStatus().getAvailableReplicas()) : "-",
                "Unavailable", dep.getStatus() != null ? value(dep.getStatus().getUnavailableReplicas()) : "-",
                "Conditions", dep.getStatus() != null && dep.getStatus().getConditions() != null
                        ? value(dep.getStatus().getConditions().size()) + " conditions" : "-"
        );

        List<DetailSection> sections = List.of(
                new DetailSection("Overview", overview),
                new DetailSection("Spec", spec),
                new DetailSection("Status", status)
        );

        return new ResourceDetail(
                "Deployment: " + name,
                "Deployment",
                name,
                namespace,
                ready,
                age(dep.getMetadata() != null ? dep.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(dep.getMetadata() != null ? dep.getMetadata().getLabels() : null),
                labelList(dep.getMetadata() != null ? dep.getMetadata().getAnnotations() : null),
                containersFromTemplate(dep)
        );
    }

    private ResourceDetail podDetail(String namespace, String name) {
        Pod pod = kubernetesService.getPod(namespace, name);
        if (pod == null) {
            return missingDetail("Pod", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Phase", pod.getStatus() != null ? value(pod.getStatus().getPhase()) : "-",
                "Pod IP", pod.getStatus() != null ? value(pod.getStatus().getPodIP()) : "-",
                "Node", pod.getSpec() != null ? value(pod.getSpec().getNodeName()) : "-",
                "Restart Count", pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null
                        ? value(pod.getStatus().getContainerStatuses().stream()
                        .mapToInt(cs -> cs.getRestartCount() != null ? cs.getRestartCount() : 0)
                        .sum()) : "-",
                "Age", age(pod.getMetadata() != null ? pod.getMetadata().getCreationTimestamp() : null)
        );

        Map<String, String> spec = Map.of(
                "QoS", pod.getStatus() != null ? value(pod.getStatus().getQosClass()) : "-",
                "Service Account", pod.getSpec() != null ? value(pod.getSpec().getServiceAccountName()) : "-",
                "Priority", pod.getSpec() != null ? value(pod.getSpec().getPriority()) : "-"
        );

        List<DetailSection> sections = List.of(
                new DetailSection("Overview", overview),
                new DetailSection("Spec", spec)
        );

        return new ResourceDetail(
                "Pod: " + name,
                "Pod",
                name,
                namespace,
                pod.getStatus() != null ? value(pod.getStatus().getPhase()) : "-",
                age(pod.getMetadata() != null ? pod.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(pod.getMetadata() != null ? pod.getMetadata().getLabels() : null),
                labelList(pod.getMetadata() != null ? pod.getMetadata().getAnnotations() : null),
                containersOf(pod.getSpec() != null ? pod.getSpec().getContainers() : null)
        );
    }

    private ResourceDetail serviceDetail(String namespace, String name) {
        Service service = kubernetesService.getService(namespace, name);
        if (service == null) {
            return missingDetail("Service", namespace, name);
        }

        Map<String, String> overview = Map.of(
                "Type", service.getSpec() != null ? value(service.getSpec().getType()) : "-",
                "Cluster IP", service.getSpec() != null ? value(service.getSpec().getClusterIP()) : "-",
                "Ports", service.getSpec() != null && service.getSpec().getPorts() != null
                        ? service.getSpec().getPorts().stream()
                        .map(p -> value(p.getPort()) + "/" + value(p.getProtocol()))
                        .collect(Collectors.joining(", ")) : "-",
                "Selector", service.getSpec() != null && service.getSpec().getSelector() != null
                        ? service.getSpec().getSelector().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")) : "-",
                "Age", age(service.getMetadata() != null ? service.getMetadata().getCreationTimestamp() : null)
        );

        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "Service: " + name,
                "Service",
                name,
                namespace,
                service.getSpec() != null ? value(service.getSpec().getType()) : "-",
                age(service.getMetadata() != null ? service.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(service.getMetadata() != null ? service.getMetadata().getLabels() : null),
                labelList(service.getMetadata() != null ? service.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail configMapDetail(String namespace, String name) {
        ConfigMap cm = kubernetesService.getConfigMap(namespace, name);
        if (cm == null) {
            return missingDetail("ConfigMap", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Data entries", cm.getData() != null ? value(cm.getData().size()) : "0",
                "Immutable", cm.getImmutable() != null && cm.getImmutable() ? "Yes" : "No",
                "Age", age(cm.getMetadata() != null ? cm.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "ConfigMap: " + name,
                "ConfigMap",
                name,
                namespace,
                "Ready",
                age(cm.getMetadata() != null ? cm.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(cm.getMetadata() != null ? cm.getMetadata().getLabels() : null),
                labelList(cm.getMetadata() != null ? cm.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail secretDetail(String namespace, String name) {
        Secret secret = kubernetesService.getSecret(namespace, name);
        if (secret == null) {
            return missingDetail("Secret", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Type", value(secret.getType()),
                "Keys", secret.getData() != null ? value(secret.getData().size()) : "0",
                "Age", age(secret.getMetadata() != null ? secret.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "Secret: " + name,
                "Secret",
                name,
                namespace,
                value(secret.getType()),
                age(secret.getMetadata() != null ? secret.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(secret.getMetadata() != null ? secret.getMetadata().getLabels() : null),
                labelList(secret.getMetadata() != null ? secret.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail cronJobDetail(String namespace, String name) {
        CronJob cj = kubernetesService.getCronJob(namespace, name);
        if (cj == null) {
            return missingDetail("CronJob", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Schedule", cj.getSpec() != null ? value(cj.getSpec().getSchedule()) : "-",
                "Suspend", cj.getSpec() != null && Boolean.TRUE.equals(cj.getSpec().getSuspend()) ? "Yes" : "No",
                "Age", age(cj.getMetadata() != null ? cj.getMetadata().getCreationTimestamp() : null)
        );

        Map<String, String> status = Map.of(
                "Active Jobs", cj.getStatus() != null && cj.getStatus().getActive() != null ? value(cj.getStatus().getActive().size()) : "0",
                "Last Schedule", cj.getStatus() != null ? value(cj.getStatus().getLastScheduleTime()) : "-",
                "Last Successful", cj.getStatus() != null ? value(cj.getStatus().getLastSuccessfulTime()) : "-"
        );

        List<DetailSection> sections = List.of(
                new DetailSection("Overview", overview),
                new DetailSection("Status", status)
        );

        return new ResourceDetail(
                "CronJob: " + name,
                "CronJob",
                name,
                namespace,
                cj.getSpec() != null && Boolean.TRUE.equals(cj.getSpec().getSuspend()) ? "Suspended" : "Active",
                age(cj.getMetadata() != null ? cj.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(cj.getMetadata() != null ? cj.getMetadata().getLabels() : null),
                labelList(cj.getMetadata() != null ? cj.getMetadata().getAnnotations() : null),
                containersOf(cj.getSpec() != null && cj.getSpec().getJobTemplate() != null
                        && cj.getSpec().getJobTemplate().getSpec() != null
                        && cj.getSpec().getJobTemplate().getSpec().getTemplate() != null
                        && cj.getSpec().getJobTemplate().getSpec().getTemplate().getSpec() != null
                        ? cj.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers() : null)
        );
    }

    private ResourceDetail jobDetail(String namespace, String name) {
        Job job = kubernetesService.getJob(namespace, name);
        if (job == null) {
            return missingDetail("Job", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Completions", job.getSpec() != null ? value(job.getSpec().getCompletions()) : "-",
                "Parallelism", job.getSpec() != null ? value(job.getSpec().getParallelism()) : "-",
                "Active Deadline Seconds", job.getSpec() != null ? value(job.getSpec().getActiveDeadlineSeconds()) : "-",
                "Age", age(job.getMetadata() != null ? job.getMetadata().getCreationTimestamp() : null)
        );

        Map<String, String> status = Map.of(
                "Succeeded", job.getStatus() != null ? value(job.getStatus().getSucceeded()) : "0",
                "Active", job.getStatus() != null ? value(job.getStatus().getActive()) : "0",
                "Failed", job.getStatus() != null ? value(job.getStatus().getFailed()) : "0"
        );

        List<DetailSection> sections = List.of(
                new DetailSection("Overview", overview),
                new DetailSection("Status", status)
        );

        return new ResourceDetail(
                "Job: " + name,
                "Job",
                name,
                namespace,
                job.getStatus() != null ? value(job.getStatus().getSucceeded()) + " succeeded" : "-",
                age(job.getMetadata() != null ? job.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(job.getMetadata() != null ? job.getMetadata().getLabels() : null),
                labelList(job.getMetadata() != null ? job.getMetadata().getAnnotations() : null),
                containersOf(job.getSpec() != null && job.getSpec().getTemplate() != null && job.getSpec().getTemplate().getSpec() != null
                        ? job.getSpec().getTemplate().getSpec().getContainers() : null)
        );
    }

    private ResourceDetail replicaSetDetail(String namespace, String name) {
        ReplicaSet rs = kubernetesService.getReplicaSet(namespace, name);
        if (rs == null) {
            return missingDetail("ReplicaSet", namespace, name);
        }
        String ready = readiness(rs.getStatus() != null ? rs.getStatus().getReadyReplicas() : null,
                rs.getStatus() != null ? rs.getStatus().getReplicas() : null);
        Map<String, String> overview = Map.of(
                "Replicas", rs.getSpec() != null ? value(rs.getSpec().getReplicas()) : "-",
                "Ready", ready,
                "Fully Labeled", rs.getStatus() != null ? value(rs.getStatus().getFullyLabeledReplicas()) : "-",
                "Age", age(rs.getMetadata() != null ? rs.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "ReplicaSet: " + name,
                "ReplicaSet",
                name,
                namespace,
                ready,
                age(rs.getMetadata() != null ? rs.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(rs.getMetadata() != null ? rs.getMetadata().getLabels() : null),
                labelList(rs.getMetadata() != null ? rs.getMetadata().getAnnotations() : null),
                containersOf(rs.getSpec() != null && rs.getSpec().getTemplate() != null && rs.getSpec().getTemplate().getSpec() != null
                        ? rs.getSpec().getTemplate().getSpec().getContainers() : null)
        );
    }

    private ResourceDetail statefulSetDetail(String namespace, String name) {
        StatefulSet sts = kubernetesService.getStatefulSet(namespace, name);
        if (sts == null) {
            return missingDetail("StatefulSet", namespace, name);
        }
        String ready = readiness(sts.getStatus() != null ? sts.getStatus().getReadyReplicas() : null,
                sts.getStatus() != null ? sts.getStatus().getReplicas() : null);
        Map<String, String> overview = Map.of(
                "Replicas", sts.getSpec() != null ? value(sts.getSpec().getReplicas()) : "-",
                "Ready", ready,
                "Service", sts.getSpec() != null ? value(sts.getSpec().getServiceName()) : "-",
                "Age", age(sts.getMetadata() != null ? sts.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "StatefulSet: " + name,
                "StatefulSet",
                name,
                namespace,
                ready,
                age(sts.getMetadata() != null ? sts.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(sts.getMetadata() != null ? sts.getMetadata().getLabels() : null),
                labelList(sts.getMetadata() != null ? sts.getMetadata().getAnnotations() : null),
                containersOf(sts.getSpec() != null && sts.getSpec().getTemplate() != null && sts.getSpec().getTemplate().getSpec() != null
                        ? sts.getSpec().getTemplate().getSpec().getContainers() : null)
        );
    }

    private ResourceDetail daemonSetDetail(String namespace, String name) {
        DaemonSet ds = kubernetesService.getDaemonSet(namespace, name);
        if (ds == null) {
            return missingDetail("DaemonSet", namespace, name);
        }
        String ready = readiness(ds.getStatus() != null ? ds.getStatus().getNumberReady() : null,
                ds.getStatus() != null ? ds.getStatus().getDesiredNumberScheduled() : null);
        Map<String, String> overview = Map.of(
                "Desired", ds.getStatus() != null ? value(ds.getStatus().getDesiredNumberScheduled()) : "-",
                "Ready", ready,
                "Current", ds.getStatus() != null ? value(ds.getStatus().getCurrentNumberScheduled()) : "-",
                "Age", age(ds.getMetadata() != null ? ds.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "DaemonSet: " + name,
                "DaemonSet",
                name,
                namespace,
                ready,
                age(ds.getMetadata() != null ? ds.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(ds.getMetadata() != null ? ds.getMetadata().getLabels() : null),
                labelList(ds.getMetadata() != null ? ds.getMetadata().getAnnotations() : null),
                containersOf(ds.getSpec() != null && ds.getSpec().getTemplate() != null && ds.getSpec().getTemplate().getSpec() != null
                        ? ds.getSpec().getTemplate().getSpec().getContainers() : null)
        );
    }

    private ResourceDetail ingressDetail(String namespace, String name) {
        Ingress ingress = kubernetesService.getIngress(namespace, name);
        if (ingress == null) {
            return missingDetail("Ingress", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Hosts", ingress.getSpec() != null && ingress.getSpec().getRules() != null
                        ? ingress.getSpec().getRules().stream().map(rule -> value(rule.getHost())).collect(Collectors.joining(", ")) : "-",
                "Class", ingress.getSpec() != null ? value(ingress.getSpec().getIngressClassName()) : "-",
                "TLS", ingress.getSpec() != null && ingress.getSpec().getTls() != null ? value(ingress.getSpec().getTls().size()) : "-",
                "Age", age(ingress.getMetadata() != null ? ingress.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "Ingress: " + name,
                "Ingress",
                name,
                namespace,
                ingress.getStatus() != null && ingress.getStatus().getLoadBalancer() != null
                        ? ingress.getStatus().getLoadBalancer().getIngress().toString() : "-",
                age(ingress.getMetadata() != null ? ingress.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(ingress.getMetadata() != null ? ingress.getMetadata().getLabels() : null),
                labelList(ingress.getMetadata() != null ? ingress.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail hpaDetail(String namespace, String name) {
        HorizontalPodAutoscaler hpa = kubernetesService.getHorizontalPodAutoscaler(namespace, name);
        if (hpa == null) {
            return missingDetail("HPA", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Current Replicas", hpa.getStatus() != null ? value(hpa.getStatus().getCurrentReplicas()) : "-",
                "Desired Replicas", hpa.getStatus() != null ? value(hpa.getStatus().getDesiredReplicas()) : "-",
                "Max Replicas", hpa.getSpec() != null ? value(hpa.getSpec().getMaxReplicas()) : "-",
                "Age", age(hpa.getMetadata() != null ? hpa.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "HPA: " + name,
                "HorizontalPodAutoscaler",
                name,
                namespace,
                hpa.getStatus() != null ? value(hpa.getStatus().getCurrentReplicas()) + " current" : "-",
                age(hpa.getMetadata() != null ? hpa.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(hpa.getMetadata() != null ? hpa.getMetadata().getLabels() : null),
                labelList(hpa.getMetadata() != null ? hpa.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail serviceAccountDetail(String namespace, String name) {
        ServiceAccount sa = kubernetesService.getServiceAccount(namespace, name);
        if (sa == null) {
            return missingDetail("ServiceAccount", namespace, name);
        }
        Map<String, String> overview = Map.of(
                "Secrets", sa.getSecrets() != null ? value(sa.getSecrets().size()) : "0",
                "Automount Token", sa.getAutomountServiceAccountToken() != null && sa.getAutomountServiceAccountToken() ? "Yes" : "No",
                "Age", age(sa.getMetadata() != null ? sa.getMetadata().getCreationTimestamp() : null)
        );
        List<DetailSection> sections = List.of(new DetailSection("Overview", overview));

        return new ResourceDetail(
                "ServiceAccount: " + name,
                "ServiceAccount",
                name,
                namespace,
                "Ready",
                age(sa.getMetadata() != null ? sa.getMetadata().getCreationTimestamp() : null),
                sections,
                labelList(sa.getMetadata() != null ? sa.getMetadata().getLabels() : null),
                labelList(sa.getMetadata() != null ? sa.getMetadata().getAnnotations() : null),
                List.of()
        );
    }

    private ResourceDetail missingDetail(String kind, String namespace, String name) {
        List<DetailSection> sections = List.of(new DetailSection("Overview", Map.of(
                "Namespace", namespace,
                "Name", name,
                "Status", "Not found"
        )));
        return new ResourceDetail(kind + ": " + name, kind, name, namespace, "Not found", "-", sections, List.of(), List.of(), List.of());
    }

    private List<String> labelList(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        return labels.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toList();
    }

    private List<ContainerInfo> containersFromTemplate(Deployment dep) {
        if (dep == null || dep.getSpec() == null || dep.getSpec().getTemplate() == null
                || dep.getSpec().getTemplate().getSpec() == null) {
            return List.of();
        }
        return containersOf(dep.getSpec().getTemplate().getSpec().getContainers());
    }

    private List<ContainerInfo> containersOf(List<Container> containers) {
        if (containers == null) {
            return List.of();
        }
        return containers.stream()
                .map(container -> new ContainerInfo(
                        value(container.getName()),
                        value(container.getImage()),
                        container.getResources() != null && container.getResources().getLimits() != null
                                ? "Limits set" : "-",
                        container.getPorts() != null ? container.getPorts().stream()
                                .map(this::portLabel)
                                .collect(Collectors.joining(", ")) : "-"
                ))
                .toList();
    }

    private String portLabel(ContainerPort port) {
        if (port == null) {
            return "-";
        }
        return (port.getName() != null ? port.getName() + " " : "")
                + value(port.getContainerPort())
                + "/"
                + value(port.getProtocol());
    }

    private record WorkloadCard(String label, int total, int ready) {
        public int pending() {
            return Math.max(total - ready, 0);
        }
    }

    private record ResourceDataset(String title, List<TableColumn> columns, List<ResourceRow> rows,
                                   Map<String, WorkloadCard> summary) {
    }

    private record ResourceRow(String name, String namespace, String status, String details, String age, String href) {
    }

    private record TableColumn(String label) {
    }

    public record NavItem(String label, String icon, String href, boolean active) {
    }

    private record ResourceDetail(String title, String kind, String name, String namespace, String status, String age,
                                  List<DetailSection> sections, List<String> labels, List<String> annotations,
                                  List<ContainerInfo> containers) {
    }

    private record DetailSection(String title, Map<String, String> items) {
    }

    private record ContainerInfo(String name, String image, String resources, String ports) {
    }
}
