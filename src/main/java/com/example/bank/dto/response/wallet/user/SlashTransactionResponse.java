package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlashTransactionResponse {

    private String id;
    private String date;
    private String description;
    private Long amountCents;

    private String status;
    private String detailedStatus;

    private String accountId;
    private String accountSubtype;

    private String memo;
    private String merchantDescription;

    private MerchantData merchantData;

    private String virtualAccountId;
    private String cardId;

    private OriginalCurrency originalCurrency;

    private String orderId;
    private String referenceNumber;

    private String authorizedAt;

    private String declineReason;
    private String approvalReason;

    private String providerAuthorizationId;

    private WireInfo wireInfo;
    private AchInfo achInfo;
    private RtpInfo rtpInfo;
    private FeeInfo feeInfo;
    private CryptoInfo cryptoInfo;

    // =========================
    // 🔥 MERCHANT
    // =========================
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchantData {
        private String description;
        private String categoryCode;
        private Location location;
    }

    @Data
    public static class Location {
        private String city;
        private String state;
        private String country;
        private String zip;
    }

    // =========================
    // 🔥 ORIGINAL CURRENCY
    // =========================
    @Data
    public static class OriginalCurrency {
        private String code;
        private Long amountCents;
        private Double conversionRate;
    }

    // =========================
    // 🔥 WIRE INFO
    // =========================
    @Data
    public static class WireInfo {
        private String typeCode;
        private String subtypeCode;
        private String omad;
        private String imad;
        private String senderReference;
        private String businessFunctionCode;
        private String counterpartyBank;
    }

    // =========================
    // 🔥 ACH INFO
    // =========================
    @Data
    public static class AchInfo {
        private String receiverId;
        private String companyId;
        private String companyDiscretionaryData;
        private String traceNumber;
        private String entryClassCode;
        private String paymentRelatedInfo;
        private String counterpartyBank;
        private String companyEntryDescription;
    }

    // =========================
    // 🔥 RTP INFO
    // =========================
    @Data
    public static class RtpInfo {
        private String counterpartyBank;
        private String endToEndId;
        private String routingNumber;
        private String originatorName;
        private String description;
    }

    // =========================
    // 🔥 FEE INFO
    // =========================
    @Data
    public static class FeeInfo {
        private RelatedTransaction relatedTransaction;
    }

    @Data
    public static class RelatedTransaction {
        private String id;
        private Long amount;
    }

    // =========================
    // 🔥 CRYPTO INFO
    // =========================
    @Data
    public static class CryptoInfo {
        private String txHash;
        private String senderAddress;
    }
}