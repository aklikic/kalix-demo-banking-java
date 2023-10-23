package com.example.banking;

public interface CommonModel {
    record Ack(){
        public static Ack of(){
            return new Ack();
        }
    }
}
