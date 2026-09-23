package com.smartbancs.transactions.model;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
