package com.peter.budget.scheduler;

import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.repository.RefreshTokenRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.service.RecurringDetectionService;
import com.peter.budget.service.simplefin.SimpleFinSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final SimpleFinConnectionRepository connectionRepository;
    private final SimpleFinSyncService syncService;
    private final RecurringDetectionService recurringService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 30 6 * * *")
    public void morningSyncAll() {
        log.info("Starting morning sync for all connections");
        syncAllConnections();
    }

    @Scheduled(cron = "0 0 20 * * *")
    public void eveningSyncAll() {
        log.info("Starting evening sync for all connections");
        syncAllConnections();
    }

    @Scheduled(fixedRate = 4 * 60 * 60 * 1000)
    public void periodicSync() {
        log.info("Starting periodic sync for eligible connections");
        syncEligibleConnections();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyMaintenance() {
        log.info("Running nightly maintenance tasks");

        refreshTokenRepository.deleteExpired();
        log.info("Cleaned up expired refresh tokens");
    }

    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyRecurringDetection() {
        log.info("Running weekly recurring pattern detection");
        List<SimpleFinConnection> connections = connectionRepository.findDueForSync();

        for (SimpleFinConnection connection : connections) {
            try {
                int detected = recurringService.detectRecurringPatterns(connection.getUserId());
                log.info("Detected {} recurring patterns for user {}", detected, connection.getUserId());
            } catch (Exception e) {
                log.error("Error detecting patterns for user {}", connection.getUserId(), e);
            }
        }
    }

    private void syncAllConnections() {
        List<SimpleFinConnection> connections = connectionRepository.findDueForSync();
        log.info("Found {} connections to sync", connections.size());

        for (SimpleFinConnection connection : connections) {
            try {
                syncService.syncConnection(connection.getUserId(), connection.getId());
                log.info("Successfully synced connection {} for user {}",
                        connection.getId(), connection.getUserId());
            } catch (Exception e) {
                log.error("Failed to sync connection {} for user {}",
                        connection.getId(), connection.getUserId(), e);
            }
        }
    }

    private void syncEligibleConnections() {
        List<SimpleFinConnection> connections = connectionRepository.findDueForSync();
        log.info("Found {} eligible connections for periodic sync", connections.size());

        for (SimpleFinConnection connection : connections) {
            try {
                syncService.syncConnection(connection.getUserId(), connection.getId());
                log.info("Successfully synced connection {} for user {}",
                        connection.getId(), connection.getUserId());
            } catch (Exception e) {
                log.error("Failed to sync connection {} for user {}",
                        connection.getId(), connection.getUserId(), e);
            }
        }
    }
}
