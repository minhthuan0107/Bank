package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.CardHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardHolderRepository extends JpaRepository<CardHolder, Long> {
}
