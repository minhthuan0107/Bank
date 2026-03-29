package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    long countByUserId(Long userId);

    Optional<Card> findBySlashCardId(String slashCardId);
}
