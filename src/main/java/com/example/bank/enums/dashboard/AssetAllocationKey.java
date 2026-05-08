package com.example.bank.enums.dashboard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetAllocationKey {

    AVAILABLE_BALANCE("available_balance"),
    ALLOCATED_BALANCE("allocated_balance"),
    FROZEN_BALANCE("frozen_balance");

    private final String value;
}
