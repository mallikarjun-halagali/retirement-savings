package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NpsResponse {

    @JsonProperty("invested")
    private double invested;

    @JsonProperty("returns")
    private double returns;

    @JsonProperty("profit")
    private double profit;

    @JsonProperty("taxBenefit")
    private double taxBenefit;

    @JsonProperty("inflationAdjusted")
    private double inflationAdjusted;

    public NpsResponse() {
    }

    public NpsResponse(double invested, double returns, double profit, double taxBenefit, double inflationAdjusted) {
        this.invested = invested;
        this.returns = returns;
        this.profit = profit;
        this.taxBenefit = taxBenefit;
        this.inflationAdjusted = inflationAdjusted;
    }

    public double getInvested() {
        return invested;
    }

    public void setInvested(double invested) {
        this.invested = invested;
    }

    public double getReturns() {
        return returns;
    }

    public void setReturns(double returns) {
        this.returns = returns;
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

    public double getInflationAdjusted() {
        return inflationAdjusted;
    }

    public void setInflationAdjusted(double inflationAdjusted) {
        this.inflationAdjusted = inflationAdjusted;
    }
}
