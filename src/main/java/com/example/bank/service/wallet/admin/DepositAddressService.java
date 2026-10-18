package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.CreateDepositAddressRequest;
import com.example.bank.dto.response.wallet.admin.DepositAddressResponse;

import java.util.List;

public interface DepositAddressService {
    DepositAddressResponse createDepositAddress(CreateDepositAddressRequest request);

    List<DepositAddressResponse> getDepositAddresses();

    void deleteDepositAddress(Long id);
}
