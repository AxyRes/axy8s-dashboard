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
     *
     * Rule (áp dụng cho NON-SUPER):
     *  - Nếu allowedNamespaces rỗng/null -> không được vào gì hết
     *  - Nếu allowedNamespaces = "*" -> được vào tất cả namespace TRỪ "default"
     *  - Nếu là list "ns1,ns2" -> chỉ được vào ns nằm trong list
     *  - "default" bị chặn, trừ khi sau này policy thay đổi.
     *
     * SUPER_ADMIN: bỏ qua rule này, vào được tất cả, kể cả "default".
     */
    private String allowedNamespaces;

    private boolean active;

    /**
     * Đánh dấu user hệ thống, ví dụ SUPER_ADMIN khởi tạo.
     * Có thể dùng để chặn xoá / sửa quyền sau này.
     */
    private boolean systemUser;
}
