package com.dofe.axy8s.k8s;

import com.dofe.axy8s.security.AppUserDetails;
import com.dofe.axy8s.user.Role;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudget;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/k8s")
public class K8sController {

    private final KubernetesService kubernetesService;

    public K8sController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    // ================== NAMESPACES ==================

    @GetMapping("/namespaces")
    public List<Namespace> getNamespaces(Authentication authentication) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        return kubernetesService.listNamespaces().stream()
                .filter(ns -> {
                    String name = ns.getMetadata() != null ? ns.getMetadata().getName() : null;
                    return user.canAccessNamespace(name);
                })
                .toList();
    }

    // ================== PODS ==================

    @GetMapping("/namespaces/{namespace}/pods")
    public List<Pod> getPods(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listPods(namespace);
    }

    @GetMapping("/namespaces/{namespace}/pods/{pod}/logs")
    public String getPodLogs(
            @PathVariable String namespace,
            @PathVariable String pod,
            @RequestParam(required = false) String container,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.getPodLogs(namespace, pod, container);
    }

    @DeleteMapping("/namespaces/{namespace}/pods/{pod}")
    public void deletePod(
            @PathVariable String namespace,
            @PathVariable String pod,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deletePod(namespace, pod);
    }

    // ================== DEPLOYMENTS ==================

    @GetMapping("/namespaces/{namespace}/deployments")
    public List<Deployment> getDeployments(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listDeployments(namespace);
    }

    @GetMapping("/namespaces/{namespace}/deployments/{name}")
    public Deployment getDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        Deployment deployment = kubernetesService.getDeployment(namespace, name);
        if (deployment == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Deployment not found: " + name
            );
        }
        return deployment;
    }

    @PostMapping("/namespaces/{namespace}/deployments")
    public Deployment createOrUpdateDeployment(
            @PathVariable String namespace,
            @RequestBody Deployment deployment,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateDeployment(namespace, deployment);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/deployments/{name}")
    public void deleteDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteDeployment(namespace, name);
    }

    @PostMapping("/namespaces/{namespace}/deployments/{name}/restart")
    public void restartDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            kubernetesService.restartDeployment(namespace, name);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ================== SERVICES ==================

    @GetMapping("/namespaces/{namespace}/services")
    public List<Service> getServices(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listServices(namespace);
    }

    @GetMapping("/namespaces/{namespace}/services/{name}")
    public Service getService(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        Service service = kubernetesService.getService(namespace, name);
        if (service == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Service not found: " + name
            );
        }
        return service;
    }

    @PostMapping("/namespaces/{namespace}/services")
    public Service createOrUpdateService(
            @PathVariable String namespace,
            @RequestBody Service service,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateService(namespace, service);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/services/{name}")
    public void deleteService(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteService(namespace, name);
    }

    // ================== CONFIGMAPS ==================

    @GetMapping("/namespaces/{namespace}/configmaps")
    public List<ConfigMap> getConfigMaps(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listConfigMaps(namespace);
    }

    @GetMapping("/namespaces/{namespace}/configmaps/{name}")
    public ConfigMap getConfigMap(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        ConfigMap cm = kubernetesService.getConfigMap(namespace, name);
        if (cm == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "ConfigMap not found: " + name
            );
        }
        return cm;
    }

    @PostMapping("/namespaces/{namespace}/configmaps")
    public ConfigMap createOrUpdateConfigMap(
            @PathVariable String namespace,
            @RequestBody ConfigMap configMap,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateConfigMap(namespace, configMap);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/configmaps/{name}")
    public void deleteConfigMap(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteConfigMap(namespace, name);
    }

    // ================== SECRETS ==================

    @GetMapping("/namespaces/{namespace}/secrets")
    public List<Secret> getSecrets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listSecrets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/secrets/{name}")
    public Secret getSecret(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        Secret secret = kubernetesService.getSecret(namespace, name);
        if (secret == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Secret not found: " + name
            );
        }
        return secret;
    }

    @PostMapping("/namespaces/{namespace}/secrets")
    public Secret createOrUpdateSecret(
            @PathVariable String namespace,
            @RequestBody Secret secret,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateSecret(namespace, secret);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/secrets/{name}")
    public void deleteSecret(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteSecret(namespace, name);
    }

    // ================== CRONJOBS ==================

    @GetMapping("/namespaces/{namespace}/cronjobs")
    public List<CronJob> getCronJobs(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listCronJobs(namespace);
    }

    @GetMapping("/namespaces/{namespace}/cronjobs/{name}")
    public CronJob getCronJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        CronJob cronJob = kubernetesService.getCronJob(namespace, name);
        if (cronJob == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "CronJob not found: " + name
            );
        }
        return cronJob;
    }

    @PostMapping("/namespaces/{namespace}/cronjobs")
    public CronJob createOrUpdateCronJob(
            @PathVariable String namespace,
            @RequestBody CronJob cronJob,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateCronJob(namespace, cronJob);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/cronjobs/{name}")
    public void deleteCronJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteCronJob(namespace, name);
    }

    // ================== JOBS ==================

    @GetMapping("/namespaces/{namespace}/jobs")
    public List<Job> getJobs(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listJobs(namespace);
    }

    @GetMapping("/namespaces/{namespace}/jobs/{name}")
    public Job getJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        Job job = kubernetesService.getJob(namespace, name);
        if (job == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Job not found: " + name
            );
        }
        return job;
    }

    @PostMapping("/namespaces/{namespace}/jobs")
    public Job createOrUpdateJob(
            @PathVariable String namespace,
            @RequestBody Job job,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateJob(namespace, job);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/jobs/{name}")
    public void deleteJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteJob(namespace, name);
    }

    // ================== REPLICASETS ==================

    @GetMapping("/namespaces/{namespace}/replicasets")
    public List<ReplicaSet> getReplicaSets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listReplicaSets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/replicasets/{name}")
    public ReplicaSet getReplicaSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        ReplicaSet rs = kubernetesService.getReplicaSet(namespace, name);
        if (rs == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "ReplicaSet not found: " + name
            );
        }
        return rs;
    }

    @PostMapping("/namespaces/{namespace}/replicasets")
    public ReplicaSet createOrUpdateReplicaSet(
            @PathVariable String namespace,
            @RequestBody ReplicaSet replicaSet,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateReplicaSet(namespace, replicaSet);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/replicasets/{name}")
    public void deleteReplicaSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteReplicaSet(namespace, name);
    }

    // ================== STATEFULSETS ==================

    @GetMapping("/namespaces/{namespace}/statefulsets")
    public List<StatefulSet> getStatefulSets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listStatefulSets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/statefulsets/{name}")
    public StatefulSet getStatefulSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        StatefulSet ss = kubernetesService.getStatefulSet(namespace, name);
        if (ss == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "StatefulSet not found: " + name
            );
        }
        return ss;
    }

    @PostMapping("/namespaces/{namespace}/statefulsets")
    public StatefulSet createOrUpdateStatefulSet(
            @PathVariable String namespace,
            @RequestBody StatefulSet statefulSet,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateStatefulSet(namespace, statefulSet);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/statefulsets/{name}")
    public void deleteStatefulSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteStatefulSet(namespace, name);
    }

    // ================== DAEMONSETS ==================

    @GetMapping("/namespaces/{namespace}/daemonsets")
    public List<DaemonSet> getDaemonSets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listDaemonSets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/daemonsets/{name}")
    public DaemonSet getDaemonSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        DaemonSet ds = kubernetesService.getDaemonSet(namespace, name);
        if (ds == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "DaemonSet not found: " + name
            );
        }
        return ds;
    }

    @PostMapping("/namespaces/{namespace}/daemonsets")
    public DaemonSet createOrUpdateDaemonSet(
            @PathVariable String namespace,
            @RequestBody DaemonSet daemonSet,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateDaemonSet(namespace, daemonSet);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/daemonsets/{name}")
    public void deleteDaemonSet(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteDaemonSet(namespace, name);
    }

    // ================== PVC ==================

    @GetMapping("/namespaces/{namespace}/persistentvolumeclaims")
    public List<PersistentVolumeClaim> getPersistentVolumeClaims(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listPersistentVolumeClaims(namespace);
    }

    @GetMapping("/namespaces/{namespace}/persistentvolumeclaims/{name}")
    public PersistentVolumeClaim getPersistentVolumeClaim(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        PersistentVolumeClaim pvc = kubernetesService.getPersistentVolumeClaim(namespace, name);
        if (pvc == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "PVC not found: " + name
            );
        }
        return pvc;
    }

    @PostMapping("/namespaces/{namespace}/persistentvolumeclaims")
    public PersistentVolumeClaim createOrUpdatePersistentVolumeClaim(
            @PathVariable String namespace,
            @RequestBody PersistentVolumeClaim pvc,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdatePersistentVolumeClaim(namespace, pvc);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/persistentvolumeclaims/{name}")
    public void deletePersistentVolumeClaim(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deletePersistentVolumeClaim(namespace, name);
    }

    // ================== INGRESS ==================

    @GetMapping("/namespaces/{namespace}/ingresses")
    public List<Ingress> getIngresses(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listIngresses(namespace);
    }

    @GetMapping("/namespaces/{namespace}/ingresses/{name}")
    public Ingress getIngress(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        Ingress ing = kubernetesService.getIngress(namespace, name);
        if (ing == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Ingress not found: " + name
            );
        }
        return ing;
    }

    @PostMapping("/namespaces/{namespace}/ingresses")
    public Ingress createOrUpdateIngress(
            @PathVariable String namespace,
            @RequestBody Ingress ingress,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateIngress(namespace, ingress);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/ingresses/{name}")
    public void deleteIngress(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteIngress(namespace, name);
    }

    // ================== SERVICEACCOUNTS ==================

    @GetMapping("/namespaces/{namespace}/serviceaccounts")
    public List<ServiceAccount> getServiceAccounts(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listServiceAccounts(namespace);
    }

    @GetMapping("/namespaces/{namespace}/serviceaccounts/{name}")
    public ServiceAccount getServiceAccount(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        ServiceAccount sa = kubernetesService.getServiceAccount(namespace, name);
        if (sa == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "ServiceAccount not found: " + name
            );
        }
        return sa;
    }

    @PostMapping("/namespaces/{namespace}/serviceaccounts")
    public ServiceAccount createOrUpdateServiceAccount(
            @PathVariable String namespace,
            @RequestBody ServiceAccount sa,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateServiceAccount(namespace, sa);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/serviceaccounts/{name}")
    public void deleteServiceAccount(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteServiceAccount(namespace, name);
    }

    // ================== HPA ==================

    @GetMapping("/namespaces/{namespace}/hpas")
    public List<HorizontalPodAutoscaler> getHorizontalPodAutoscalers(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listHorizontalPodAutoscalers(namespace);
    }

    @GetMapping("/namespaces/{namespace}/hpas/{name}")
    public HorizontalPodAutoscaler getHorizontalPodAutoscaler(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        HorizontalPodAutoscaler hpa = kubernetesService.getHorizontalPodAutoscaler(namespace, name);
        if (hpa == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "HPA not found: " + name
            );
        }
        return hpa;
    }

    @PostMapping("/namespaces/{namespace}/hpas")
    public HorizontalPodAutoscaler createOrUpdateHorizontalPodAutoscaler(
            @PathVariable String namespace,
            @RequestBody HorizontalPodAutoscaler hpa,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateHorizontalPodAutoscaler(namespace, hpa);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/hpas/{name}")
    public void deleteHorizontalPodAutoscaler(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteHorizontalPodAutoscaler(namespace, name);
    }

    // ================== NETWORKPOLICIES ==================

    @GetMapping("/namespaces/{namespace}/networkpolicies")
    public List<NetworkPolicy> getNetworkPolicies(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listNetworkPolicies(namespace);
    }

    @GetMapping("/namespaces/{namespace}/networkpolicies/{name}")
    public NetworkPolicy getNetworkPolicy(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        NetworkPolicy np = kubernetesService.getNetworkPolicy(namespace, name);
        if (np == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "NetworkPolicy not found: " + name
            );
        }
        return np;
    }

    @PostMapping("/namespaces/{namespace}/networkpolicies")
    public NetworkPolicy createOrUpdateNetworkPolicy(
            @PathVariable String namespace,
            @RequestBody NetworkPolicy np,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateNetworkPolicy(namespace, np);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/networkpolicies/{name}")
    public void deleteNetworkPolicy(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteNetworkPolicy(namespace, name);
    }

    // ================== PDB ==================

    @GetMapping("/namespaces/{namespace}/poddisruptionbudgets")
    public List<PodDisruptionBudget> getPodDisruptionBudgets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listPodDisruptionBudgets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/poddisruptionbudgets/{name}")
    public PodDisruptionBudget getPodDisruptionBudget(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        PodDisruptionBudget pdb = kubernetesService.getPodDisruptionBudget(namespace, name);
        if (pdb == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "PDB not found: " + name
            );
        }
        return pdb;
    }

    @PostMapping("/namespaces/{namespace}/poddisruptionbudgets")
    public PodDisruptionBudget createOrUpdatePodDisruptionBudget(
            @PathVariable String namespace,
            @RequestBody PodDisruptionBudget pdb,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdatePodDisruptionBudget(namespace, pdb);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/poddisruptionbudgets/{name}")
    public void deletePodDisruptionBudget(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deletePodDisruptionBudget(namespace, name);
    }

    // ================== RESOURCE QUOTAS ==================

    @GetMapping("/namespaces/{namespace}/resourcequotas")
    public List<ResourceQuota> getResourceQuotas(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listResourceQuotas(namespace);
    }

    @GetMapping("/namespaces/{namespace}/resourcequotas/{name}")
    public ResourceQuota getResourceQuota(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        ResourceQuota rq = kubernetesService.getResourceQuota(namespace, name);
        if (rq == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "ResourceQuota not found: " + name
            );
        }
        return rq;
    }

    @PostMapping("/namespaces/{namespace}/resourcequotas")
    public ResourceQuota createOrUpdateResourceQuota(
            @PathVariable String namespace,
            @RequestBody ResourceQuota rq,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateResourceQuota(namespace, rq);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/resourcequotas/{name}")
    public void deleteResourceQuota(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteResourceQuota(namespace, name);
    }

    // ================== LIMIT RANGES ==================

    @GetMapping("/namespaces/{namespace}/limitranges")
    public List<LimitRange> getLimitRanges(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);
        return kubernetesService.listLimitRanges(namespace);
    }

    @GetMapping("/namespaces/{namespace}/limitranges/{name}")
    public LimitRange getLimitRange(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceReadAccess(authentication, namespace);

        LimitRange lr = kubernetesService.getLimitRange(namespace, name);
        if (lr == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "LimitRange not found: " + name
            );
        }
        return lr;
    }

    @PostMapping("/namespaces/{namespace}/limitranges")
    public LimitRange createOrUpdateLimitRange(
            @PathVariable String namespace,
            @RequestBody LimitRange lr,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        try {
            return kubernetesService.createOrUpdateLimitRange(namespace, lr);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/namespaces/{namespace}/limitranges/{name}")
    public void deleteLimitRange(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);
        kubernetesService.deleteLimitRange(namespace, name);
    }

    @PostMapping("/namespaces/{namespace}/pods/{pod}/exec")
    public String execInPod(
            @PathVariable String namespace,
            @PathVariable String pod,
            @RequestParam(required = false) String container,
            @RequestParam("command") String command,
            Authentication authentication
    ) {
        ensureNamespaceWriteAccess(authentication, namespace);

        if (command == null || command.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "command must not be empty"
            );
        }

        return kubernetesService.execInPod(namespace, pod, container, command);
    }

    // ================== Helpers ==================

    private void ensureNamespaceReadAccess(Authentication authentication, String namespace) {
        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        if (!currentUser.canAccessNamespace(namespace)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access namespace: " + namespace
            );
        }
    }

    private void ensureNamespaceWriteAccess(Authentication authentication, String namespace) {
        AppUserDetails currentUser = (AppUserDetails) authentication.getPrincipal();

        if (!currentUser.canAccessNamespace(namespace)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access namespace: " + namespace
            );
        }

        if (currentUser.getRole() == Role.VIEWER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "VIEWER is read-only for namespace: " + namespace
            );
        }
    }
}
