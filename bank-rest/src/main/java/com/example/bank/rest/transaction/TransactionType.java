package com.example.bank.rest.transaction;

// Types of money operations stored in TransactionEntity
public enum TransactionType {
    DEPOSIT,       // money added to account
    WITHDRAW,      // money taken from account
    TRANSFER_OUT,  // sent to another account
    TRANSFER_IN    // received from another account
}
