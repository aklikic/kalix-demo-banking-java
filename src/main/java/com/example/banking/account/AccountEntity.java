package com.example.banking.account;


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
import java.util.Optional;

import static com.example.banking.account.AccountApiModel.*;
import static com.example.banking.account.DomainModel.*;

@Id("accountId")
@TypeId("account")
@RequestMapping("/account/{accountId}")
public class AccountEntity extends EventSourcedEntity<State, Event> {

    private static final Logger log = LoggerFactory.getLogger(AccountEntity.class);
    private final String accountId;

    public AccountEntity(EventSourcedEntityContext context) {
        this.accountId = context.entityId();
    }

    @Override
    public State emptyState() {
        return State.empty();
    }

    @PostMapping("/create")
    public Effect<Ack> create(@RequestBody CreateAccountRequest request){
        log.info("Create account [{}] with details: {}", accountId, request);
        if(currentState().isEmpty()){
            var event = new Created(request.accountNumber(), request.initialBalance(), Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> Ack.of());
        }else {
            log.info("Already processed create [{}]",accountId);
            return effects().reply(Ack.of());
        }
    }
    @PostMapping("/reserve")
    public Effect<BalanceResponse> reserve(@RequestBody ReserveBalanceRequest request){
        log.info("Reserve balance from account [{}]: {}", accountId, request);
        if(currentState().isEmpty()){
            return effects().reply(BalanceResponse.of(BalanceStatus.ACCOUNT_NOT_FOUND));
        }else {
            //deduplicate transaction reservation by checking if already received
            if(currentState().checkIfTransactionAlreadyProcessed(request.transactionId())){
                return effects().reply(BalanceResponse.success());
            }
            double newBalance = currentState().account().balance() - request.amountToWithdraw();
            if(newBalance < 0){
                return effects().reply(BalanceResponse.of(BalanceStatus.FUNDS_UNAVAILABLE));
            }
            var event = new BalanceReserved(request.transactionId(), request.amountToWithdraw(), newBalance,Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> BalanceResponse.success());

        }
    }
    @PostMapping("/complete-reservation")
    public Effect<BalanceResponse> completeReservation(@RequestBody CompleteBalanceReservationRequest request){
        log.info("Complete balance reservation from account [{}]: {}", accountId, request);
        if(currentState().isEmpty()){
            return effects().reply(BalanceResponse.of(BalanceStatus.ACCOUNT_NOT_FOUND));
        }else {
            //if transaction reservation does not exist it was already completed or canceled
            if(!currentState().checkIfTransactionAlreadyProcessed(request.transactionId())){
                return effects().reply(BalanceResponse.success());
            }
            var event = new BalanceReservationCompleted(request.transactionId(), Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> BalanceResponse.success());

        }
    }

    @PostMapping("/cancel-reservation")
    public Effect<BalanceResponse> cancelReservation(@RequestBody CancelBalanceReservationRequest request){
        log.info("Cancel balance reservation from account [{}]: {}", accountId, request);
        if(currentState().isEmpty()){
            return effects().reply(BalanceResponse.of(BalanceStatus.ACCOUNT_NOT_FOUND));
        }else {
            Optional<Transaction> reservationTransaction = currentState().getTransactionById(request.transactionId());
            //if transaction reservation does not exist it was already completed or canceled
            if(reservationTransaction.isEmpty()){
                return effects().reply(BalanceResponse.success());
            }

            //add amount back to balance
            double newBalance = currentState().account().balance() + reservationTransaction.get().amountReserved();
            var event = new BalanceReservationCanceled(request.transactionId(), reservationTransaction.get().amountReserved(), newBalance,Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> BalanceResponse.success());

        }
    }

    @EventHandler
    public State onCreated(Created event){
        return currentState().onCreatedEvent(event);
    }
    @EventHandler
    public State onBalanceReserved(BalanceReserved event){
        return currentState().onBalanceReservedEvent(event);
    }
    @EventHandler
    public State onBalanceReservationCompleted(BalanceReservationCompleted event){
        return currentState().onBalanceReservationCompletedEvent(event);
    }
    @EventHandler
    public State onBalanceReservationCanceled(BalanceReservationCanceled event){
        return currentState().onBalanceReservationCanceledEvent(event);
    }


}
