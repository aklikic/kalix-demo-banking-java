package com.example.banking.account;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface DomainModel {

    interface Event{}

    record Created(String accountNumber, double initialBalance, Instant timestamp) implements Event{}
    record BalanceReserved(String transactionId, double amountReserved,  double balanceAfter, Instant timestamp) implements Event{}
    record BalanceReservationCanceled(String transactionId, double amountReserved, double balanceAfter,  Instant timestamp) implements Event{}
    record BalanceReservationCompleted(String transactionId, Instant timestamp) implements Event{}
//    record BalanceToppedUp(String transactionId, double amountToppedUp, double balanceAfter,  Instant timestamp) implements Event{}


    record State(Account account) {
        public static State empty() {
            return new State(null);
        }

        public boolean isEmpty() {
            return account() == null;
        }

        public Optional<Transaction> getTransactionById(String transactionId){
            return account().processedTransactions().stream().filter(t -> t.transactionId().equals(transactionId)).findFirst();
        }
        public boolean checkIfTransactionAlreadyProcessed(String transactionId) {
            return getTransactionById(transactionId).isPresent();
        }

        public State onCreatedEvent(Created event) {
            return new State(new Account(event.accountNumber(), event.initialBalance(), new ArrayList<>(), event.timestamp()));
        }

        public State onBalanceReservedEvent(BalanceReserved event) {
            account().processedTransactions().add(new Transaction(event.transactionId(), event.amountReserved()));
            return new State(new Account(account().accountNumber(), event.balanceAfter(), account().processedTransactions(), event.timestamp()));
        }

        public State onBalanceReservationCanceledEvent(BalanceReservationCanceled event) {
            account().processedTransactions().removeIf(t -> t.transactionId().equals(event.transactionId()));
            return new State(new Account(account().accountNumber(), event.balanceAfter(), account().processedTransactions(), event.timestamp()));
        }

        public State onBalanceReservationCompletedEvent(BalanceReservationCompleted event) {
            account().processedTransactions().removeIf(t -> t.transactionId().equals(event.transactionId()));
            return new State(new Account(account().accountNumber(), account().balance(), account().processedTransactions(), event.timestamp()));
        }

//        public State onBalanceToppedUpEvent(BalanceToppedUp event) {
//            account().processedTransactionsIds().add(event.transactionId());
//            return new State(new Account(account().accountNumber(), event.balanceAfter(), account().processedTransactionsIds(), event.timestamp()));
//        }
    }

    record Transaction(String transactionId, double amountReserved){};
    record Account(String accountNumber, double balance,List<Transaction> processedTransactions, Instant lastUpdatedTimestamp){}
}
