package com.peter.budget.service;

import com.peter.budget.model.dto.CategorizationRuleBackfillResultDto;
import com.peter.budget.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizationRuleBackfillStartupServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CategorizationRuleBackfillStartupService startupService;

    @Test
    void runBackfillOnStartupSkipsWhenDisabled() {
        ReflectionTestUtils.setField(Objects.requireNonNull(startupService), "backfillOnStartup", false);

        startupService.runBackfillOnStartup();

        verify(userRepository, never()).findAllUserIds();
        verify(transactionService, never()).backfillCategorizationRules(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void runBackfillOnStartupProcessesAllUsers() {
        ReflectionTestUtils.setField(Objects.requireNonNull(startupService), "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of(1L, 2L, 3L));
        when(transactionService.backfillCategorizationRules(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(CategorizationRuleBackfillResultDto.builder()
                        .totalTransactions(10)
                        .eligibleTransactions(5)
                        .matchedTransactions(3)
                        .updatedTransactions(2)
                        .build());

        startupService.runBackfillOnStartup();

        verify(transactionService).backfillCategorizationRules(1L);
        verify(transactionService).backfillCategorizationRules(2L);
        verify(transactionService).backfillCategorizationRules(3L);
    }

    @Test
    void runBackfillOnStartupContinuesAfterFailure() {
        ReflectionTestUtils.setField(Objects.requireNonNull(startupService), "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("DB error")).when(transactionService).backfillCategorizationRules(1L);
        when(transactionService.backfillCategorizationRules(2L))
                .thenReturn(CategorizationRuleBackfillResultDto.builder()
                        .totalTransactions(5)
                        .eligibleTransactions(3)
                        .matchedTransactions(2)
                        .updatedTransactions(1)
                        .build());

        startupService.runBackfillOnStartup();

        verify(transactionService).backfillCategorizationRules(1L);
        verify(transactionService).backfillCategorizationRules(2L);
    }

    @Test
    void runBackfillOnStartupHandlesEmptyUserList() {
        ReflectionTestUtils.setField(Objects.requireNonNull(startupService), "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of());

        startupService.runBackfillOnStartup();

        verify(transactionService, never()).backfillCategorizationRules(org.mockito.ArgumentMatchers.anyLong());
    }
}
