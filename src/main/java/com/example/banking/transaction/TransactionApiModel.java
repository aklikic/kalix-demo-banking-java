package com.example.banking.transaction;

import com.example.banking.CommonModel;

import java.util.List;

public interface TransactionApiModel extends CommonModel {

    record TransactionProcessRequest(double amountToWithdraw, String cardId){}
    record TransactionProcessStatusRequest(TransactionProcessStatus status){}
    enum TransactionProcessStatus{
        USER_NOT_FOUND,
        ACCOUNT_NOT_FOUND,
        FUNDS_UNAVAILABLE,
        COMPLETED
    }

    record TransactionByStatusViewRecord(String statusId, String transactionId){}
    record TransactionByStatusViewRecordList(List<TransactionByStatusViewRecord> list){}
}
