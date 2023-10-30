package com.example.banking.process;

import com.example.banking.transaction.TransactionEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.banking.CommonModel.Ack;
import static com.example.banking.process.ProcessApiModel.StartProcessRequest;
import static com.example.banking.transaction.DomainModel.ProcessInitiated;

@Subscribe.EventSourcedEntity(value = TransactionEntity.class,ignoreUnknown = true)
public class TransactionProcessAction extends Action {

    private final static Logger log = LoggerFactory.getLogger(TransactionProcessAction.class);
    private final ComponentClient componentClient;

    public TransactionProcessAction(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    public Effect<Ack> onProcessInitiated(ProcessInitiated event){
        String transactionId = actionContext().eventSubject().get();
        log.info("handleInitiated for {}: {}",transactionId,event);
        var deferredCall = componentClient.forWorkflow(transactionId).call(WithdrawBalanceWorkflow::startProcess).params(new StartProcessRequest(event.amountToWithdraw(), event.cardId()));
        return effects().forward(deferredCall);
    }
}
