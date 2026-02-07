package com.peter.budget.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkTransferRequest {
    @NotNull(message = "pairTransactionId is required")
    private Long pairTransactionId;
}
