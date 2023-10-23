package com.example.banking.account;

import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.example.banking.account.AccountApiModel.*;
import static com.example.banking.account.DomainModel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountEntityTest {

    @Test
    public void test(){
        var accountNumber = "1111";
        var accountId = UUID.randomUUID().toString();
        var initialBalance = 10000;

        var transactionId = UUID.randomUUID().toString();
        var amountToReserve = 100;
        var testKit = EventSourcedTestKit.of(accountId, AccountEntity::new);

        var createResult = testKit.call(entity -> entity.create(new CreateAccountRequest(accountNumber,initialBalance)));
        var createdEvent = createResult.getNextEventOfType(Created.class);
        assertEquals(initialBalance, createdEvent.initialBalance());
        State updatedState = (State)createResult.getUpdatedState();
        assertEquals(initialBalance,updatedState.account().balance());
        assertEquals(0, updatedState.account().processedTransactions().size());


        var reserveResult = testKit.call(entity -> entity.reserve(new ReserveBalanceRequest(transactionId,amountToReserve)));
        var reservedEvent = reserveResult.getNextEventOfType(BalanceReserved.class);
        assertEquals(amountToReserve, reservedEvent.amountReserved());
        assertEquals(initialBalance - amountToReserve, reservedEvent.balanceAfter());
        updatedState = (State)reserveResult.getUpdatedState();
        assertEquals(reservedEvent.balanceAfter(),updatedState.account().balance());
        assertEquals(1, updatedState.account().processedTransactions().size());

        var completeReservationResult = testKit.call(entity -> entity.completeReservation(new CompleteBalanceReservationRequest(transactionId)));
        var completedEvent = completeReservationResult.getNextEventOfType(BalanceReservationCompleted.class);
        updatedState = (State)completeReservationResult.getUpdatedState();
        assertEquals(reservedEvent.balanceAfter(),updatedState.account().balance());
        assertEquals(0, updatedState.account().processedTransactions().size());
    }
}
