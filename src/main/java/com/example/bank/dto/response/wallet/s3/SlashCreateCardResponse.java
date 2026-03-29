package com.example.bank.dto.response.wallet.s3;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlashCreateCardResponse {

    private String id;
    private String brand;
    private String last4;

    private Expiration expiration;

    @Getter
    @Setter
    public static class Expiration {
        private Integer month;
        private Integer year;
    }
}
