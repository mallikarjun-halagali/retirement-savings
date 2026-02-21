package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

public class ValidatorRequest {

    private List<Transaction> transactions;

    @JsonProperty("wage")
    private double wage;

    public ValidatorRequest() {
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @JsonSetter("transactions")
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @JsonSetter("expenses")
    public void setExpenses(List<Transaction> expenses) {
        this.transactions = expenses;
    }

    public double getWage() {
        return wage;
    }

    public void setWage(double wage) {
        this.wage = wage;
    }
}
