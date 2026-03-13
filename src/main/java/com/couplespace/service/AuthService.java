package com.couplespace.service;

import com.couplespace.dto.AuthResponse;
import com.couplespace.dto.LoginRequest;
import com.couplespace.dto.RegisterRequest;
import com.couplespace.entity.User;
import com.couplespace.repository.*;
import com.couplespace.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final CallLogRepository callLogRepository;
    private final PartnerPersonaRepository personaRepository;
    private final AiCoachMessageRepository coachMessageRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;
    private final CoupleRepository coupleRepository;
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
        String access = jwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refresh = jwtUtil.generateRefreshToken(user.getUserId());
        return new AuthResponse(access, refresh, AuthResponse.UserDto.from(user));
    }

    @Transactional
    public void deleteUserAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Decouple/Delete Relationship
        coupleRepository.findByPartnerId(userId).ifPresent(couple -> {
            boolean isPartner1 = couple.getPartner1().getUserId().equals(userId);
            if (couple.getPartner2() == null) {
                // Alone in couple -> Delete couple
                coupleRepository.delete(couple);
            } else {
                // Has partner -> Wipe the departing user's slot
                if (isPartner1) {
                    // Partner 1 is mandatory, shift Partner 2 to Partner 1
                    couple.setPartner1(couple.getPartner2());
                    couple.setPartner2(null);
                    coupleRepository.save(couple);
                } else {
                    couple.setPartner2(null);
                    coupleRepository.save(couple);
                }
            }
        });

        // 2. Wipe Personal Data
        messageRepository.deleteBySenderId(userId);
        callLogRepository.deleteByInitiatorId(userId);
        personaRepository.deleteByUserId(userId);
        coachMessageRepository.deleteBySenderId(userId);
        onboardingResponseRepository.deleteByUserId(userId);

        // 3. Delete User Record
        userRepository.delete(user);
    }
}
