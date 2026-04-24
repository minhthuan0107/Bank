package com.example.bank.repository.user;

import com.example.bank.entity.user.User;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.projection.UserNameProjection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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

    @Query("""
       SELECT u
       FROM User u
       JOIN u.role r
       WHERE r.name = 'ADMIN'
       AND u.status = :status
       """)
    Optional<User> findActiveAdmin(@Param("status") AccountStatus status);

    @Query("SELECT u.email FROM User u WHERE u.id = :userId")
    Optional<String> findEmailByUserId(Long userId);

    List<UserNameProjection> findByIdIn(Collection<Long> ids);


}
