package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.CreateDepositAddressRequest;
import com.example.bank.dto.response.wallet.response.DepositAddressResponse;
import com.example.bank.entity.wallet.DepositAddress;
import com.example.bank.repository.wallet.DepositAddressRepository;
import com.example.bank.service.wallet.admin.DepositAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepositAddressServiceImpl implements DepositAddressService {

    private final DepositAddressRepository repository;

    @Override
    public DepositAddressResponse createDepositAddress(CreateDepositAddressRequest request) {


        String network = request.getNetwork().trim().toUpperCase();

        repository.findByCurrencyAndNetwork(request.getCurrency(), network)
                .ifPresent(existing -> {
                    throw new WalletException(
                            MessageKeys.DEPOSIT_ADDRESS_ALREADY_EXISTS,
                            HttpStatus.CONFLICT
                    );
                });

        DepositAddress depositAddress = new DepositAddress();
        depositAddress.setCurrency(request.getCurrency());
        depositAddress.setNetwork(network);
        depositAddress.setAddress(request.getAddress().trim());
        depositAddress.setDisplayOrder(request.getDisplayOrder());
        depositAddress.setStatus("ACTIVE");

        DepositAddress saved = repository.save(depositAddress);

        return DepositAddressResponse.builder()
                .id(saved.getId())
                .currency(request.getCurrency())
                .network(saved.getNetwork())
                .address(saved.getAddress())
                .displayOrder(saved.getDisplayOrder())
                .status(saved.getStatus())
                .build();
    }
}
