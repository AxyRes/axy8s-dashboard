package com.dofe.axy8s.security;

import com.dofe.axy8s.user.Role;
import com.dofe.axy8s.user.UserEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserDetailsNamespaceAccessTest {

    private AppUserDetails createUser(Role role, String allowedNamespaces, boolean systemUser) {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(role.name().toLowerCase() + "-user")
                .passwordHash("dummy")
                .role(role)
                .allowedNamespaces(allowedNamespaces)
                .active(true)
                .systemUser(systemUser)
                .build();

        return new AppUserDetails(user);
    }

    @Test
    void superAdminCanAccessAllNamespacesIncludingDefault() {
        AppUserDetails superAdmin = createUser(Role.SUPER_ADMIN, "*", true);

        assertThat(superAdmin.canAccessNamespace("default")).isTrue();
        assertThat(superAdmin.canAccessNamespace("kube-system")).isTrue();
        assertThat(superAdmin.canAccessNamespace("dev")).isTrue();
        assertThat(superAdmin.canAccessNamespace("uat")).isTrue();
    }

    @Test
    void adminWithWildcardCannotAccessDefaultButCanAccessOthers() {
        AppUserDetails admin = createUser(Role.ADMIN, "*", false);

        assertThat(admin.canAccessNamespace("default")).isFalse();
        assertThat(admin.canAccessNamespace("kube-system")).isTrue();
        assertThat(admin.canAccessNamespace("dev")).isTrue();
    }

    @Test
    void userWithExplicitNamespacesCanAccessOnlyThoseNamespaces() {
        AppUserDetails user = createUser(Role.USER, "dev, uat", false);

        assertThat(user.canAccessNamespace("dev")).isTrue();
        assertThat(user.canAccessNamespace("uat")).isTrue();
        assertThat(user.canAccessNamespace("kube-system")).isFalse();
        assertThat(user.canAccessNamespace("default")).isFalse();
    }

    @Test
    void viewerWithEmptyAllowedNamespacesCannotAccessAnything() {
        AppUserDetails viewer = createUser(Role.VIEWER, "", false);

        assertThat(viewer.canAccessNamespace("dev")).isFalse();
        assertThat(viewer.canAccessNamespace("any")).isFalse();
    }

    @Test
    void viewerWithExplicitNamespacesCanOnlyViewThoseNamespaces() {
        AppUserDetails viewer = createUser(Role.VIEWER, "dev, uat", false);

        assertThat(viewer.canAccessNamespace("dev")).isTrue();
        assertThat(viewer.canAccessNamespace("uat")).isTrue();
        assertThat(viewer.canAccessNamespace("kube-system")).isFalse();
        assertThat(viewer.canAccessNamespace("default")).isFalse();
    }

    @Test
    void nullOrBlankNamespaceShouldReturnFalse() {
        AppUserDetails user = createUser(Role.USER, "*", false);

        assertThat(user.canAccessNamespace(null)).isFalse();
        assertThat(user.canAccessNamespace("")).isFalse();
        assertThat(user.canAccessNamespace("   ")).isFalse();
    }
}
