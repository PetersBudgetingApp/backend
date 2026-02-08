package com.peter.budget.scheduler;

import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.repository.RefreshTokenRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.service.RecurringDetectionService;
import com.peter.budget.service.simplefin.SimpleFinSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock
    private SimpleFinConnectionRepository connectionRepository;
    @Mock
    private SimpleFinSyncService syncService;
    @Mock
    private RecurringDetectionService recurringService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private SyncScheduler syncScheduler;

    @Test
    void morningSyncAllSyncsAllDueConnections() {
        SimpleFinConnection conn = connection(1L, 10L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn));

        syncScheduler.morningSyncAll();

        verify(syncService).syncConnection(10L, 1L);
    }

    @Test
    void eveningSyncAllSyncsAllDueConnections() {
        SimpleFinConnection conn1 = connection(1L, 10L);
        SimpleFinConnection conn2 = connection(2L, 20L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn1, conn2));

        syncScheduler.eveningSyncAll();

        verify(syncService).syncConnection(10L, 1L);
        verify(syncService).syncConnection(20L, 2L);
    }

    @Test
    void morningSyncContinuesAfterFailure() {
        SimpleFinConnection conn1 = connection(1L, 10L);
        SimpleFinConnection conn2 = connection(2L, 20L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn1, conn2));
        doThrow(new RuntimeException("API error")).when(syncService).syncConnection(10L, 1L);

        syncScheduler.morningSyncAll();

        verify(syncService).syncConnection(10L, 1L);
        verify(syncService).syncConnection(20L, 2L);
    }

    @Test
    void periodicSyncSyncsEligibleConnections() {
        SimpleFinConnection conn = connection(3L, 30L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn));

        syncScheduler.periodicSync();

        verify(syncService).syncConnection(30L, 3L);
    }

    @Test
    void periodicSyncContinuesAfterFailure() {
        SimpleFinConnection conn1 = connection(1L, 10L);
        SimpleFinConnection conn2 = connection(2L, 20L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn1, conn2));
        doThrow(new RuntimeException("Timeout")).when(syncService).syncConnection(10L, 1L);

        syncScheduler.periodicSync();

        verify(syncService).syncConnection(20L, 2L);
    }

    @Test
    void nightlyMaintenanceDeletesExpiredTokens() {
        syncScheduler.nightlyMaintenance();

        verify(refreshTokenRepository).deleteExpired();
    }

    @Test
    void weeklyRecurringDetectionRunsForAllConnections() {
        SimpleFinConnection conn1 = connection(1L, 10L);
        SimpleFinConnection conn2 = connection(2L, 20L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn1, conn2));
        when(recurringService.detectRecurringPatterns(10L)).thenReturn(3);
        when(recurringService.detectRecurringPatterns(20L)).thenReturn(1);

        syncScheduler.weeklyRecurringDetection();

        verify(recurringService).detectRecurringPatterns(10L);
        verify(recurringService).detectRecurringPatterns(20L);
    }

    @Test
    void weeklyRecurringDetectionContinuesAfterFailure() {
        SimpleFinConnection conn1 = connection(1L, 10L);
        SimpleFinConnection conn2 = connection(2L, 20L);
        when(connectionRepository.findDueForSync()).thenReturn(List.of(conn1, conn2));
        doThrow(new RuntimeException("DB error")).when(recurringService).detectRecurringPatterns(10L);
        when(recurringService.detectRecurringPatterns(20L)).thenReturn(2);

        syncScheduler.weeklyRecurringDetection();

        verify(recurringService).detectRecurringPatterns(10L);
        verify(recurringService).detectRecurringPatterns(20L);
    }

    @Test
    void syncHandlesEmptyConnectionList() {
        when(connectionRepository.findDueForSync()).thenReturn(List.of());

        syncScheduler.morningSyncAll();

        verify(syncService, never()).syncConnection(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    private SimpleFinConnection connection(Long id, Long userId) {
        return SimpleFinConnection.builder()
                .id(id)
                .userId(userId)
                .build();
    }
}
