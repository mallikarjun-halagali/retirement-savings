package com.blackrock.challenge.dto;

import com.blackrock.challenge.model.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FilterResponse {

    @JsonProperty("valid")
    private List<ValidTransaction> valid;

    @JsonProperty("invalid")
    private List<InvalidTransaction> invalid;

    public FilterResponse() {
    }

    public FilterResponse(List<ValidTransaction> valid, List<InvalidTransaction> invalid) {
        this.valid = valid;
        this.invalid = invalid;
    }

    public List<ValidTransaction> getValid() {
        return valid;
    }

    public void setValid(List<ValidTransaction> valid) {
        this.valid = valid;
    }

    public List<InvalidTransaction> getInvalid() {
        return invalid;
    }

    public void setInvalid(List<InvalidTransaction> invalid) {
        this.invalid = invalid;
    }
}
