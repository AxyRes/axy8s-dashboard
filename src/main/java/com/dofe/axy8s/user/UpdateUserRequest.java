package com.dofe.axy8s.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request cập nhật user (role / namespaces / active)")
public class UpdateUserRequest {

    @Schema(description = "Vai trò mới của user (OPTIONAL)")
    private Role role;

    @Schema(
            description = """
                    Namespace user được phép xem, dạng:
                    - "*" : tất cả (trừ default nếu không phải SUPER_ADMIN)
                    - "ns1,ns2,ns3"
                    """
    )
    @Size(max = 500)
    private String allowedNamespaces;

    @Schema(description = "Bật/tắt user (true = active, false = disable)", example = "true")
    private Boolean active;

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
