package com.celehub.backend.tuition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.celehub.backend.user.User;

@Entity
@Table(name = "tuition_payments")
public class TuitionPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private TuitionBill bill;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "bank_txn_id", unique = true, length = 100)
    private String bankTxnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected TuitionPayment() {}

    public TuitionPayment(TuitionBill bill, BigDecimal amount, PaymentMethod method, String bankTxnId, User confirmedBy) {
        this.bill = bill;
        this.amount = amount;
        this.method = method;
        this.bankTxnId = bankTxnId;
        this.confirmedBy = confirmedBy;
    }

    public Long getId() { return id; }
    public TuitionBill getBill() { return bill; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public String getBankTxnId() { return bankTxnId; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
