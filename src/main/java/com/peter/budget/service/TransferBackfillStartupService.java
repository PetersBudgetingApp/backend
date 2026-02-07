package com.peter.budget.service;

import com.peter.budget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferBackfillStartupService {

    private final UserRepository userRepository;
    private final TransferDetectionService transferDetectionService;

    @Value("${app.transfer.backfill-on-startup:false}")
    private boolean backfillOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void runBackfillOnStartup() {
        if (!backfillOnStartup) {
            return;
        }

        for (Long userId : userRepository.findAllUserIds()) {
            try {
                int detected = transferDetectionService.detectTransfers(userId);
                log.info("Transfer startup backfill complete for user {}: detected={}", userId, detected);
            } catch (Exception exception) {
                log.warn("Transfer startup backfill failed for user {}", userId, exception);
            }
        }
    }
}
