package com.example.bank.repository.user;

import com.example.bank.entity.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM User u
            JOIN FETCH u.role
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithRole(@Param("id") Long id);


}
