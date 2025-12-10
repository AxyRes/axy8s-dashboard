package com.dofe.axy8s.k8s;

import com.dofe.axy8s.security.AppUserDetails;
import com.dofe.axy8s.user.Role;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
