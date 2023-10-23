package com.example.banking.account;

import com.example.banking.CommonModel;

public interface AccountApiModel extends CommonModel {

    record CreateAccountRequest(String accountNumber, double initialBalance){}
    record ReserveBalanceRequest(String transactionId, double amountToWithdraw){}
    record CompleteBalanceReservationRequest(String transactionId){}
    record CancelBalanceReservationRequest(String transactionId){}
//    record TopUpBalanceRequest(String transactionId, double amountToTopUp){}

    record BalanceResponse(BalanceStatus status){
        public static BalanceResponse success(){
            return new BalanceResponse(BalanceStatus.SUCCESS);
        }
        public static BalanceResponse of(BalanceStatus status){
            return new BalanceResponse(status);
        }
    }
    enum BalanceStatus{
        SUCCESS,
        ACCOUNT_NOT_FOUND,
        FUNDS_UNAVAILABLE,
    }

}
