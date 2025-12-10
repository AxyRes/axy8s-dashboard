package com.dofe.axy8s.k8s;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
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

    // ========== CONFIGMAPS ==========

    public java.util.List<ConfigMap> listConfigMaps(String namespace) {
        return client.configMaps()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public ConfigMap getConfigMap(String namespace, String name) {
        return client.configMaps()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public ConfigMap createOrUpdateConfigMap(String namespace, ConfigMap configMap) {
        if (configMap.getMetadata() != null) {
            String bodyNs = configMap.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                configMap.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "ConfigMap namespace in body does not match path namespace"
                );
            }
        }

        return client.configMaps()
                .inNamespace(namespace)
                .resource(configMap)
                .createOrReplace();
    }

    public void deleteConfigMap(String namespace, String name) {
        client.configMaps()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    // ========== SECRETS ==========

    public java.util.List<Secret> listSecrets(String namespace) {
        return client.secrets()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public Secret getSecret(String namespace, String name) {
        return client.secrets()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Secret createOrUpdateSecret(String namespace, Secret secret) {
        if (secret.getMetadata() != null) {
            String bodyNs = secret.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                secret.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "Secret namespace in body does not match path namespace"
                );
            }
        }

        return client.secrets()
                .inNamespace(namespace)
                .resource(secret)
                .createOrReplace();
    }

    public void deleteSecret(String namespace, String name) {
        client.secrets()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== CRONJOBS ==========

    public java.util.List<CronJob> listCronJobs(String namespace) {
        return client.batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public CronJob getCronJob(String namespace, String name) {
        return client.batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public CronJob createOrUpdateCronJob(String namespace, CronJob cronJob) {
        if (cronJob.getMetadata() != null) {
            String bodyNs = cronJob.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                cronJob.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "CronJob namespace in body does not match path namespace"
                );
            }
        }

        return client.batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .resource(cronJob)
                .createOrReplace();
    }

    public void deleteCronJob(String namespace, String name) {
        client.batch()
                .v1()
                .cronjobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

     // ========== JOBS ==========

    public java.util.List<Job> listJobs(String namespace) {
        return client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public Job getJob(String namespace, String name) {
        return client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Job createOrUpdateJob(String namespace, Job job) {
        if (job.getMetadata() != null) {
            String bodyNs = job.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                job.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "Job namespace in body does not match path namespace"
                );
            }
        }

        return client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .resource(job)
                .createOrReplace();
    }

    public void deleteJob(String namespace, String name) {
        client.batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    // ========== REPLICASETS ==========

    public java.util.List<ReplicaSet> listReplicaSets(String namespace) {
        return client.apps()
                .replicaSets()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public ReplicaSet getReplicaSet(String namespace, String name) {
        return client.apps()
                .replicaSets()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public ReplicaSet createOrUpdateReplicaSet(String namespace, ReplicaSet replicaSet) {
        if (replicaSet.getMetadata() != null) {
            String bodyNs = replicaSet.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                replicaSet.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "ReplicaSet namespace in body does not match path namespace"
                );
            }
        }

        return client.apps()
                .replicaSets()
                .inNamespace(namespace)
                .resource(replicaSet)
                .createOrReplace();
    }

    public void deleteReplicaSet(String namespace, String name) {
        client.apps()
                .replicaSets()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    // ========== STATEFULSETS ==========

    public java.util.List<StatefulSet> listStatefulSets(String namespace) {
        return client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public StatefulSet getStatefulSet(String namespace, String name) {
        return client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public StatefulSet createOrUpdateStatefulSet(String namespace, StatefulSet statefulSet) {
        if (statefulSet.getMetadata() != null) {
            String bodyNs = statefulSet.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                statefulSet.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "StatefulSet namespace in body does not match path namespace"
                );
            }
        }

        return client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .resource(statefulSet)
                .createOrReplace();
    }

    public void deleteStatefulSet(String namespace, String name) {
        client.apps()
                .statefulSets()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }
}
