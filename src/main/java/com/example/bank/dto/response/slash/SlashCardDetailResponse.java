package com.example.bank.dto.response.slash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlashCardDetailResponse {
    private String id;
    private String name;
    private String last4;
    private String accountId;
    private String virtualAccountId;
    private String expiryYear;
    private String expiryMonth;
    private String createdAt;
    private Boolean isPhysical;
    private Boolean isSingleUse;
    private String status;
    private SpendingConstraint spendingConstraint;
    private String cardProductId;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpendingConstraint {
        private SpendingRule spendingRule;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpendingRule {
        private UtilizationLimit utilizationLimit;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UtilizationLimit {
        private LimitAmount limitAmount;
        private String preset;
        private String startDate;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LimitAmount {
        private Integer amountCents;
    }
}
