package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.Expense;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ValidatorResponse {

    @JsonProperty("valid")
    private List<Expense> valid;

    @JsonProperty("invalid")
    private List<Expense> invalid;

    public ValidatorResponse() {
    }

    public ValidatorResponse(List<Expense> valid, List<Expense> invalid) {
        this.valid = valid;
        this.invalid = invalid;
    }

    public List<Expense> getValid() {
        return valid;
    }

    public void setValid(List<Expense> valid) {
        this.valid = valid;
    }

    public List<Expense> getInvalid() {
        return invalid;
    }

    public void setInvalid(List<Expense> invalid) {
        this.invalid = invalid;
    }
}
