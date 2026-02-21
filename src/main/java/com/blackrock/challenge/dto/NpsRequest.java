package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NpsRequest {

    @JsonProperty("invested")
    private double invested;

    @JsonProperty("wage")
    private double wage;

    @JsonProperty("age")
    private int age;

    @JsonProperty("inflation")
    private double inflation;

    public NpsRequest() {
    }

    public double getInvested() {
        return invested;
    }

    public void setInvested(double invested) {
        this.invested = invested;
    }

    public double getWage() {
        return wage;
    }

    public void setWage(double wage) {
        this.wage = wage;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getInflation() {
        return inflation;
    }

    public void setInflation(double inflation) {
        this.inflation = inflation;
    }
}
