package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Expense {

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private double amount;

    public Expense() {
    }

    public Expense(String date, double amount) {
        this.date = date;
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
