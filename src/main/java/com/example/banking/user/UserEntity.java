package com.example.banking.user;


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

import static com.example.banking.user.DomainModel.*;
import static com.example.banking.user.UserApiModel.*;

@Id("userId")
@TypeId("user")
@RequestMapping("/user/{userId}")
public class UserEntity extends EventSourcedEntity<State, Event> {

    private final static Logger log = LoggerFactory.getLogger(UserEntity.class);
    private final String userId;

    public UserEntity(EventSourcedEntityContext context) {
        this.userId = context.entityId();
    }

    @Override
    public State emptyState() {
        return State.empty();
    }

    @PostMapping("/create")
    public Effect<Ack> create(@RequestBody CreateUserRequest request){
        log.info("Create user [{}] with details: {}", userId, request);
        if(currentState().isEmpty()){
            var event = new Created(request.name(), request.cardId(), request.accountId(), Instant.now());
            return effects().emitEvent(event).thenReply(updatedState -> Ack.of());
        }else {
            log.info("Already processed create [{}]",userId);
            return effects().reply(Ack.of());
        }
    }

    @EventHandler
    public State onCreated(Created event){
        return currentState().onCreatedEvent(event);
    }

}
