package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidTransaction extends Transaction {

    @JsonProperty("inKPeriod")
    private boolean inKPeriod;

    public ValidTransaction() {
    }

    public ValidTransaction(String date, double amount, double ceiling, double remanent, boolean inKPeriod) {
        super(date, amount, ceiling, remanent);
        this.inKPeriod = inKPeriod;
    }

    public boolean isInKPeriod() {
        return inKPeriod;
    }

    public void setInKPeriod(boolean inKPeriod) {
        this.inKPeriod = inKPeriod;
    }
}
