package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Expense {

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private long amount;

    public Expense() {
    }

    public Expense(String date, long amount) {
        this.date = date;
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
