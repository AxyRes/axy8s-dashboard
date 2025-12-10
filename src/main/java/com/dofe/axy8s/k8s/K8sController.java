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

    @GetMapping("/namespaces/{namespace}/pods")
    public List<Pod> getPods(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        if (!user.canAccessNamespace(namespace)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not allowed to access namespace: " + namespace);
        }
        return kubernetesService.listPods(namespace);
    }

    @GetMapping("/namespaces/{namespace}/pods/{pod}/logs")
    public String getPodLogs(
            @PathVariable String namespace,
            @PathVariable String pod,
            @RequestParam(required = false) String container,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        if (!user.canAccessNamespace(namespace)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User is not allowed to access namespace: " + namespace);
        }

        return kubernetesService.getPodLogs(namespace, pod, container);
    }

    // NEW: delete pod (coi như restart pod nếu thuộc deployment)

    @DeleteMapping("/namespaces/{namespace}/pods/{pod}")
    public void deletePod(
            @PathVariable String namespace,
            @PathVariable String pod,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        // chỉ SUPER_ADMIN & ADMIN được xoá pod
        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete pods"
            );
        }

        kubernetesService.deletePod(namespace, pod);
    }

    // ========== DEPLOYMENTS (đã bổ sung trước) ==========

    @GetMapping("/namespaces/{namespace}/deployments")
    public List<Deployment> getDeployments(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);
        return kubernetesService.listDeployments(namespace);
    }

    @GetMapping("/namespaces/{namespace}/deployments/{name}")
    public Deployment getDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update deployments"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete deployments"
            );
        }

        kubernetesService.deleteDeployment(namespace, name);
    }

    // NEW: restart deployment (rollout restart)

    @PostMapping("/namespaces/{namespace}/deployments/{name}/restart")
    public void restartDeployment(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can restart deployments"
            );
        }

        try {
            kubernetesService.restartDeployment(namespace, name);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    // ========== SERVICES ==========

    @GetMapping("/namespaces/{namespace}/services")
    public List<Service> getServices(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);
        return kubernetesService.listServices(namespace);
    }

    @GetMapping("/namespaces/{namespace}/services/{name}")
    public Service getService(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update services"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete services"
            );
        }

        kubernetesService.deleteService(namespace, name);
    }

     // ========== CONFIGMAPS ==========

    @GetMapping("/namespaces/{namespace}/configmaps")
    public java.util.List<ConfigMap> getConfigMaps(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);
        return kubernetesService.listConfigMaps(namespace);
    }

    @GetMapping("/namespaces/{namespace}/configmaps/{name}")
    public ConfigMap getConfigMap(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update configmaps"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete configmaps"
            );
        }

        kubernetesService.deleteConfigMap(namespace, name);
    }

    // ========== SECRETS ==========

    @GetMapping("/namespaces/{namespace}/secrets")
    public java.util.List<Secret> getSecrets(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can list secrets"
            );
        }

        return kubernetesService.listSecrets(namespace);
    }

    @GetMapping("/namespaces/{namespace}/secrets/{name}")
    public Secret getSecret(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can get secrets"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update secrets"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete secrets"
            );
        }

        kubernetesService.deleteSecret(namespace, name);
    }

    // ========== CRONJOBS ==========

    @GetMapping("/namespaces/{namespace}/cronjobs")
    public java.util.List<CronJob> getCronJobs(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);
        // Cho mọi role có quyền namespace đều xem được
        return kubernetesService.listCronJobs(namespace);
    }

    @GetMapping("/namespaces/{namespace}/cronjobs/{name}")
    public CronJob getCronJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        // CRUD CronJob: chỉ ADMIN + SUPER_ADMIN
        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update cronjobs"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete cronjobs"
            );
        }

        kubernetesService.deleteCronJob(namespace, name);
    }

     // ========== JOBS ==========

    @GetMapping("/namespaces/{namespace}/jobs")
    public java.util.List<Job> getJobs(
            @PathVariable String namespace,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);
        return kubernetesService.listJobs(namespace);
    }

    @GetMapping("/namespaces/{namespace}/jobs/{name}")
    public Job getJob(
            @PathVariable String namespace,
            @PathVariable String name,
            Authentication authentication
    ) {
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can create or update jobs"
            );
        }

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
        AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
        ensureNamespaceAccess(user, namespace);

        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can delete jobs"
            );
        }

        kubernetesService.deleteJob(namespace, name);
    }

    // ========== Helper ==========

    private void ensureNamespaceAccess(AppUserDetails user, String namespace) {
        if (!user.canAccessNamespace(namespace)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not allowed to access namespace: " + namespace
            );
        }
    }
}
