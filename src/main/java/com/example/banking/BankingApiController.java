package com.example.banking;

import com.example.banking.account.AccountApiModel;
import com.example.banking.account.AccountController;
import com.example.banking.transaction.TransactionController;
import com.example.banking.user.UserApiModel;
import com.example.banking.user.UserByCardView;
import com.example.banking.user.UserController;
import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.*;

import static com.example.banking.transaction.TransactionApiModel.*;
import static com.example.banking.account.AccountApiModel.*;
import static com.example.banking.user.UserApiModel.*;

@RequestMapping("/api")
public class BankingApiController extends Action {

    private final ComponentClient componentClient;

    public BankingApiController(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @PostMapping("/user/manage/{userId}/create")
    public Effect<Ack> createUser(@PathVariable String userId, @RequestBody CreateUserRequest request){
        return effects().forward(componentClient.forAction().call(UserController::create).params(userId, request));
    }
    @GetMapping("/user/get-by-card/{cardId}")
    public Effect<UserByCardViewRecord> getUserByCard(@PathVariable String cardId){
        return effects().forward(componentClient.forView().call(UserByCardView::getUserByCard).params(cardId));
    }
    @PostMapping("/account/manage/{accountId}/create")
    public Effect<Ack> createAccount(@PathVariable String accountId, @RequestBody CreateAccountRequest request){
        return effects().forward(componentClient.forAction().call(AccountController::create).params(accountId, request));
    }
    @PostMapping("/transaction/{transactionId}/process")
    public Effect<Ack> processTransaction(@PathVariable String transactionId, @RequestBody TransactionProcessRequest request){
        return effects().forward(componentClient.forAction().call(TransactionController::process).params(transactionId, request));
    }
    @PostMapping("/transaction/get-by-status/{statusId}")
    public Effect<TransactionByStatusViewRecordList> getTransactionsByStatus(@PathVariable String statusId){
        return effects().forward(componentClient.forAction().call(TransactionController::getTransactionsByStatus).params(statusId));
    }

}
