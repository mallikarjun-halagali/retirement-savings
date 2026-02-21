package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PPeriod {

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    @JsonProperty("extra")
    private double extra;

    public PPeriod() {
    }

    public PPeriod(String start, String end, double extra) {
        this.start = start;
        this.end = end;
        this.extra = extra;
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

    public double getExtra() {
        return extra;
    }

    public void setExtra(double extra) {
        this.extra = extra;
    }
}
