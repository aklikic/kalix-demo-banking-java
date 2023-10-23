package com.example.banking.user;

import java.time.Instant;

public interface DomainModel {

    interface Event{}

    record Created(String name, String cardId, String accountId, Instant timestamp) implements Event{}


    record State(User user){
        public static State empty(){
            return new State(null);
        }
        public boolean isEmpty(){
            return user() == null;
        }

        public State onCreatedEvent(Created event){
            return new State(new User(event.name(), event.cardId(), event.accountId(), UserStatus.CREATED ,event.timestamp()));
        }
    }

    enum UserStatus{
        CREATED
    }
    record User(String name, String cardId, String accountId, UserStatus status, Instant lastUpdatedTimestamp){}
}
