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
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudget;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.storage.StorageClass;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.stereotype.Service;

import io.fabric8.kubernetes.client.dsl.ExecWatch;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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

    public Namespace getNamespace(String name) {
        return client.namespaces()
                .withName(name)
                .get();
    }

    public Namespace createOrUpdateNamespace(Namespace namespace) {
        if (namespace == null || namespace.getMetadata() == null ||
                namespace.getMetadata().getName() == null ||
                namespace.getMetadata().getName().isBlank()) {
            throw new IllegalArgumentException("Namespace metadata.name is required");
        }

        return client.namespaces()
                .resource(namespace)
                .createOrReplace();
    }

    public void deleteNamespace(String name) {
        client.namespaces()
                .withName(name)
                .delete();
    }

    // ========== PODS & LOGS ==========

    public List<Pod> listPods(String namespace) {
        return client.pods()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public Pod getPod(String namespace, String name) {
        return client.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Pod getPod(String namespace, String name) {
        return client.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();
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

        // ========== DAEMONSETS ==========

    public java.util.List<DaemonSet> listDaemonSets(String namespace) {
        return client.apps()
                .daemonSets()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public DaemonSet getDaemonSet(String namespace, String name) {
        return client.apps()
                .daemonSets()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public DaemonSet createOrUpdateDaemonSet(String namespace, DaemonSet daemonSet) {
        if (daemonSet.getMetadata() != null) {
            String bodyNs = daemonSet.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                daemonSet.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "DaemonSet namespace in body does not match path namespace"
                );
            }
        }

        return client.apps()
                .daemonSets()
                .inNamespace(namespace)
                .resource(daemonSet)
                .createOrReplace();
    }

    public void deleteDaemonSet(String namespace, String name) {
        client.apps()
                .daemonSets()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== PERSISTENTVOLUMECLAIMS (PVC) ==========

    public java.util.List<PersistentVolumeClaim> listPersistentVolumeClaims(String namespace) {
        return client.persistentVolumeClaims()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public PersistentVolumeClaim getPersistentVolumeClaim(String namespace, String name) {
        return client.persistentVolumeClaims()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public PersistentVolumeClaim createOrUpdatePersistentVolumeClaim(
            String namespace,
            PersistentVolumeClaim pvc
    ) {
        if (pvc.getMetadata() != null) {
            String bodyNs = pvc.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                pvc.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "PVC namespace in body does not match path namespace"
                );
            }
        }

        return client.persistentVolumeClaims()
                .inNamespace(namespace)
                .resource(pvc)
                .createOrReplace();
    }

    public void deletePersistentVolumeClaim(String namespace, String name) {
        client.persistentVolumeClaims()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== INGRESS ==========

    public java.util.List<Ingress> listIngresses(String namespace) {
        return client.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public Ingress getIngress(String namespace, String name) {
        return client.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Ingress createOrUpdateIngress(String namespace, Ingress ingress) {
        if (ingress.getMetadata() != null) {
            String bodyNs = ingress.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                ingress.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "Ingress namespace in body does not match path namespace"
                );
            }
        }

        return client.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .resource(ingress)
                .createOrReplace();
    }

    public void deleteIngress(String namespace, String name) {
        client.network()
                .v1()
                .ingresses()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== SERVICEACCOUNTS ==========

    public java.util.List<ServiceAccount> listServiceAccounts(String namespace) {
        return client.serviceAccounts()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public ServiceAccount getServiceAccount(String namespace, String name) {
        return client.serviceAccounts()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public ServiceAccount createOrUpdateServiceAccount(String namespace, ServiceAccount sa) {
        if (sa.getMetadata() != null) {
            String bodyNs = sa.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                sa.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "ServiceAccount namespace in body does not match path namespace"
                );
            }
        }

        return client.serviceAccounts()
                .inNamespace(namespace)
                .resource(sa)
                .createOrReplace();
    }

    public void deleteServiceAccount(String namespace, String name) {
        client.serviceAccounts()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== HORIZONTAL POD AUTOSCALERS (HPA) ==========

    public java.util.List<HorizontalPodAutoscaler> listHorizontalPodAutoscalers(String namespace) {
        return client.autoscaling()
                .v2()
                .horizontalPodAutoscalers()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public HorizontalPodAutoscaler getHorizontalPodAutoscaler(String namespace, String name) {
        return client.autoscaling()
                .v2()
                .horizontalPodAutoscalers()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public HorizontalPodAutoscaler createOrUpdateHorizontalPodAutoscaler(
            String namespace,
            HorizontalPodAutoscaler hpa
    ) {
        if (hpa.getMetadata() != null) {
            String bodyNs = hpa.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                hpa.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "HPA namespace in body does not match path namespace"
                );
            }
        }

        return client.autoscaling()
                .v2()
                .horizontalPodAutoscalers()
                .inNamespace(namespace)
                .resource(hpa)
                .createOrReplace();
    }

    public void deleteHorizontalPodAutoscaler(String namespace, String name) {
        client.autoscaling()
                .v2()
                .horizontalPodAutoscalers()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== NETWORKPOLICIES ==========

    public java.util.List<NetworkPolicy> listNetworkPolicies(String namespace) {
        return client.network()
                .v1()
                .networkPolicies()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public NetworkPolicy getNetworkPolicy(String namespace, String name) {
        return client.network()
                .v1()
                .networkPolicies()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public NetworkPolicy createOrUpdateNetworkPolicy(String namespace, NetworkPolicy np) {
        if (np.getMetadata() != null) {
            String bodyNs = np.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                np.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "NetworkPolicy namespace in body does not match path namespace"
                );
            }
        }

        return client.network()
                .v1()
                .networkPolicies()
                .inNamespace(namespace)
                .resource(np)
                .createOrReplace();
    }

    public void deleteNetworkPolicy(String namespace, String name) {
        client.network()
                .v1()
                .networkPolicies()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== POD DISRUPTION BUDGETS (PDB) ==========

    public java.util.List<PodDisruptionBudget> listPodDisruptionBudgets(String namespace) {
        return client.policy()
                .v1()
                .podDisruptionBudget()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public PodDisruptionBudget getPodDisruptionBudget(String namespace, String name) {
        return client.policy()
                .v1()
                .podDisruptionBudget()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public PodDisruptionBudget createOrUpdatePodDisruptionBudget(
            String namespace,
            PodDisruptionBudget pdb
    ) {
        if (pdb.getMetadata() != null) {
            String bodyNs = pdb.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                pdb.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "PDB namespace in body does not match path namespace"
                );
            }
        }

        return client.policy()
                .v1()
                .podDisruptionBudget()
                .inNamespace(namespace)
                .resource(pdb)
                .createOrReplace();
    }

    public void deletePodDisruptionBudget(String namespace, String name) {
        client.policy()
                .v1()
                .podDisruptionBudget()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== RESOURCE QUOTAS ==========

    public java.util.List<ResourceQuota> listResourceQuotas(String namespace) {
        return client.resourceQuotas()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public ResourceQuota getResourceQuota(String namespace, String name) {
        return client.resourceQuotas()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public ResourceQuota createOrUpdateResourceQuota(String namespace, ResourceQuota rq) {
        if (rq.getMetadata() != null) {
            String bodyNs = rq.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                rq.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "ResourceQuota namespace in body does not match path namespace"
                );
            }
        }

        return client.resourceQuotas()
                .inNamespace(namespace)
                .resource(rq)
                .createOrReplace();
    }

    public void deleteResourceQuota(String namespace, String name) {
        client.resourceQuotas()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

        // ========== LIMIT RANGES ==========

    public java.util.List<LimitRange> listLimitRanges(String namespace) {
        return client.limitRanges()
                .inNamespace(namespace)
                .list()
                .getItems();
    }

    public LimitRange getLimitRange(String namespace, String name) {
        return client.limitRanges()
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public LimitRange createOrUpdateLimitRange(String namespace, LimitRange lr) {
        if (lr.getMetadata() != null) {
            String bodyNs = lr.getMetadata().getNamespace();
            if (bodyNs == null || bodyNs.isBlank()) {
                lr.getMetadata().setNamespace(namespace);
            } else if (!bodyNs.equals(namespace)) {
                throw new IllegalArgumentException(
                        "LimitRange namespace in body does not match path namespace"
                );
            }
        }

        return client.limitRanges()
                .inNamespace(namespace)
                .resource(lr)
                .createOrReplace();
    }

    public void deleteLimitRange(String namespace, String name) {
        client.limitRanges()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    // ========== PERSISTENTVOLUMES (CLUSTER SCOPE) ==========

    public java.util.List<PersistentVolume> listPersistentVolumes() {
        return client.persistentVolumes()
                .list()
                .getItems();
    }

    public PersistentVolume getPersistentVolume(String name) {
        return client.persistentVolumes()
                .withName(name)
                .get();
    }

    public PersistentVolume createOrUpdatePersistentVolume(PersistentVolume pv) {
        if (pv == null || pv.getMetadata() == null ||
                pv.getMetadata().getName() == null ||
                pv.getMetadata().getName().isBlank()) {
            throw new IllegalArgumentException("PersistentVolume metadata.name is required");
        }

        return client.persistentVolumes()
                .resource(pv)
                .createOrReplace();
    }

    public void deletePersistentVolume(String name) {
        client.persistentVolumes()
                .withName(name)
                .delete();
    }

    // ========== STORAGECLASSES (CLUSTER SCOPE) ==========

    public java.util.List<StorageClass> listStorageClasses() {
        return client.storage()
                .v1()
                .storageClasses()
                .list()
                .getItems();
    }

    public StorageClass getStorageClass(String name) {
        return client.storage()
                .v1()
                .storageClasses()
                .withName(name)
                .get();
    }

    public StorageClass createOrUpdateStorageClass(StorageClass sc) {
        if (sc == null || sc.getMetadata() == null ||
                sc.getMetadata().getName() == null ||
                sc.getMetadata().getName().isBlank()) {
            throw new IllegalArgumentException("StorageClass metadata.name is required");
        }

        return client.storage()
                .v1()
                .storageClasses()
                .resource(sc)
                .createOrReplace();
    }

    public void deleteStorageClass(String name) {
        client.storage()
                .v1()
                .storageClasses()
                .withName(name)
                .delete();
    }

    /**
     * Exec 1 lệnh trong pod (namespace scope).
     * Dùng cho API "terminal đơn giản": FE gửi command, backend chạy và trả về stdout + stderr.
     */
    /**
     * Exec 1 lệnh trong pod (namespace scope).
     * Dùng cho API "terminal đơn giản": FE gửi command, backend chạy và trả về stdout + stderr.
     */
    public String execInPod(String namespace, String podName, String container, String command) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Namespace must not be null or blank");
        }
        if (podName == null || podName.isBlank()) {
            throw new IllegalArgumentException("Pod name must not be null or blank");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command must not be null or blank");
        }

        // Check pod tồn tại trước
        var podRes = client.pods()
                .inNamespace(namespace)
                .withName(podName);

        if (podRes.get() == null) {
            throw new IllegalArgumentException("Pod not found: " + podName + " in namespace: " + namespace);
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ExecWatch watch = null;

        try {
            // Nếu có container -> exec trong container đó
            if (container != null && !container.isBlank()) {
                watch = client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .inContainer(container)
                        .writingOutput(stdout)
                        .writingError(stderr)
                        .withTTY()
                        .exec("sh", "-c", command);
            } else {
                // Không có container -> dùng container default
                watch = client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .writingOutput(stdout)
                        .writingError(stderr)
                        .withTTY()
                        .exec("sh", "-c", command);
            }

            // Tạm thời chờ command chạy xong (blocking kiểu "run & wait")
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while executing command in pod", e);
        } finally {
            if (watch != null) {
                watch.close();
            }
        }

        String out = stdout.toString(StandardCharsets.UTF_8);
        String err = stderr.toString(StandardCharsets.UTF_8);

        if (!err.isBlank()) {
            return out + "\n[stderr]\n" + err;
        }
        return out;
    }
}
