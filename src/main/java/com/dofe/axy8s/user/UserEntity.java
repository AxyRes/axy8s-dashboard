package com.dofe.axy8s.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    /**
     * Danh sách namespace được phép, dạng:
     *   "*"
     *   "ns1,ns2,ns3"
     */
    private String allowedNamespaces;

    private boolean active;

    /**
     * Đánh dấu user hệ thống (SUPER_ADMIN khởi tạo).
     * Đổi tên cột để tránh đụng keyword SYSTEM_USER của H2.
     */
    @Column(name = "system_user_flag", nullable = false)
    private boolean systemUser;
}
