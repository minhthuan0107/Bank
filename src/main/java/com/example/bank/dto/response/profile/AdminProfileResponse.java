package com.example.bank.dto.response.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AdminProfileResponse(

        @JsonProperty("username")
        String username,

        @JsonProperty("email")
        String email
) {
}
