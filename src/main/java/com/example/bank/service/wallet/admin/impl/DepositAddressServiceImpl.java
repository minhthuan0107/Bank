package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.CreateDepositAddressRequest;
import com.example.bank.dto.response.wallet.admin.DepositAddressResponse;
import com.example.bank.entity.wallet.DepositAddress;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.DepositAddressRepository;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.service.wallet.admin.DepositAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DepositAddressServiceImpl implements DepositAddressService {

    private final DepositAddressRepository repository;
    private final DepositOrderRepository depositOrderRepository;

    @Override
    @Transactional
    public DepositAddressResponse createDepositAddress(CreateDepositAddressRequest request) {

        String network = request.getNetwork().trim().toUpperCase(Locale.ROOT);
        Stablecoin currency = request.getCurrency();

        // ===== VALIDATE CURRENCY + NETWORK =====
        boolean validNetwork = switch (currency) {
            case USDT -> network.equals("TRC20") || network.equals("BEP20");
            case USDC -> network.equals("BASE");
            default -> false;
        };

        if (!validNetwork) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_ADDRESS_INVALID_NETWORK,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== CHECK NETWORK ALREADY CONFIGURED =====
        if (repository.existsByNetwork(network)) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_ADDRESS_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        // ===== CREATE ADDRESS =====
        DepositAddress depositAddress = new DepositAddress();
        depositAddress.setCurrency(currency);
        depositAddress.setNetwork(network);
        depositAddress.setAddress(request.getAddress().trim());
        depositAddress.setStatus("ACTIVE");

        // saveAndFlush để phát hiện vi phạm UNIQUE ngay trong phương thức.
        try {
            DepositAddress saved = repository.saveAndFlush(depositAddress);
            return toResponse(saved);

        } catch (DataIntegrityViolationException ex) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_ADDRESS_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepositAddressResponse> getDepositAddresses() {
        return repository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DepositAddressResponse toResponse(DepositAddress address) {
        return DepositAddressResponse.builder()
                .id(address.getId())
                .currency(address.getCurrency())
                .network(address.getNetwork())
                .address(address.getAddress())
                .status(address.getStatus())
                .build();
    }

    @Override
    @Transactional
    public void deleteDepositAddress(Long id) {

        // ===== FIND ADDRESS =====
        DepositAddress depositAddress = repository.findById(id)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.DEPOSIT_ADDRESS_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== CHECK PENDING DEPOSIT ORDERS =====
        boolean hasPendingOrders = depositOrderRepository
                .existsByCurrencyAndNetworkAndDestinationAddressAndStatus(
                        depositAddress.getCurrency(),
                        depositAddress.getNetwork(),
                        depositAddress.getAddress(),
                        DepositOrderStatus.PENDING
                );

        if (hasPendingOrders) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_ADDRESS_HAS_PENDING_ORDERS,
                    HttpStatus.CONFLICT
            );
        }

        // ===== DELETE ADDRESS =====
        repository.delete(depositAddress);
    }
}
