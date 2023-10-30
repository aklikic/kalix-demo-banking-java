package com.example.banking.transaction;

import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static  com.example.banking.transaction.TransactionApiModel.*;
@RequestMapping("/api/transaction")
public class TransactionController extends Action {

    private final ComponentClient componentClient;

    public TransactionController(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @PostMapping("/{transactionId}/process")
    public Effect<Ack> process(@PathVariable String transactionId, @RequestBody TransactionProcessRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(transactionId).call(TransactionEntity::process).params(request));
    }
    @PostMapping("/get-by-status/{statusId}")
    public Effect<TransactionByStatusViewRecordList> getTransactionsByStatus(@PathVariable String statusId){
        return effects().forward(componentClient.forView().call(TransactionByStatusView::getTransactionsByStatus).params(statusId));
    }

    @PostMapping("/{transactionId}/set-processed-status")
    public Effect<Ack> setProcessedStatus(@PathVariable String transactionId, @RequestBody TransactionProcessStatusRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(transactionId).call(TransactionEntity::setProcessedStatus).params(request));
    }
}
