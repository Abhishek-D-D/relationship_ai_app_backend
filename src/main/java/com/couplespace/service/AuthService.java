package com.couplespace.service;

import com.couplespace.dto.AuthResponse;
import com.couplespace.dto.LoginRequest;
import com.couplespace.dto.RegisterRequest;
import com.couplespace.entity.User;
import com.couplespace.repository.UserRepository;
import com.couplespace.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .gender(request.gender())
                .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String access  = jwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refresh = jwtUtil.generateRefreshToken(user.getUserId());
        return new AuthResponse(access, refresh, AuthResponse.UserDto.from(user));
    }
}
