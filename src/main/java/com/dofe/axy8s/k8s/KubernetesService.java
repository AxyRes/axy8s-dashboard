package com.dofe.axy8s.k8s;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KubernetesService {

    private final KubernetesClient client;

    public KubernetesService(KubernetesClient client) {
        this.client = client;
    }

    // ========== NAMESPACES ==========

    public List<Namespace> listNamespaces() {
        return client.namespaces()
                .list()
                .getItems();
    }

    // ========== PODS & LOGS ==========

    public List<Pod> listPods(String namespace) {
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public String getPodLogs(String namespace, String podName, String containerName) {
        if (containerName != null && !containerName.isBlank()) {
            return client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(containerName)
                    .getLog();
        }

        return client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .getLog();
    }

    public void deletePod(String namespace, String podName) {
        client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .delete();
    }

    // ========== DEPLOYMENTS ==========

    public List<Deployment> listDeployments(String namespace) {
        return client.apps()
                .deployments()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public Deployment getDeployment(String namespace, String name) {
        return client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Deployment createOrUpdateDeployment(String namespace, Deployment deployment) {
        if (deployment.getMetadata() != null) {
            String bodyNs = deployment.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                deployment.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "Deployment namespace in body does not match path namespace"
                );
            }
        }

        return client.apps()
                .deployments()
                .inNamespace(namespace)
                .resource(deployment)
                .createOrReplace();
    }

    public void deleteDeployment(String namespace, String name) {
        client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    public void restartDeployment(String namespace, String name) {
        Deployment deployment = client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

        if (deployment == null) {
            throw new IllegalArgumentException("Deployment not found: " + name);
        }

        // Giống kubectl rollout restart: update annotation để trigger rollout
        var templateMeta = deployment.getSpec().getTemplate().getMetadata();
        if (templateMeta.getAnnotations() == null) {
            templateMeta.setAnnotations(new HashMap<>());
        }
        Map<String, String> ann = templateMeta.getAnnotations();
        ann.put("kubectl.kubernetes.io/restartedAt", Instant.now().toString());

        client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .patch(deployment);
    }

    // ========== SERVICES ==========

    // Lưu ý: dùng FQN io.fabric8...Service để không trùng với @Service của Spring

    public List<io.fabric8.kubernetes.api.model.Service> listServices(String namespace) {
        return client.services()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public io.fabric8.kubernetes.api.model.Service getService(String namespace, String name) {
        return client.services()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public io.fabric8.kubernetes.api.model.Service createOrUpdateService(
            String namespace,
            io.fabric8.kubernetes.api.model.Service service
    ) {
        if (service.getMetadata() != null) {
            String bodyNs = service.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                service.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "Service namespace in body does not match path namespace"
                );
            }
        }

        return client.services()
                .inNamespace(namespace)
                .resource(service)
                .createOrReplace();
    }

    public void deleteService(String namespace, String name) {
        client.services()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }
}
