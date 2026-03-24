package com.couplespace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInsightDTO {
    private String moodSuggestion; // e.g. "Your partner seems a bit stressed."
    private String analyticalInsight; // More detailed reason from Aria
    private List<String> responseSuggestions; // List of suggested messages
}
