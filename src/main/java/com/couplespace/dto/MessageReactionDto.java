package com.couplespace.dto;

import java.util.UUID;

public record MessageReactionDto(
    UUID messageId,
    UUID userId,
    String userName,
    String emoji,
    String action, // "ADD" or "REMOVE"
    String updatedReactions // full JSON string of all reactions after update
) {}
