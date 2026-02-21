package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QPeriod {

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    @JsonProperty("fixed")
    private double fixed;

    public QPeriod() {
    }

    public QPeriod(String start, String end, double fixed) {
        this.start = start;
        this.end = end;
        this.fixed = fixed;
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

    public double getFixed() {
        return fixed;
    }

    public void setFixed(double fixed) {
        this.fixed = fixed;
    }
}
