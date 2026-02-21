package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("ceiling")
    private double ceiling;

    @JsonProperty("remanent")
    private double remanent;

    public Transaction() {
    }

    public Transaction(String date, double amount, double ceiling, double remanent) {
        this.date = date;
        this.amount = amount;
        this.ceiling = ceiling;
        this.remanent = remanent;
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

    public double getCeiling() {
        return ceiling;
    }

    public void setCeiling(double ceiling) {
        this.ceiling = ceiling;
    }

    public double getRemanent() {
        return remanent;
    }

    public void setRemanent(double remanent) {
        this.remanent = remanent;
    }
}
