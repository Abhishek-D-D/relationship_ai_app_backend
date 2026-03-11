package com.couplespace.dto;

import com.couplespace.entity.User;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {
    public record UserDto(
        UUID userId,
        String name,
        String email,
        String gender,
        String avatarUrl
    ) {
        public static UserDto from(User user) {
            return new UserDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getGender(),
                user.getAvatarUrl()
            );
        }
    }
}
