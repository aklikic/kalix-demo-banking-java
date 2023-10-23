package com.example.banking.transaction;

import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static com.example.banking.transaction.DomainModel.*;
import static com.example.banking.transaction.TransactionApiModel.TransactionByStatusViewRecord;
import static com.example.banking.transaction.TransactionApiModel.TransactionByStatusViewRecordList;
@Table("transaction_by_status")
@Subscribe.EventSourcedEntity(value = TransactionEntity.class, ignoreUnknown = true)
public class TransactionByStatusView extends View<TransactionByStatusViewRecord> {

    @GetMapping("/transaction/get_by_status/{statusId}")
    @Query("SELECT * as list FROM transaction_by_status WHERE statusId = :statusId")
    public TransactionByStatusViewRecordList getTransactionsByStatus(@PathVariable String statusId){
        return null;
    }

    public UpdateEffect<TransactionByStatusViewRecord> onInitiated(Initiated event){
        String transactionId = updateContext().eventSubject().get();
        return effects().updateState(new TransactionByStatusViewRecord(TransactionStatus.INITIATED.name(),transactionId));
    }
    public UpdateEffect<TransactionByStatusViewRecord> onProcessed(Processed event){
        String transactionId = updateContext().eventSubject().get();
        return effects().updateState(new TransactionByStatusViewRecord(event.status().name(),transactionId));
    }

}
