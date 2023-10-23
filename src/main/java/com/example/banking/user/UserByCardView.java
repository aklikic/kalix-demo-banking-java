package com.example.banking.user;

import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static com.example.banking.user.UserApiModel.*;
@Table("user_by_card")
@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class UserByCardView extends View<UserByCardViewRecord> {

    @GetMapping("/user/get_by_card/{cardId}")
    @Query("SELECT * FROM user_by_card WHERE cardId = :cardId")
    public UserByCardViewRecord getUserByCard(@PathVariable String cardId){
        return null;
    }

    public UpdateEffect<UserByCardViewRecord> onCreated(DomainModel.Created event){
        String userId = updateContext().eventSubject().get();
        return effects().updateState(new UserByCardViewRecord(event.cardId(), userId, event.accountId()));
    }

}
