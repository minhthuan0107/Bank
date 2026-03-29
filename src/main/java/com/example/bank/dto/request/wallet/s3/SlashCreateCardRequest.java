package com.example.bank.dto.request.wallet.s3;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlashCreateCardRequest {

    private String type;
    private String name;
    private String accountId;
    private String cardProductId;
    private SpendingConstraint spendingConstraint;

    @Getter
    @Builder
    public static class SpendingConstraint {
        private SpendingRule spendingRule;
    }

    @Getter
    @Builder
    public static class SpendingRule {
        private UtilizationLimit utilizationLimit;
    }

    @Getter
    @Builder
    public static class UtilizationLimit {
        private LimitAmount limitAmount;
        private String preset;
    }

    @Getter
    @Builder
    public static class LimitAmount {
        private Long amountCents;
    }
}
