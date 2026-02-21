package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QPeriod {

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    @JsonProperty("fixed")
    private long fixed;

    public QPeriod() {
    }

    public QPeriod(String start, String end, long fixed) {
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

    public long getFixed() {
        return fixed;
    }

    public void setFixed(long fixed) {
        this.fixed = fixed;
    }
}
