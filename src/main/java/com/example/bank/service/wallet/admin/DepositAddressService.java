package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.CreateDepositAddressRequest;
import com.example.bank.dto.response.wallet.response.DepositAddressResponse;

public interface DepositAddressService {
    DepositAddressResponse createDepositAddress(CreateDepositAddressRequest request);
}
