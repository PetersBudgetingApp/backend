package com.peter.budget.service;

import com.peter.budget.model.dto.CategorizationRuleBackfillResultDto;
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
public class CategorizationRuleBackfillStartupService {

    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Value("${app.categorization.backfill-on-startup:false}")
    private boolean backfillOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void runBackfillOnStartup() {
        if (!backfillOnStartup) {
            return;
        }

        for (Long userId : userRepository.findAllUserIds()) {
            try {
                CategorizationRuleBackfillResultDto result = transactionService.backfillCategorizationRules(userId);
                log.info(
                        "Categorization rule startup backfill complete for user {}: total={}, eligible={}, matched={}, updated={}",
                        userId,
                        result.getTotalTransactions(),
                        result.getEligibleTransactions(),
                        result.getMatchedTransactions(),
                        result.getUpdatedTransactions()
                );
            } catch (Exception exception) {
                log.warn("Categorization rule startup backfill failed for user {}", userId, exception);
            }
        }
    }
}
