package com.example.bank.service.cashback.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.cashback.admin.CreateCashbackRuleRequest;
import com.example.bank.dto.request.cashback.admin.UpdateCashbackRuleRequest;
import com.example.bank.dto.response.cashback.admin.CashbackRuleResponse;
import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.repository.wallet.CashbackRuleRepository;
import com.example.bank.service.cashback.admin.CashbackRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashbackRuleServiceImpl implements CashbackRuleService {
    private final CashbackRuleRepository repository;

    /**
     * CREATE cashback rule
     */
    @Transactional
    public CashbackRuleResponse createCashbackRule(CreateCashbackRuleRequest request) {

        // ===== VALIDATE BUSINESS =====
        validateRange(request.getMinSpent(), request.getMaxSpent());
        if (repository.existsByCashbackPercentAndIsActiveTrue(request.getCashbackPercent())) {
            throw new WalletException(
                    MessageKeys.CASHBACK_PERCENT_ALREADY_EXISTS,
                    HttpStatus.BAD_REQUEST
            );
        }
        // ===== CREATE ENTITY =====
        CashbackRule rule = CashbackRule.builder()
                .minSpent(request.getMinSpent())
                .maxSpent(request.getMaxSpent())
                .cashbackPercent(request.getCashbackPercent())
                .isActive(true)
                .build();
        repository.save(rule);

        return mapToResponse(rule);
    }

    @Transactional
    public CashbackRuleResponse updateCashbackRule(Long id, UpdateCashbackRuleRequest request) {
        CashbackRule rule = repository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CASHBACK_RULE_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== UPDATE FIELD =====
        if (request.getMinSpent() != null) {
            rule.setMinSpent(request.getMinSpent());
        }

        if (request.getMaxSpent() != null) {
            rule.setMaxSpent(request.getMaxSpent());
        }

        // ===== VALIDATE =====
        validateRange(rule.getMinSpent(), rule.getMaxSpent());

        if (request.getCashbackPercent() != null) {
            boolean exists = repository
                    .existsByCashbackPercentAndIsActiveTrueAndIdNot(
                            request.getCashbackPercent(),
                            rule.getId()
                    );
            if (exists) {
                throw new WalletException(
                        MessageKeys.CASHBACK_PERCENT_ALREADY_EXISTS,
                        HttpStatus.BAD_REQUEST
                );
            }
            rule.setCashbackPercent(request.getCashbackPercent());
        }


        repository.save(rule);
        // RETURN giống create
        return mapToResponse(rule);
    }

    private CashbackRuleResponse mapToResponse(CashbackRule rule) {
        return CashbackRuleResponse.builder()
                .id(rule.getId())
                .minSpent(rule.getMinSpent())
                .maxSpent(rule.getMaxSpent())
                .cashbackPercent(rule.getCashbackPercent())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    /**
     * Validate logic business (KHÔNG để DTO làm)
     */
    private void validateRange(BigDecimal min, BigDecimal max) {
        if (min == null) {
            throw new WalletException(
                    MessageKeys.INVALID_CASHBACK_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (max != null && min.compareTo(max) >= 0) {
            throw new WalletException(
                    MessageKeys.INVALID_CASHBACK_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Soft delete cashback rule
     */
    @Transactional
    public void deleteCashbackRule(Long id) {

        CashbackRule rule = repository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CASHBACK_RULE_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // nếu đã inactive rồi thì không làm gì
        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            return;
        }

        rule.setIsActive(false);
        repository.save(rule);
    }

    @Override
    public List<CashbackRuleResponse> getCashbackRules() {
        List<CashbackRule> rules = repository.findAllByIsActiveTrueOrderByMinSpentAsc();
        return rules.stream()
                .map(this::mapToResponse)
                .toList();
    }
}
