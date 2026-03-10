package com.example.bank.dto.response.otp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpEnqueuedResponse {
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("masked_recipient")
    private String maskedRecipient;
    @JsonProperty("ttl_seconds")  // user****@gmail.com, 09******89
    private int ttlSeconds;
    @JsonProperty("cooldown_seconds")
    private int cooldownSeconds;
}
