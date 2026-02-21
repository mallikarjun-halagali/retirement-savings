package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

public class FilterRequest {

    private List<Expense> expenses;

    @JsonProperty("q")
    private List<QPeriod> q;

    @JsonProperty("p")
    private List<PPeriod> p;

    @JsonProperty("k")
    private List<KPeriod> k;

    @JsonProperty("age")
    private int age;

    @JsonProperty("wage")
    private double wage;

    @JsonProperty("inflation")
    private double inflation;

    public FilterRequest() {
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    // Accept both "expenses" and "transactions" as field name
    @JsonSetter("expenses")
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @JsonSetter("transactions")
    public void setTransactions(List<Expense> transactions) {
        this.expenses = transactions;
    }

    public List<QPeriod> getQ() {
        return q;
    }

    public void setQ(List<QPeriod> q) {
        this.q = q;
    }

    public List<PPeriod> getP() {
        return p;
    }

    public void setP(List<PPeriod> p) {
        this.p = p;
    }

    public List<KPeriod> getK() {
        return k;
    }

    public void setK(List<KPeriod> k) {
        this.k = k;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWage() {
        return wage;
    }

    public void setWage(double wage) {
        this.wage = wage;
    }

    public double getInflation() {
        return inflation;
    }

    public void setInflation(double inflation) {
        this.inflation = inflation;
    }
}
