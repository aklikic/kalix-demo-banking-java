package com.example.banking.transaction;

import kalix.javasdk.StatusCode;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

import static com.example.banking.transaction.TransactionApiModel.*;
import static com.example.banking.transaction.DomainModel.*;

@Id("transactionId")
@TypeId("transaction")
@RequestMapping("/transaction/{transactionId}")
public class TransactionEntity extends EventSourcedEntity<State, Event> {

    private static final Logger log = LoggerFactory.getLogger(TransactionEntity.class);
    private final String transactionId;

    public TransactionEntity(EventSourcedEntityContext context) {
        this.transactionId = context.entityId();
    }

    @Override
    public State emptyState() {
        return State.empty();
    }

    @PostMapping("/process")
    public Effect<Ack> process(@RequestBody TransactionProcessRequest request){
        log.info("Process transaction [{}] with details: {}", transactionId, request);
        if(currentState().isEmpty()){
            var event = new Initiated(request.amountToWithdraw(), request.cardId(), Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> Ack.of());
        }else if(currentState().transaction().status() == TransactionStatus.INITIATED){
            log.info("Already processed transaction [{}]",transactionId);
            return effects().reply(Ack.of());
        }else{
            log.error("Transaction in wrong status for process [{}]",transactionId);
            return effects().error("Transaction in wrong status for process", StatusCode.ErrorCode.BAD_REQUEST);
        }
    }

    @PostMapping("/set-processed-status")
    public Effect<Ack> setProcessedStatus(@RequestBody TransactionProcessStatusRequest request){
        TransactionStatus status = from(request.status());
        log.info("Set processed status for transaction [{}] with details: {}, status: {}/{}", transactionId, request,status,currentState().transaction().status());

        if(currentState().isEmpty()){
            log.error("Transaction not found [{}]",transactionId);
            return effects().error("Transaction not found", StatusCode.ErrorCode.NOT_FOUND);
        }else if(status == currentState().transaction().status()){
            log.info("Already processed transaction status [{}]",transactionId);
            return effects().reply(Ack.of());
        }else{
            var event = new Processed(status, Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> Ack.of());
        }
    }
    private TransactionStatus from(TransactionProcessStatus status){
        return switch (status){
            case COMPLETED -> TransactionStatus.PROCESSED_SUCCESS;
            case USER_NOT_FOUND -> TransactionStatus.PROCESSED_USER_NOT_FOUND;
            case ACCOUNT_NOT_FOUND -> TransactionStatus.PROCESSED_ACCOUNT_NOT_FOUND;
            case FUNDS_UNAVAILABLE -> TransactionStatus.PROCESSED_FUNDS_UNAVAILABLE;
        };
    }

    @EventHandler
    public State onInitiated(Initiated event){
        return currentState().onInitiatedEvent(event);
    }

    @EventHandler
    public State onProcessed(Processed event){
        return currentState().onProcessedEvent(event);
    }
}
