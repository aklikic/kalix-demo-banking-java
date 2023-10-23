package com.example.banking.user;

import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.example.banking.user.DomainModel.*;
import static com.example.banking.user.UserApiModel.CreateUserRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserEntityTest {

    @Test
    public void test(){
        var name = "John Doe";
        var userId = UUID.randomUUID().toString();
        var cardId = UUID.randomUUID().toString();
        var accountId = UUID.randomUUID().toString();
        var testKit = EventSourcedTestKit.of(userId, UserEntity::new);

        var createResult = testKit.call(entity -> entity.create(new CreateUserRequest(name, cardId, accountId)));
        var createdEvent = createResult.getNextEventOfType(Created.class);
        assertEquals(accountId, createdEvent.accountId());
        State updatedState = (State)createResult.getUpdatedState();
        assertEquals(UserStatus.CREATED,updatedState.user().status());
    }
}
