package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.Expense;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ParseRequest {

    @JsonProperty("expenses")
    private List<Expense> expenses;

    public ParseRequest() {
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }
}
