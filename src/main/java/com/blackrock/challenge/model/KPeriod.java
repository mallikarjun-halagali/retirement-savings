package com.blackrock.challenge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KPeriod {

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    public KPeriod() {
    }

    public KPeriod(String start, String end) {
        this.start = start;
        this.end = end;
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
}
