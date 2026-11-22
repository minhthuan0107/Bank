package com.example.bank.repository.wallet.slash;

import com.example.bank.entity.wallet.slash.CardBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardBinRepository extends JpaRepository<CardBin, Long> {

    Optional<CardBin> findByBin(String bin);
}