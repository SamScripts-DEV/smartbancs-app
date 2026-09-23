package com.smartbancs.transactions.dto;

import com.smartbancs.transactions.model.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransferResponse {


    private Long transactionId;
    private String status;
    private BigDecimal amount;
    private String traceId;
    private OffsetDateTime createdAt;

    public TransferResponse(Transaction transaction) {
        this.transactionId = transaction.getId();
        this.status = transaction.getStatus().name();
        this.amount = transaction.getAmount();
        this.traceId = transaction.getTraceId();
        this.createdAt = transaction.getCreatedAt();
    }

    public Long getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getTraceId() { return traceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    
}
