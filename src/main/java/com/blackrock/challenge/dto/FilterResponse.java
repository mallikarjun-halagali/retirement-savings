package com.blackrock.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FilterResponse {

    @JsonProperty("savingsByDates")
    private List<Long> savingsByDates;

    @JsonProperty("totalSavings")
    private long totalSavings;

    public FilterResponse() {
    }

    public FilterResponse(List<Long> savingsByDates, long totalSavings) {
        this.savingsByDates = savingsByDates;
        this.totalSavings = totalSavings;
    }

    public List<Long> getSavingsByDates() {
        return savingsByDates;
    }

    public void setSavingsByDates(List<Long> savingsByDates) {
        this.savingsByDates = savingsByDates;
    }

    public long getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(long totalSavings) {
        this.totalSavings = totalSavings;
    }
}
