package com.peter.budget.service;

import com.peter.budget.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferBackfillStartupServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TransferDetectionService transferDetectionService;

    @InjectMocks
    private TransferBackfillStartupService startupService;

    @Test
    void runBackfillOnStartupSkipsWhenDisabled() {
        ReflectionTestUtils.setField(startupService, "backfillOnStartup", false);

        startupService.runBackfillOnStartup();

        verify(userRepository, never()).findAllUserIds();
        verify(transferDetectionService, never()).detectTransfers(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void runBackfillOnStartupProcessesAllUsers() {
        ReflectionTestUtils.setField(startupService, "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of(1L, 2L));
        when(transferDetectionService.detectTransfers(1L)).thenReturn(3);
        when(transferDetectionService.detectTransfers(2L)).thenReturn(0);

        startupService.runBackfillOnStartup();

        verify(transferDetectionService).detectTransfers(1L);
        verify(transferDetectionService).detectTransfers(2L);
    }

    @Test
    void runBackfillOnStartupContinuesAfterFailure() {
        ReflectionTestUtils.setField(startupService, "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("Error")).when(transferDetectionService).detectTransfers(1L);
        when(transferDetectionService.detectTransfers(2L)).thenReturn(1);

        startupService.runBackfillOnStartup();

        verify(transferDetectionService).detectTransfers(1L);
        verify(transferDetectionService).detectTransfers(2L);
    }

    @Test
    void runBackfillOnStartupHandlesEmptyUserList() {
        ReflectionTestUtils.setField(startupService, "backfillOnStartup", true);

        when(userRepository.findAllUserIds()).thenReturn(List.of());

        startupService.runBackfillOnStartup();

        verify(transferDetectionService, never()).detectTransfers(org.mockito.ArgumentMatchers.anyLong());
    }
}
