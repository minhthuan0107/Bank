package com.example.bank.repository.auth;

import com.example.bank.entity.auth.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
}
