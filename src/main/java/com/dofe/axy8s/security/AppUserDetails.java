package com.dofe.axy8s.security;

import com.dofe.axy8s.user.Role;
import com.dofe.axy8s.user.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    private final UserEntity user;

    public AppUserDetails(UserEntity user) {
        this.user = user;
    }

    public Role getRole() {
        return user.getRole();
    }

    public String getAllowedNamespacesRaw() {
        return user.getAllowedNamespaces();
    }

    public boolean isSuperAdmin() {
        return user.getRole() == Role.SUPER_ADMIN;
    }

    public boolean isAdmin() {
        return user.getRole() == Role.ADMIN;
    }

    public boolean isSystemUser() {
        return user.isSystemUser();
    }

    /**
     * Kiểm tra user có được phép truy cập namespace này không.
     *
     * SUPER_ADMIN:
     *  - Luôn được vào tất cả namespace, kể cả "default".
     *
     * Các role khác:
     *  - Nếu allowedNamespaces rỗng/null -> không được vào gì hết
     *  - Nếu allowedNamespaces = "*" -> được vào tất cả namespace TRỪ "default"
     *  - Nếu là list "ns1,ns2" -> chỉ được vào ns nằm trong list
     *  - "default" luôn bị chặn trong rule hiện tại.
     */
    public boolean canAccessNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }

        // SUPER_ADMIN chơi full cluster
        if (isSuperAdmin()) {
            return true;
        }

        String allowed = user.getAllowedNamespaces();
        if (allowed == null || allowed.isBlank()) {
            return false;
        }

        // Chặn default theo policy
        if ("default".equals(namespace)) {
            return false;
        }

        allowed = allowed.trim();

        // "*" = all namespace trừ default
        if ("*".equals(allowed)) {
            return true;
        }

        String[] parts = allowed.split(",");
        for (String part : parts) {
            if (namespace.equals(part.trim())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_USER, ROLE_VIEWER
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.isActive();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.isActive();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
