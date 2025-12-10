package com.dofe.axy8s.auth;

import com.dofe.axy8s.security.JwtService;
import com.dofe.axy8s.user.UserEntity;
import com.dofe.axy8s.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()
                    )
            );

            UserEntity user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow();

            String token = jwtService.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(
                    new AuthResponse(token, user.getUsername(), user.getRole())
            );
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
}
