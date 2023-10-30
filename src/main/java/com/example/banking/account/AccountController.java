package com.example.banking.account;



import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.*;

import static com.example.banking.account.AccountApiModel.*;

@RequestMapping("/api/account")
public class AccountController extends Action {

    private final ComponentClient componentClient;

    public AccountController(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @PostMapping("/manage/{accountId}/create")
    public Effect<Ack> create(@PathVariable String accountId, @RequestBody CreateAccountRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(accountId).call(AccountEntity::create).params(request));
    }
    @PostMapping("/manage/{accountId}/reserve")
    public Effect<BalanceResponse> reserve(@PathVariable String accountId, @RequestBody ReserveBalanceRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(accountId).call(AccountEntity::reserve).params(request));
    }
    @PostMapping("/manage/{accountId}/complete-reservation")
    public Effect<BalanceResponse> completeReservation(@PathVariable String accountId, @RequestBody CompleteBalanceReservationRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(accountId).call(AccountEntity::completeReservation).params(request));
    }
    @PostMapping("/manage/{accountId}/cancel-reservation")
    public Effect<BalanceResponse> cancelReservation(@PathVariable String accountId, @RequestBody CancelBalanceReservationRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(accountId).call(AccountEntity::cancelReservation).params(request));
    }
}
