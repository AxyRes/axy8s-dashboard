package com.dofe.axy8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Smoke test: đảm bảo Spring context khởi động được.
 */
@SpringBootTest
@ActiveProfiles("test")
class Axy8sBackendApplicationTests {

    // Mock KubernetesClient để test không cần connect cluster thật
    @MockBean
    private KubernetesClient kubernetesClient;

    @Autowired
    private Axy8sBackendApplication application;

    @Test
    void contextLoads() {
        // Nếu context không lên được thì test này sẽ fail
    }
}
