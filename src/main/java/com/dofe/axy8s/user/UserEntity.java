package com.dofe.axy8s.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
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

    public UserEntity() {
    }

    public UserEntity(Long id, String username, String passwordHash, Role role,
                      String allowedNamespaces, boolean active, boolean systemUser) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.allowedNamespaces = allowedNamespaces;
        this.active = active;
        this.systemUser = systemUser;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAllowedNamespaces() {
        return allowedNamespaces;
    }

    public void setAllowedNamespaces(String allowedNamespaces) {
        this.allowedNamespaces = allowedNamespaces;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public void setSystemUser(boolean systemUser) {
        this.systemUser = systemUser;
    }

    public static class Builder {
        private Long id;
        private String username;
        private String passwordHash;
        private Role role;
        private String allowedNamespaces;
        private boolean active;
        private boolean systemUser;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder allowedNamespaces(String allowedNamespaces) {
            this.allowedNamespaces = allowedNamespaces;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder systemUser(boolean systemUser) {
            this.systemUser = systemUser;
            return this;
        }

        public UserEntity build() {
            return new UserEntity(id, username, passwordHash, role, allowedNamespaces, active, systemUser);
        }
    }
}
