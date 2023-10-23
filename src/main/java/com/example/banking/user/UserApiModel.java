package com.example.banking.user;

import com.example.banking.CommonModel;

public interface UserApiModel extends CommonModel {

    record CreateUserRequest(String name, String cardId, String accountId){}

    record UserByCardViewRecord(String cardId, String userId, String accountId){}
}
