package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FilterResponse {

    @JsonProperty("totalTransactionAmount")
    private double totalTransactionAmount;

    @JsonProperty("totalCeiling")
    private double totalCeiling;

    @JsonProperty("savingsByDates")
    private List<KPeriodSavings> savingsByDates;

    public FilterResponse() {
    }

    public FilterResponse(double totalTransactionAmount, double totalCeiling, List<KPeriodSavings> savingsByDates) {
        this.totalTransactionAmount = totalTransactionAmount;
        this.totalCeiling = totalCeiling;
        this.savingsByDates = savingsByDates;
    }

    public double getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public void setTotalTransactionAmount(double totalTransactionAmount) {
        this.totalTransactionAmount = totalTransactionAmount;
    }

    public double getTotalCeiling() {
        return totalCeiling;
    }

    public void setTotalCeiling(double totalCeiling) {
        this.totalCeiling = totalCeiling;
    }

    public List<KPeriodSavings> getSavingsByDates() {
        return savingsByDates;
    }

    public void setSavingsByDates(List<KPeriodSavings> savingsByDates) {
        this.savingsByDates = savingsByDates;
    }
}
