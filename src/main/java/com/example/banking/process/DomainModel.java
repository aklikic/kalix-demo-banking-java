package com.example.banking.process;

public interface DomainModel {
    record State(double amountToWithdraw, String cardId, String userId, String accountId, ProcessStatus status){

        public static State empty(){
            return new State(0,null,null,null,ProcessStatus.NOT_STARTED);
        }
        public State started(double amountToWithdraw, String cardId){
            return new State(amountToWithdraw, cardId, null, null, ProcessStatus.STARTED);
        }
        public State userValid(String userId, String accountId){
            return new State(amountToWithdraw(), cardId(), userId, accountId, ProcessStatus.USER_VALIDATED);
        }
        public State userNotFound(){
            return new State(amountToWithdraw(), cardId(), userId(), accountId(), ProcessStatus.USER_NOT_FOUND);
        }

        public State accountNotFound(){
            return new State(amountToWithdraw(), cardId(), userId(), accountId(), ProcessStatus.ACCOUNT_NOT_FOUND);
        }

        public State fundsUnavailable(){
            return new State(amountToWithdraw(), cardId(), userId(), accountId(), ProcessStatus.FUNDS_UNAVAILABLE);
        }

        public State completed(){
            return new State(amountToWithdraw(),cardId(), userId(), accountId(),ProcessStatus.COMPLETED);
        }
    }

    enum ProcessStatus{
        NOT_STARTED,
        STARTED,
        USER_VALIDATED,
        USER_NOT_FOUND,
        ACCOUNT_NOT_FOUND,
        FUNDS_UNAVAILABLE,
        COMPLETED
    }
}
