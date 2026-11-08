package com.celehub.backend.tuition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TuitionPaymentRepository extends JpaRepository<TuitionPayment, Long> {

    Optional<TuitionPayment> findByBankTxnId(String bankTxnId);

    @Query("SELECT p FROM TuitionPayment p WHERE p.bill.id IN :ids ORDER BY p.paidAt DESC")
    List<TuitionPayment> findByBillIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM TuitionPayment p WHERE p.bill.id = :billId")
    BigDecimal sumByBill(@Param("billId") Long billId);

    long countByBillId(Long billId);

    @Query("""
            SELECT p FROM TuitionPayment p JOIN FETCH p.bill b JOIN FETCH b.student JOIN FETCH b.course
            WHERE p.id = :id""")
    Optional<TuitionPayment> findDetail(@Param("id") Long id);
}
