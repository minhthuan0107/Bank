package com.example.bank.service.cashback.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.cashback.admin.CreateCashbackRuleRequest;
import com.example.bank.dto.request.cashback.admin.UpdateCashbackRuleRequest;
import com.example.bank.dto.response.cashback.admin.CashbackRefundPageResponse;
import com.example.bank.dto.response.cashback.admin.CashbackRefundResponse;
import com.example.bank.dto.response.cashback.admin.CashbackRuleResponse;
import com.example.bank.dto.response.cashback.admin.UserSimpleInfo;
import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.entity.wallet.UserCashbackMonthly;
import com.example.bank.enums.wallet.CashbackStatus;
import com.example.bank.projection.UserNameProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.CashbackRuleRepository;
import com.example.bank.repository.wallet.UserCashbackMonthlyRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.cashback.admin.CashbackRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashbackRuleServiceImpl implements CashbackRuleService {
    private final CashbackRuleRepository repository;
    private final UserCashbackMonthlyRepository userCashbackMonthlyRepository;
    private final WalletRepository walletRepository;
    private final WalletProperties walletProperties;
    private final UserRepository userRepository;


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

    @Transactional
    public void approveCashbackBatch(List<Long> userIds, String month) {
        if (userIds == null || userIds.isEmpty()) return;
        List<UserCashbackMonthly> monthlyList =
                userCashbackMonthlyRepository.findByUserIdInAndMonth(userIds, month);
        Map<Long, UserCashbackMonthly> monthlyMap =
                monthlyList.stream()
                        .collect(Collectors.toMap(UserCashbackMonthly::getUserId, m -> m));
        for (Long userId : userIds) {
            UserCashbackMonthly monthly = monthlyMap.get(userId);
            // ===== bắt buộc phải có =====
            if (monthly == null) {
                continue; // hoặc throw nếu muốn strict
            }
            // ===== skip nếu đã xử lý =====
            if (monthly.getStatus() == CashbackStatus.APPROVED ||
                    monthly.getStatus() == CashbackStatus.REJECTED) {
                continue;
            }
            BigDecimal cashback = monthly.getCashbackAmount();

            // ===== CREDIT WALLET =====
            int updated = walletRepository.increaseBalance(userId, cashback);
            if (updated == 0) {
                throw new WalletException(
                        MessageKeys.WALLET_UPDATE_FAILED,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== UPDATE STATUS =====
            monthly.setStatus(CashbackStatus.APPROVED);
            monthly.setApprovedAt(Instant.now());
        }

        // ===== chỉ cần save update =====
        userCashbackMonthlyRepository.saveAll(monthlyList);
    }



    @Override
    public CashbackRefundPageResponse getPendingCashback(
            String month,
            int page
    ) {
          int size = walletProperties.getDefaultPageSize();
        // ===== 1. Validate =====
        if (month == null || month.isBlank()) {
            throw new WalletException(
                    MessageKeys.INVALID_MONTH,
                    HttpStatus.BAD_REQUEST
            );
        }
        if (size <= 0) size = 15;
        // ===== 2. Query DB (snapshot ONLY) =====
        Page<UserCashbackMonthly> pageData =
                userCashbackMonthlyRepository.findByMonthAndStatus(
                        month,
                        CashbackStatus.PENDING,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "totalSpent")
                                        .and(Sort.by(Sort.Direction.DESC, "userId"))
                        )
                );

        List<UserCashbackMonthly> content = pageData.getContent();

        // ===== 3. Lấy list userId từ pageData =====
        List<Long> userIds = content.stream()
                .map(UserCashbackMonthly::getUserId)
                .distinct()
                .toList();

        // ===== Query username theo list userId =====
        Map<Long, UserSimpleInfo> userInfoMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.findByIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserNameProjection::getId,
                        u -> new UserSimpleInfo(
                                u.getUsername(),
                                u.getEmail()
                        )
                ));



        // ===== 5. Map response =====
        List<CashbackRefundResponse> items = content.stream()
                .map(m -> {
                    UserSimpleInfo userInfo = userInfoMap.get(m.getUserId());
                    return CashbackRefundResponse.builder()
                            .userId(m.getUserId())
                            .displayName(userInfo != null ? userInfo.username() : null)
                            .email(userInfo != null ? userInfo.email() : null)
                            .totalSpent(m.getTotalSpent())
                            .cashbackAmount(m.getCashbackAmount())
                            .percent(m.getPercent())
                            .cashbackStatus(m.getStatus())
                            .month(month)
                            .updatedAt(m.getUpdatedAt())
                            .build();
                })
                .toList();
        // ===== 4. Response =====
        return CashbackRefundPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalSize(pageData.getTotalElements())
                .hasNext(pageData.hasNext())
                .build();
    }
}
