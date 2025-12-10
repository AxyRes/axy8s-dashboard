package com.dofe.axy8s.k8s;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        // Auto:
        // - Nếu chạy trong cluster: dùng InClusterConfig
        // - Nếu dev local: dùng ~/.kube/config
        Config config = Config.autoConfigure(null);
        return new DefaultKubernetesClient(new ConfigBuilder(config).build());
    }
}
