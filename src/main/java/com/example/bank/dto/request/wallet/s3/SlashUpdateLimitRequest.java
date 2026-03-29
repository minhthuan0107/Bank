package com.example.bank.dto.request.wallet.s3;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlashUpdateLimitRequest {

    private SpendingConstraint spendingConstraint;

    @Getter
    @Setter
    @Builder
    public static class SpendingConstraint {
        private SpendingRule spendingRule;
    }

    @Getter
    @Setter
    @Builder
    public static class SpendingRule {
        private UtilizationLimit utilizationLimit;
    }

    @Getter
    @Setter
    @Builder
    public static class UtilizationLimit {
        private LimitAmount limitAmount;
        private String preset; // "collective"
    }

    @Getter
    @Setter
    @Builder
    public static class LimitAmount {
        private long amountCents;
    }
}
