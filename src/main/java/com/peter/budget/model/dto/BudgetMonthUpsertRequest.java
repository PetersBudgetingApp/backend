package com.peter.budget.model.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetMonthUpsertRequest {
    @Valid
    @Builder.Default
    private List<BudgetTargetUpsertRequest> targets = new ArrayList<>();
}
