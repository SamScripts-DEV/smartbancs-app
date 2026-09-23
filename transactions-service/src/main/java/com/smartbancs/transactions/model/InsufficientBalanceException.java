package com.smartbancs.transactions.model;

public class InsufficientBalanceException extends RuntimeException{
    public InsufficientBalanceException(String accountNumber) {
        super("Insufficient balance in account: " + accountNumber);
    }

}
