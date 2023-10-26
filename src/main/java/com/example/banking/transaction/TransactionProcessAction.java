package com.example.banking.transaction;

import com.example.banking.process.WithdrawBalanceWorkflow;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.banking.CommonModel.Ack;
import static com.example.banking.process.ProcessApiModel.StartProcessRequest;
import static com.example.banking.transaction.DomainModel.Initiated;

@Subscribe.EventSourcedEntity(value = TransactionEntity.class,ignoreUnknown = true)
public class TransactionProcessAction extends Action {

    private static final Logger log = LoggerFactory.getLogger(TransactionEntity.class);
    private final ComponentClient componentClient;

    public TransactionProcessAction(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    public Effect<Ack> onInitiated(Initiated event){
        String transactionId = actionContext().eventSubject().get();
        log.info("handleInitiated for {}: {}",transactionId,event);
        var deferredCall = componentClient.forWorkflow(transactionId).call(WithdrawBalanceWorkflow::startProcess).params(new StartProcessRequest(event.amountToWithdraw(), event.cardId()));
        return effects().forward(deferredCall);
    }
}
