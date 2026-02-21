package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KPeriodSavings {

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("profit")
    private double profit;

    @JsonProperty("taxBenefit")
    private double taxBenefit;

    public KPeriodSavings() {
    }

    public KPeriodSavings(String start, String end, double amount, double profit, double taxBenefit) {
        this.start = start;
        this.end = end;
        this.amount = amount;
        this.profit = profit;
        this.taxBenefit = taxBenefit;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public double getTaxBenefit() {
        return taxBenefit;
    }

    public void setTaxBenefit(double taxBenefit) {
        this.taxBenefit = taxBenefit;
    }
}
