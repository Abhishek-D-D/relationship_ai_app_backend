package com.couplespace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {
    private UUID userId;
    private String name;
    private String email;
    private String gender;
    private LocalDateTime createdAt;
    private boolean partnerLinked;
}
