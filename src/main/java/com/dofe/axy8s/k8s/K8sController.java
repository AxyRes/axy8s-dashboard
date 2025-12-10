package com.dofe.axy8s.k8s;

import com.dofe.axy8s.security.AppUserDetails;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
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
}
