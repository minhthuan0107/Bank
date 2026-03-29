package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardFundingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardFundingTransactionRepository extends JpaRepository<CardFundingTransaction, Long> {
    Optional<CardFundingTransaction> findByReferenceId(String referenceId);

}
