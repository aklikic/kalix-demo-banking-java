package com.example.banking.user;


import kalix.javasdk.action.Action;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.*;

import static com.example.banking.user.UserApiModel.*;
@RequestMapping("/api/user")
public class UserController extends Action {

    private final ComponentClient componentClient;

    public UserController(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @PostMapping("/manage/{userId}/create")
    public Effect<Ack> create(@PathVariable String userId, @RequestBody CreateUserRequest request){
        return effects().forward(componentClient.forEventSourcedEntity(userId).call(UserEntity::create).params(request));
    }
    @GetMapping("/get-by-card/{cardId}")
    public Effect<UserByCardViewRecord> getUserByCard(@PathVariable String cardId){
        return effects().forward(componentClient.forView().call(UserByCardView::getUserByCard).params(cardId));
    }
}
