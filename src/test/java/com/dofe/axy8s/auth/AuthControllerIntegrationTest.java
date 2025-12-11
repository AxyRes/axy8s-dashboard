package com.dofe.axy8s.auth;

import com.dofe.axy8s.user.Role;
import com.dofe.axy8s.user.UserEntity;
import com.dofe.axy8s.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KubernetesClient kubernetesClient; // tránh connect cluster thật

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Đảm bảo có SUPER_ADMIN "admin"
        Optional<UserEntity> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            UserEntity admin = UserEntity.builder()
                    .username("admin")
                    .passwordHash("$2a$10$dummyPasswordHash") // giả, không dùng ở đây
                    .role(Role.SUPER_ADMIN)
                    .allowedNamespaces("*")
                    .active(true)
                    .systemUser(true)
                    .build();
            userRepository.save(admin);
        }
    }

    @Test
    void loginWithValidAdminCredentialsShouldReturnToken() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("123456"); // password mặc định từ AdminInitializer

        String json = objectMapper.writeValueAsString(request);

        var result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(body, AuthResponse.class);

        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    void loginWithInvalidPasswordShouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized());
    }
}
