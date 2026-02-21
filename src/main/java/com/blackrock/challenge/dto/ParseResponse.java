package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ParseResponse {

    @JsonProperty("transactions")
    private List<Transaction> transactions;

    public ParseResponse() {
    }

    public ParseResponse(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
