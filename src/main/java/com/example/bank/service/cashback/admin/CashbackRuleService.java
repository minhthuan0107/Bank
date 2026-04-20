package com.example.bank.service.cashback.admin;

import com.example.bank.dto.request.cashback.admin.CreateCashbackRuleRequest;
import com.example.bank.dto.request.cashback.admin.UpdateCashbackRuleRequest;
import com.example.bank.dto.response.cashback.admin.CashbackRuleResponse;

import java.util.List;

public interface CashbackRuleService {
    CashbackRuleResponse createCashbackRule(CreateCashbackRuleRequest request);

    CashbackRuleResponse updateCashbackRule(Long id, UpdateCashbackRuleRequest request);

    void deleteCashbackRule(Long id);

    List<CashbackRuleResponse> getCashbackRules();

}
