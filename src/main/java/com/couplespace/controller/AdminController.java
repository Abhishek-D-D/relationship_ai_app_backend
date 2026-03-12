package com.couplespace.controller;

import com.couplespace.dto.AdminStatsDto;
import com.couplespace.dto.AdminUserDto;
import com.couplespace.entity.User;
import com.couplespace.entity.Couple;
import com.couplespace.repository.CoupleRepository;
import com.couplespace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        long totalUsers = userRepository.count();
        long totalCouples = coupleRepository.count();

        return ResponseEntity.ok(AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalCouples(totalCouples)
                .build());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        List<Couple> allCouples = coupleRepository.findAll();

        List<AdminUserDto> users = userRepository.findAll().stream()
                .map(user -> {
                    boolean isLinked = allCouples.stream()
                            .anyMatch(c -> user.equals(c.getPartner1()) || user.equals(c.getPartner2()));

                    return AdminUserDto.builder()
                            .userId(user.getUserId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .gender(user.getGender())
                            .createdAt(user.getCreatedAt())
                            .partnerLinked(isLinked)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }
}
