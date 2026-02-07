package com.peter.budget.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransactionUpdateRequest {
    private Long categoryId;
    @JsonIgnore
    private boolean categoryIdProvided;
    private String notes;
    private Boolean excludeFromTotals;

    @JsonSetter("categoryId")
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        this.categoryIdProvided = true;
    }
}
