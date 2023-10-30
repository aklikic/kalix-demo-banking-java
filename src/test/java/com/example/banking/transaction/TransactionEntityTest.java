package com.example.banking.transaction;

import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.example.banking.transaction.TransactionApiModel.*;
import static com.example.banking.transaction.DomainModel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionEntityTest {

    @Test
    public void test(){
        var cardId = UUID.randomUUID().toString();
        var transactionId = UUID.randomUUID().toString();
        double amount = 100;
        var testKit = EventSourcedTestKit.of(transactionId, TransactionEntity::new);

        var processRequestResult = testKit.call(entity -> entity.process(new TransactionProcessRequest(amount,cardId)));
        var initiatedEvent = processRequestResult.getNextEventOfType(ProcessInitiated.class);
        assertEquals(amount, initiatedEvent.amountToWithdraw());
        State updatedState = (State)processRequestResult.getUpdatedState();
        assertEquals(TransactionStatus.INITIATED,updatedState.transaction().status());

        var setProcessedRequestResult = testKit.call(entity -> entity.setProcessedStatus(new TransactionProcessStatusRequest(TransactionProcessStatus.COMPLETED)));
        var processedEvent = setProcessedRequestResult.getNextEventOfType(Processed.class);
        assertEquals(TransactionStatus.PROCESSED_SUCCESS, processedEvent.status());
        updatedState = (State)setProcessedRequestResult.getUpdatedState();
        assertEquals(TransactionStatus.PROCESSED_SUCCESS,updatedState.transaction().status());
    }
}
