package com.dofe.axy8s.k8s;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KubernetesService {

    private final KubernetesClient client;

    public KubernetesService(KubernetesClient client) {
        this.client = client;
    }

    public List<Namespace> listNamespaces() {
        return client.namespaces().list().getItems();
    }

    public List<Pod> listPods(String namespace) {
        return client.pods().inNamespace(namespace).list().getItems();
    }

    public String getPodLogs(String namespace, String podName, String container) {
        if (container != null && !container.isEmpty()) {
            return client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(container)
                    .getLog();
        } else {
            return client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .getLog();
        }
    }
}
