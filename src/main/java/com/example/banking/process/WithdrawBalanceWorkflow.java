package com.example.banking.process;


import com.example.banking.CommonModel;
import com.example.banking.account.AccountController;
import com.example.banking.transaction.TransactionController;
import com.example.banking.user.UserController;
import io.grpc.Status;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

import static com.example.banking.account.AccountApiModel.*;
import static com.example.banking.process.DomainModel.State;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessStatus;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessStatusRequest;
import static com.example.banking.user.UserApiModel.UserByCardViewRecord;
@Id("transactionId")
@TypeId("process")
@RequestMapping("/process/{transactionId}")
public class WithdrawBalanceWorkflow extends Workflow<State> {

    private final static Logger log = LoggerFactory.getLogger(WithdrawBalanceWorkflow.class);
    private final String transactionId;
    private final ComponentClient componentClient;

    public WithdrawBalanceWorkflow(WorkflowContext context, ComponentClient componentClient) {
        this.transactionId = context.workflowId();
        this.componentClient = componentClient;
    }

    @Override
    public State emptyState() {
        return State.empty();
    }

    record GetUserAndAccountInput(String cardId){}
    record GetUserAndAccountOutput(Optional<UserByCardViewRecord> maybeRecord){}
    record ReserveBalanceInput(double amountToWithdraw, String accountId){}
    record UpdateTransactionStatusInput(TransactionProcessStatus status){}

    @Override
    public WorkflowDef<State> definition() {
        Step validateUser =
                step("validateUser")
                .asyncCall(GetUserAndAccountInput.class,  cmd  ->
                   componentClient.forAction().call(UserController::getUserByCard).params(cmd.cardId()).execute()
                           .thenApply(Optional::ofNullable)
                           .thenApply(GetUserAndAccountOutput::new)
                            .exceptionally(e -> {
                                if(e.getCause().getCause() instanceof WebClientResponseException.NotFound){
                                    log.error("User not found by cardId "+cmd.cardId());
                                    return new GetUserAndAccountOutput(Optional.empty());
                                }else {
                                    log.error("Error on validateUser:{}", e);
                                    throw (RuntimeException) e;
                                }
                            })
                )
                .andThen(GetUserAndAccountOutput.class, output -> {
                    if(output.maybeRecord().isPresent()){
                        return effects().updateState(currentState().userValid(output.maybeRecord().get().userId(), output.maybeRecord().get().accountId()))
                                .transitionTo("reserveBalance", new ReserveBalanceInput(currentState().amountToWithdraw(), output.maybeRecord().get().accountId()));
                    } else {
                        return effects().updateState(currentState().userNotFound())
                                .transitionTo("updateTransactionWithoutReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.USER_NOT_FOUND));
                    }
                });


        Step updateTransactionWithoutReserve =
                step("updateTransactionWithoutReserve")
                        .call( UpdateTransactionStatusInput.class, cmd -> componentClient.forAction().call(TransactionController::setProcessedStatus)
                                .params(transactionId, new TransactionProcessStatusRequest(cmd.status())))
                        .andThen(Ack.class, __ -> effects().updateState(currentState().completed()).end());

        Step reserveBalance =
                step("reserveBalance")
                .call(ReserveBalanceInput.class, cmd -> componentClient.forAction().call(AccountController::reserve).params(cmd.accountId(),new ReserveBalanceRequest(transactionId, cmd.amountToWithdraw())))
                .andThen(BalanceResponse.class, balanceResponse ->
                    switch (balanceResponse.status()){
                        case SUCCESS -> effects().updateState(currentState().completed()).transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.COMPLETED));
                        case ACCOUNT_NOT_FOUND -> effects().updateState(currentState().accountNotFound())
                                .transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.ACCOUNT_NOT_FOUND));
                        case FUNDS_UNAVAILABLE -> effects().updateState(currentState().fundsUnavailable())
                                .transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.FUNDS_UNAVAILABLE));
                    }
                );

        Step updateTransactionWithReserve =
                step("updateTransactionWithReserve")
                .call( UpdateTransactionStatusInput.class, cmd -> componentClient.forAction().call(TransactionController::setProcessedStatus).params(transactionId,new TransactionProcessStatusRequest(cmd.status())))
                .andThen(Ack.class, __ -> effects().updateState(currentState().completed())
                        .transitionTo("completeBalanceReservation", new ReserveBalanceInput(currentState().amountToWithdraw(), currentState().accountId())));

        Step completeBalanceReservation =
                step("completeBalanceReservation")
                .call(ReserveBalanceInput.class, cmd -> componentClient.forAction().call(AccountController::completeReservation).params(cmd.accountId(),new CompleteBalanceReservationRequest(transactionId)))
                .andThen(BalanceResponse.class, balanceResponse -> effects().updateState(currentState().completed()).end());
        return workflow()
                .addStep(validateUser)
                .addStep(updateTransactionWithoutReserve)
                .addStep(reserveBalance)
                .addStep(updateTransactionWithReserve)
                .addStep(completeBalanceReservation);
    }


    @PostMapping("/start")
    public Effect<CommonModel.Ack> startProcess(@RequestBody ProcessApiModel.StartProcessRequest request){
        return switch (currentState().status()){
            case NOT_STARTED -> effects()
                    .updateState(currentState().started(request.amountToWithdraw(), request.cardId()))
                    .transitionTo("validateUser", new GetUserAndAccountInput(request.cardId()))
                    .thenReply(CommonModel.Ack.of());
            case STARTED -> effects().reply(CommonModel.Ack.of());
            default -> effects().error("Wrong status [%s]".formatted(currentState().status()), Status.Code.INVALID_ARGUMENT);
        };
    }


}
