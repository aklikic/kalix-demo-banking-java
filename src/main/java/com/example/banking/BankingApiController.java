package com.example.banking;

import com.example.banking.account.AccountApiModel;
import com.example.banking.account.AccountEntity;
import com.example.banking.transaction.TransactionByStatusView;
import com.example.banking.transaction.TransactionEntity;
import com.example.banking.user.UserApiModel;
import com.example.banking.user.UserByCardView;
import com.example.banking.user.UserEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.*;

import static com.example.banking.transaction.TransactionApiModel.*;

@RequestMapping("/api")
public class BankingApiController extends Action {

    private final ComponentClient componentClient;

    public BankingApiController(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @PostMapping("/user/manage/{userId}/create")
    public Effect<Ack> createUser(@PathVariable String userId, @RequestBody UserApiModel.CreateUserRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(userId).call(UserEntity::create).params(request));
    }
    @GetMapping("/user/get-by-card/{cardId}")
    public Effect<UserApiModel.UserByCardViewRecord> getUserByCard(@PathVariable String cardId){
        return effects().forward(componentClient.forView().call(UserByCardView::getUserByCard).params(cardId));
    }
    @PostMapping("/account/manage/{accountId}/create")
    public Effect<Ack> createAccount(@PathVariable String accountId, @RequestBody AccountApiModel.CreateAccountRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(accountId).call(AccountEntity::create).params(request));
    }
    @PostMapping("/transaction/{transactionId}/process")
    public Effect<Ack> processTransaction(@PathVariable String transactionId, @RequestBody TransactionProcessRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(transactionId).call(TransactionEntity::process).params(request));
    }
    @PostMapping("/transaction/get-by-status/{statusId}")
    public Effect<TransactionByStatusViewRecordList> getTransactionsByStatus(@PathVariable String statusId){
        return effects().forward(componentClient.forView().call(TransactionByStatusView::getTransactionsByStatus).params(statusId));
    }

}
