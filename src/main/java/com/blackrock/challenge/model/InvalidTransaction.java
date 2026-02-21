package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvalidTransaction extends Transaction {

    @JsonProperty("message")
    private String message;

    public InvalidTransaction() {
    }

    public InvalidTransaction(String date, double amount, double ceiling, double remanent, String message) {
        super(date, amount, ceiling, remanent);
        this.message = message;
    }

    public InvalidTransaction(Transaction t, String message) {
        super(t.getDate(), t.getAmount(), t.getCeiling(), t.getRemanent());
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
