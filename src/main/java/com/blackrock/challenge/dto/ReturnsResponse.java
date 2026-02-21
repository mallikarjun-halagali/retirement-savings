package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ReturnsResponse {

    @JsonProperty("totalTransactionAmount")
    private double totalTransactionAmount;

    @JsonProperty("totalCeiling")
    private double totalCeiling;

    @JsonProperty("savingsByDates")
    private List<KPeriodSavings> savingsByDates;

    public ReturnsResponse() {
    }

    public ReturnsResponse(double totalTransactionAmount, double totalCeiling, List<KPeriodSavings> savingsByDates) {
        this.totalTransactionAmount = totalTransactionAmount;
        this.totalCeiling = totalCeiling;
        this.savingsByDates = savingsByDates;
    }

    public double getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public void setTotalTransactionAmount(double t) {
        this.totalTransactionAmount = t;
    }

    public double getTotalCeiling() {
        return totalCeiling;
    }

    public void setTotalCeiling(double t) {
        this.totalCeiling = t;
    }

    public List<KPeriodSavings> getSavingsByDates() {
        return savingsByDates;
    }

    public void setSavingsByDates(List<KPeriodSavings> s) {
        this.savingsByDates = s;
    }
}
