package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.DepositOrderImage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DepositOrderImageRepository extends JpaRepository<DepositOrderImage, Long> {

    boolean existsByDepositOrderId(Long depositOrderId);

    @Query("""
            SELECT i
            FROM DepositOrderImage i
            WHERE i.depositOrderId IN :depositOrderIds
            ORDER BY i.depositOrderId ASC, i.displayOrder ASC
            """)
    List<DepositOrderImage> findByDepositOrderIds(
            @Param("depositOrderIds") Collection<Long> depositOrderIds
    );


}
