package com.example.banking.process;

public interface ProcessApiModel {
    record StartProcessRequest(double amountToWithdraw, String cardId){}
}
