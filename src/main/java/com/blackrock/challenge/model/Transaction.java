package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {

    @JsonProperty("date")
    private String date;

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("ceiling")
    private long ceiling;

    @JsonProperty("remanent")
    private long remanent;

    public Transaction() {
    }

    public Transaction(String date, long amount, long ceiling, long remanent) {
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

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getCeiling() {
        return ceiling;
    }

    public void setCeiling(long ceiling) {
        this.ceiling = ceiling;
    }

    public long getRemanent() {
        return remanent;
    }

    public void setRemanent(long remanent) {
        this.remanent = remanent;
    }
}
