package com.example.bank.service.wallet.admin;


import com.example.bank.dto.response.dashboard.AdminUserFinancialStatisticsResponse;


public interface AdminUserFinancialService {

    AdminUserFinancialStatisticsResponse getFinancialStatistics(Long userId);


}