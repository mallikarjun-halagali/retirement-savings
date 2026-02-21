package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IndexRequest {

    @JsonProperty("invested")
    private double invested;

    @JsonProperty("age")
    private int age;

    @JsonProperty("inflation")
    private double inflation;

    public IndexRequest() {
    }

    public double getInvested() {
        return invested;
    }

    public void setInvested(double invested) {
        this.invested = invested;
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
