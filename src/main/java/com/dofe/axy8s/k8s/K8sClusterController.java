package com.dofe.axy8s.k8s;

import com.dofe.axy8s.security.AppUserDetails;
import com.dofe.axy8s.user.Role;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/k8s/cluster")
public class K8sClusterController {

    private final KubernetesService kubernetesService;

    public K8sClusterController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    // ========== Helper cho cluster-scope ==========

    private AppUserDetails currentUser(Authentication authentication) {
        return (AppUserDetails) authentication.getPrincipal();
    }

    // hiện tại: cả READ/WRITE đều chỉ cho SUPER_ADMIN & ADMIN
    private void ensureClusterReadAccess(Authentication authentication) {
        AppUserDetails user = currentUser(authentication);
        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can view cluster-scoped resources"
            );
        }
    }

    private void ensureClusterWriteAccess(Authentication authentication) {
        AppUserDetails user = currentUser(authentication);
        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only SUPER_ADMIN or ADMIN can modify cluster-scoped resources"
            );
        }

        // Nếu sau này muốn chỉ SUPER_ADMIN được write thì sửa thành:
        // if (user.getRole() != Role.SUPER_ADMIN) { ... }
    }

    // ========== PERSISTENTVOLUMES (CLUSTER SCOPE) ==========

    @GetMapping("/persistentvolumes")
    public List<PersistentVolume> getPersistentVolumes(Authentication authentication) {
        ensureClusterReadAccess(authentication);
        return kubernetesService.listPersistentVolumes();
    }

    @GetMapping("/persistentvolumes/{name}")
    public PersistentVolume getPersistentVolume(
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureClusterReadAccess(authentication);

        PersistentVolume pv = kubernetesService.getPersistentVolume(name);
        if (pv == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "PersistentVolume not found: " + name
            );
        }
        return pv;
    }

    @PostMapping("/persistentvolumes")
    public PersistentVolume createOrUpdatePersistentVolume(
            @RequestBody PersistentVolume pv,
            Authentication authentication
    ) {
        ensureClusterWriteAccess(authentication);

        try {
            return kubernetesService.createOrUpdatePersistentVolume(pv);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/persistentvolumes/{name}")
    public void deletePersistentVolume(
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureClusterWriteAccess(authentication);
        kubernetesService.deletePersistentVolume(name);
    }

    // ========== STORAGECLASSES (CLUSTER SCOPE) ==========

    @GetMapping("/storageclasses")
    public List<StorageClass> getStorageClasses(Authentication authentication) {
        ensureClusterReadAccess(authentication);
        return kubernetesService.listStorageClasses();
    }

    @GetMapping("/storageclasses/{name}")
    public StorageClass getStorageClass(
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureClusterReadAccess(authentication);

        StorageClass sc = kubernetesService.getStorageClass(name);
        if (sc == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "StorageClass not found: " + name
            );
        }
        return sc;
    }

    @PostMapping("/storageclasses")
    public StorageClass createOrUpdateStorageClass(
            @RequestBody StorageClass sc,
            Authentication authentication
    ) {
        ensureClusterWriteAccess(authentication);

        try {
            return kubernetesService.createOrUpdateStorageClass(sc);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/storageclasses/{name}")
    public void deleteStorageClass(
            @PathVariable String name,
            Authentication authentication
    ) {
        ensureClusterWriteAccess(authentication);
        kubernetesService.deleteStorageClass(name);
    }
}
