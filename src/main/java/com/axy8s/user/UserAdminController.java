package com.dofe.axy8s.user;

import com.dofe.axy8s.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminController(UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * List tất cả user - chỉ SUPER_ADMIN & ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<UserEntity> listUsers() {
        return userRepository.findAll();
    }

    /**
     * Tạo user mới.
     * Chỉ SUPER_ADMIN & ADMIN được tạo.
     * ADMIN không được tạo user role SUPER_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication
    ) {
        AppUserDetails current = (AppUserDetails) authentication.getPrincipal();

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (request.getRole() == Role.SUPER_ADMIN && !current.isSuperAdmin()) {
            return ResponseEntity.status(403).body("Only SUPER_ADMIN can create SUPER_ADMIN users");
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .allowedNamespaces(request.getAllowedNamespaces())
                .active(request.isActive())
                .systemUser(false)
                .build();

        userRepository.save(user);
        return ResponseEntity.ok("User created");
    }

    /**
     * Đổi password cho user khác.
     * Chỉ SUPER_ADMIN & ADMIN.
     * USER / VIEWER không có endpoint để đổi pass.
     *
     * Rule:
     *  - Nếu target là SUPER_ADMIN thì chỉ SUPER_ADMIN được đổi.
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        AppUserDetails current = (AppUserDetails) authentication.getPrincipal();

        UserEntity target = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (target == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // Nếu target là SUPER_ADMIN mà current không phải SUPER_ADMIN -> cút
        if (target.getRole() == Role.SUPER_ADMIN && !current.isSuperAdmin()) {
            return ResponseEntity.status(403).body("Only SUPER_ADMIN can change SUPER_ADMIN password");
        }

        target.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(target);

        return ResponseEntity.ok("Password updated");
    }
}
