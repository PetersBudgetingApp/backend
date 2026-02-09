package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.SyncResultDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.enums.SyncStatus;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.repository.TransactionReadRepository;
import com.peter.budget.service.EncryptionService;
import com.peter.budget.service.TransferDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleFinSyncOrchestrator {
    private static final String SIMPLEFIN_CREDENTIALS_ERROR =
            "Unable to decrypt saved SimpleFIN credentials. Reconnect this institution or restore the original ENCRYPTION_SECRET.";

    private final SimpleFinClient simpleFinClient;
    private final SimpleFinConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final TransactionReadRepository transactionReadRepository;
    private final EncryptionService encryptionService;
    private final TransferDetectionService transferDetectionService;
    private final SimpleFinSyncPolicy syncPolicy;
    private final SimpleFinSyncSupport syncSupport;

    @Transactional
    public SyncResultDto syncConnection(Long userId, Long connectionId) {
        SimpleFinConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> ApiException.notFound("Connection not found"));

        if (connection.isInitialSyncCompleted() && connection.getBackfillCursorDate() != null) {
            connection.setInitialSyncCompleted(false);
        }

        if (!syncPolicy.canMakeRequest(connection)) {
            throw ApiException.tooManyRequests("Daily request limit reached. Try again tomorrow.");
        }

        connection.setSyncStatus(SyncStatus.IN_PROGRESS);
        connectionRepository.save(connection);

        try {
            String accessUrl = encryptionService.decrypt(connection.getAccessUrlEncrypted());

            LocalDate incrementalStartDate = syncPolicy.calculateStartDate(connection);
            LocalDate incrementalEndDate = LocalDate.now();

            int accountsSynced = 0;
            int transactionsAdded = 0;
            int transactionsUpdated = 0;

            syncPolicy.consumeRequestQuota(connection, connectionId);
            var response = simpleFinClient.fetchAccounts(accessUrl, incrementalStartDate, incrementalEndDate);
            for (var sfAccount : response.accounts()) {
                Account account = syncSupport.createOrUpdateAccount(userId, connectionId, sfAccount);
                accountsSynced++;

                var result = syncSupport.syncTransactions(account, sfAccount.transactions());
                transactionsAdded += result.added();
                transactionsUpdated += result.updated();
            }

            if (!connection.isInitialSyncCompleted()) {
                LocalDate cursor = connection.getBackfillCursorDate();
                int emptyBackfillWindows = 0;
                if (cursor == null) {
                    LocalDate oldestSyncedDate = transactionReadRepository.findOldestPostedDateByConnectionId(connectionId);
                    cursor = oldestSyncedDate != null ? oldestSyncedDate : incrementalStartDate;
                }

                while (cursor.isAfter(syncPolicy.historyCutoffDate()) && syncPolicy.canMakeRequest(connection)) {
                    LocalDate windowEnd = cursor;
                    LocalDate windowStart = windowEnd.minusDays(syncPolicy.initialSyncDays());
                    int transactionsInWindow = 0;

                    syncPolicy.consumeRequestQuota(connection, connectionId);
                    var historicalResponse = simpleFinClient.fetchAccounts(accessUrl, windowStart, windowEnd);

                    for (var sfAccount : historicalResponse.accounts()) {
                        transactionsInWindow += sfAccount.transactions().size();
                        Account account = syncSupport.createOrUpdateAccount(userId, connectionId, sfAccount);
                        accountsSynced++;

                        var result = syncSupport.syncTransactions(account, sfAccount.transactions());
                        transactionsAdded += result.added();
                        transactionsUpdated += result.updated();
                    }

                    cursor = windowStart;

                    if (transactionsInWindow == 0) {
                        emptyBackfillWindows++;
                        if (emptyBackfillWindows >= syncPolicy.emptyBackfillWindowsToComplete()) {
                            connection.setInitialSyncCompleted(true);
                            connection.setBackfillCursorDate(null);
                            break;
                        }
                    } else {
                        emptyBackfillWindows = 0;
                    }
                }

                if (connection.isInitialSyncCompleted()) {
                    // completion state already set in-loop; keep it stable.
                } else if (!cursor.isAfter(syncPolicy.historyCutoffDate())) {
                    connection.setInitialSyncCompleted(true);
                    connection.setBackfillCursorDate(null);
                } else {
                    connection.setBackfillCursorDate(cursor);
                }
            }

            int transfersDetected = transferDetectionService.detectTransfers(userId);

            connection.setInstitutionName(syncSupport.summarizeInstitutionNames(
                    accountRepository.findByConnectionId(connection.getId()).stream()
                            .map(Account::getInstitutionName)
                            .toList(),
                    connection.getInstitutionName()
            ));
            connection.setSyncStatus(SyncStatus.SUCCESS);
            connection.setLastSyncAt(Instant.now());
            connection.setErrorMessage(null);
            connectionRepository.save(connection);

            return SyncResultDto.builder()
                    .success(true)
                    .message(connection.isInitialSyncCompleted()
                            ? "Sync completed successfully"
                            : "Sync completed. Historical backfill is still in progress.")
                    .accountsSynced(accountsSynced)
                    .transactionsAdded(transactionsAdded)
                    .transactionsUpdated(transactionsUpdated)
                    .transfersDetected(transfersDetected)
                    .syncedAt(Instant.now())
                    .build();
        } catch (EncryptionOperationNotPossibleException e) {
            log.warn("Sync failed for connection {}: encrypted access URL could not be decrypted", connectionId);

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(SIMPLEFIN_CREDENTIALS_ERROR);
            connectionRepository.save(connection);

            throw ApiException.badRequest(SIMPLEFIN_CREDENTIALS_ERROR);
        } catch (ApiException e) {
            log.warn("Sync failed for connection {}: {}", connectionId, e.getMessage());

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            throw e;
        } catch (Exception e) {
            log.error("Sync failed for connection {}", connectionId, e);
            String safeMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(safeMessage);
            connectionRepository.save(connection);

            throw ApiException.internal("Sync failed: " + safeMessage);
        }
    }
}
