package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.Expense;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ValidatorRequest {

    @JsonProperty("expenses")
    private List<Expense> expenses;

    @JsonProperty("wage")
    private long wage;

    public ValidatorRequest() {
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public long getWage() {
        return wage;
    }

    public void setWage(long wage) {
        this.wage = wage;
    }
}
