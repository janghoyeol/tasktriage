package com.tasktriage.backend.auth;

import com.tasktriage.backend.auth.dto.LoginRequest;
import com.tasktriage.backend.auth.dto.RegisterRequest;
import com.tasktriage.backend.auth.dto.TokenResponse;
import com.tasktriage.backend.auth.dto.UserResponse;
import com.tasktriage.backend.security.JwtTokenProvider;
import com.tasktriage.backend.user.User;
import com.tasktriage.backend.user.UserRepository;
import com.tasktriage.backend.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        UserRole role = request.role() != null ? request.role() : UserRole.OWNER;
        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role);

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }

    public TokenResponse login(LoginRequest request) {
        // AuthenticationManager가 CustomUserDetailsService + PasswordEncoder로 자격 증명을 검증한다.
        // 비밀번호가 틀리면 여기서 BadCredentialsException이 던져진다.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.email()));

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new TokenResponse(token, "Bearer", jwtTokenProvider.getExpirationSeconds());
    }
}
