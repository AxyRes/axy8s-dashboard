package com.dofe.axy8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // ép dùng application-test.yml
class Axy8sBackendApplicationTests {

    @MockBean
    private KubernetesClient kubernetesClient;

    @Autowired
    private Axy8sBackendApplication application;

    @Test
    void contextLoads() {
    }
}
