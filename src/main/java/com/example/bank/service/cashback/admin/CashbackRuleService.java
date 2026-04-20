package com.example.bank.service.cashback.admin;

import com.example.bank.dto.request.cashback.admin.CreateCashbackRuleRequest;
import com.example.bank.dto.request.cashback.admin.UpdateCashbackRuleRequest;
import com.example.bank.dto.response.cashback.admin.CashbackRuleResponse;

public interface CashbackRuleService {
    CashbackRuleResponse createCashbackRule(CreateCashbackRuleRequest request);

    CashbackRuleResponse updateCashbackRule(Long id, UpdateCashbackRuleRequest request);

    void deleteCashbackRule(Long id);

}
