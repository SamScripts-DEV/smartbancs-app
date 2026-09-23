package com.smartbancs.transactions.controller;

import com.smartbancs.transactions.dto.TransferRequest;
import com.smartbancs.transactions.dto.TransferResponse;
import com.smartbancs.transactions.model.Transaction;
import com.smartbancs.transactions.service.TransferService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;



@RestController 
@RequestMapping ("/api/transactions")
public class TransactionController {

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransaction(@Valid @RequestBody TransferRequest request) {
        Transaction transaction = transferService.transferSafely(
            request.getSourceAccountId(), 
            request.getDestinationAccountId(), 
            request.getAmount(), 
            request.getIdempotencyKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(transaction));
    }
    
}
