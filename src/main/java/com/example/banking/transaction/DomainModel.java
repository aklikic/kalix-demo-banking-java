package com.example.banking.transaction;

import java.time.Instant;

public interface DomainModel {

    interface Event{}

    record ProcessInitiated(double amountToWithdraw, String cardId, Instant timestamp)implements Event{}
    record Processed(TransactionStatus status, Instant timestamp)implements Event{}
    record State(Transaction transaction){
        public static State empty(){
            return new State(null);
        }
        public boolean isEmpty(){
            return transaction()==null;
        }
        public State onProcessInitiatedEvent(ProcessInitiated event){
            return new State(new Transaction(event.amountToWithdraw(),event.cardId(), TransactionStatus.INITIATED, event.timestamp()));
        }
        public State onProcessedEvent(Processed event){
            return new State(new Transaction(transaction.amountToWithdraw(),transaction.cardId(), event.status(), event.timestamp()));
        }
    }

    enum TransactionStatus {
        INITIATED,
        USER_NOT_FOUND,
        ACCOUNT_NOT_FOUND,
        FUNDS_UNAVAILABLE,
        SUCCESS

    }
    record Transaction(double amountToWithdraw, String cardId, TransactionStatus status, Instant lastUpdatedTimestamp){ }
}
