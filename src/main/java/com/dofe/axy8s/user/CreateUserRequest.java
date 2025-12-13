package com.dofe.axy8s.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    /**
     * allowedNamespaces:
     *  - "*" hoặc "ns1,ns2"
     *  - KHÔNG tự động thêm "default"
     */
    private String allowedNamespaces;

    private boolean active = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
