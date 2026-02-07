package com.peter.budget.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doesNotMarkCategoryProvidedWhenFieldIsAbsent() throws Exception {
        TransactionUpdateRequest request = objectMapper.readValue("{}", TransactionUpdateRequest.class);

        assertFalse(request.isCategoryIdProvided());
        assertNull(request.getCategoryId());
    }

    @Test
    void marksCategoryProvidedWhenFieldIsExplicitlyNull() throws Exception {
        TransactionUpdateRequest request = objectMapper.readValue("{\"categoryId\":null}", TransactionUpdateRequest.class);

        assertTrue(request.isCategoryIdProvided());
        assertNull(request.getCategoryId());
    }
}
