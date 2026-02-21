package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ValidatorResponse {

    @JsonProperty("valid")
    private List<Transaction> valid;

    @JsonProperty("invalid")
    private List<InvalidTransaction> invalid;

    public ValidatorResponse() {
    }

    public ValidatorResponse(List<Transaction> valid, List<InvalidTransaction> invalid) {
        this.valid = valid;
        this.invalid = invalid;
    }

    public List<Transaction> getValid() {
        return valid;
    }

    public void setValid(List<Transaction> valid) {
        this.valid = valid;
    }

    public List<InvalidTransaction> getInvalid() {
        return invalid;
    }

    public void setInvalid(List<InvalidTransaction> invalid) {
        this.invalid = invalid;
    }
}
