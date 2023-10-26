package com.example.banking.process;

import com.example.banking.CommonModel;
import com.example.banking.account.AccountEntity;
import com.example.banking.transaction.TransactionEntity;
import com.example.banking.user.UserApiModel;
import com.example.banking.user.UserController;
import io.grpc.Status;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.example.banking.account.AccountApiModel.*;
import static com.example.banking.process.DomainModel.State;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessStatus;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessStatusRequest;
@Id("transactionId")
@TypeId("process")
@RequestMapping("/process/{transactionId}")
public class WithdrawBalanceWorkflow extends Workflow<State> {

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
    record ReserveBalanceInput(double amountToWithdraw, String accountId){}
    record UpdateTransactionStatusInput(TransactionProcessStatus status){}

    @Override
    public WorkflowDef<State> definition() {
        Step validateUser =
                step("validateUser")
                .call(GetUserAndAccountInput.class,  cmd ->  componentClient.forAction().call(UserController::getUserByCard).params(cmd.cardId()))
                .andThen(UserApiModel.UserByCardViewRecord.class, userByCardViewRecord -> {
                    if(userByCardViewRecord != null){
                        return effects().updateState(currentState().userValid(userByCardViewRecord.userId(), userByCardViewRecord.accountId()))
                                .transitionTo("reserveBalance", new ReserveBalanceInput(currentState().amountToWithdraw(), userByCardViewRecord.accountId()));
                    } else {
                        return effects().updateState(currentState().userNotFound())
                                .transitionTo("updateTransactionWithoutReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.USER_NOT_FOUND));
                    }
                });

        Step updateTransactionWithoutReserve =
                step("updateTransactionWithoutReserve")
                        .call( UpdateTransactionStatusInput.class, cmd -> componentClient.forEventSourcedEntity(transactionId).call(TransactionEntity::setProcessedStatus).params(new TransactionProcessStatusRequest(cmd.status())))
                        .andThen(Ack.class, __ -> effects().updateState(currentState().completed()).end());

        Step reserveBalance =
                step("reserveBalance")
                .call(ReserveBalanceInput.class, cmd -> componentClient.forEventSourcedEntity(cmd.accountId()).call(AccountEntity::reserve).params(new ReserveBalanceRequest(transactionId, cmd.amountToWithdraw())))
                .andThen(BalanceResponse.class, balanceResponse ->
                    switch (balanceResponse.status()){
                        case SUCCESS -> effects().updateState(currentState().completed()).transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.COMPLETED));
                        case ACCOUNT_NOT_FOUND -> effects().updateState(currentState().accountNotFound()).transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.ACCOUNT_NOT_FOUND));
                        case FUNDS_UNAVAILABLE -> effects().updateState(currentState().fundsUnavailable()).transitionTo("updateTransactionWithReserve", new UpdateTransactionStatusInput(TransactionProcessStatus.FUNDS_UNAVAILABLE));
                    }
                );

        Step updateTransactionWithReserve =
                step("updateTransactionWithReserve")
                .call( UpdateTransactionStatusInput.class, cmd -> componentClient.forEventSourcedEntity(transactionId).call(TransactionEntity::setProcessedStatus).params(new TransactionProcessStatusRequest(cmd.status())))
                .andThen(Ack.class, __ -> effects().updateState(currentState().completed())
                        .transitionTo("completeBalanceReservation", new ReserveBalanceInput(currentState().amountToWithdraw(), currentState().accountId())));

        Step completeBalanceReservation =
                step("completeBalanceReservation")
                .call(ReserveBalanceInput.class, cmd -> componentClient.forEventSourcedEntity(cmd.accountId()).call(AccountEntity::completeReservation).params(new CompleteBalanceReservationRequest(transactionId)))
                .andThen(BalanceResponse.class, balanceResponse -> {
                    var updateState = switch (balanceResponse.status()){
                        case SUCCESS -> effects().updateState(currentState().completed());
                        case ACCOUNT_NOT_FOUND -> effects().updateState(currentState().accountNotFound());
                        case FUNDS_UNAVAILABLE -> effects().updateState(currentState().fundsUnavailable());
                    };
                    return updateState.end();
                });
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
