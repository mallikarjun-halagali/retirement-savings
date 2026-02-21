package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IndexResponse {

    @JsonProperty("invested")
    private double invested;

    @JsonProperty("returns")
    private double returns;

    @JsonProperty("profit")
    private double profit;

    @JsonProperty("inflationAdjusted")
    private double inflationAdjusted;

    public IndexResponse() {
    }

    public IndexResponse(double invested, double returns, double profit, double inflationAdjusted) {
        this.invested = invested;
        this.returns = returns;
        this.profit = profit;
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

    public double getInflationAdjusted() {
        return inflationAdjusted;
    }

    public void setInflationAdjusted(double inflationAdjusted) {
        this.inflationAdjusted = inflationAdjusted;
    }
}
