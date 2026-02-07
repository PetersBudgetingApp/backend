package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.dto.SyncResultDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.entity.Transaction;
import com.peter.budget.model.enums.AccountType;
import com.peter.budget.model.enums.SyncStatus;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.repository.TransactionRepository;
import com.peter.budget.service.AutoCategorizationService;
import com.peter.budget.service.EncryptionService;
import com.peter.budget.service.TransferDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleFinSyncService {

    private static final int MAX_DAILY_REQUESTS = 24;
    // SimpleFIN date range is limited, so keep initial backfill at 60 days.
    private static final int INITIAL_SYNC_DAYS = 60;
    private static final int OVERLAP_DAYS = 3;
    private static final int EMPTY_BACKFILL_WINDOWS_TO_COMPLETE = 12;
    private static final LocalDate HISTORY_CUTOFF_DATE = LocalDate.of(1970, 1, 1);

    private final SimpleFinClient simpleFinClient;
    private final SimpleFinConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EncryptionService encryptionService;
    private final AutoCategorizationService categorizationService;
    private final TransferDetectionService transferDetectionService;

    @Transactional
    public ConnectionDto setupConnection(Long userId, String setupToken) {
        String accessUrl = simpleFinClient.exchangeSetupToken(setupToken);

        var initialResponse = simpleFinClient.fetchAccounts(accessUrl);

        String institutionName = summarizeInstitutionNames(
                initialResponse.accounts().stream()
                        .map(SimpleFinClient.SimpleFinAccount::institutionName)
                        .toList(),
                null
        );

        SimpleFinConnection connection = SimpleFinConnection.builder()
                .userId(userId)
                .accessUrlEncrypted(encryptionService.encrypt(accessUrl))
                .institutionName(institutionName)
                .initialSyncCompleted(false)
                .backfillCursorDate(null)
                .syncStatus(SyncStatus.PENDING)
                .requestsToday(1)
                .requestsResetAt(Instant.now().plusSeconds(24 * 60 * 60))
                .build();

        connection = connectionRepository.save(connection);

        for (var sfAccount : initialResponse.accounts()) {
            createOrUpdateAccount(userId, connection.getId(), sfAccount);
        }

        connection.setInstitutionName(summarizeInstitutionNames(
                accountRepository.findByConnectionId(connection.getId()).stream()
                        .map(Account::getInstitutionName)
                        .toList(),
                connection.getInstitutionName()
        ));
        connection.setSyncStatus(SyncStatus.SUCCESS);
        // Keep this for UX freshness, while initialSyncCompleted=false ensures first real sync backfills history.
        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        int accountCount = accountRepository.countByConnectionId(connection.getId());

        return ConnectionDto.builder()
                .id(connection.getId())
                .institutionName(connection.getInstitutionName())
                .lastSyncAt(connection.getLastSyncAt())
                .syncStatus(connection.getSyncStatus())
                .accountCount(accountCount)
                .build();
    }

    @Transactional
    public SyncResultDto syncConnection(Long userId, Long connectionId) {
        SimpleFinConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> ApiException.notFound("Connection not found"));

        // Defensive normalization for rows created before/while backfill cursor logic was rolled out.
        if (connection.isInitialSyncCompleted() && connection.getBackfillCursorDate() != null) {
            connection.setInitialSyncCompleted(false);
        }

        if (!canMakeRequest(connection)) {
            throw ApiException.tooManyRequests("Daily request limit reached. Try again tomorrow.");
        }

        connection.setSyncStatus(SyncStatus.IN_PROGRESS);
        connectionRepository.save(connection);

        try {
            String accessUrl = encryptionService.decrypt(connection.getAccessUrlEncrypted());

            LocalDate incrementalStartDate = calculateStartDate(connection);
            LocalDate incrementalEndDate = LocalDate.now();

            int accountsSynced = 0;
            int transactionsAdded = 0;
            int transactionsUpdated = 0;

            consumeRequestQuota(connection, connectionId);
            var response = simpleFinClient.fetchAccounts(accessUrl, incrementalStartDate, incrementalEndDate);
            for (var sfAccount : response.accounts()) {
                Account account = createOrUpdateAccount(userId, connectionId, sfAccount);
                accountsSynced++;

                var result = syncTransactions(account, sfAccount.transactions());
                transactionsAdded += result.added();
                transactionsUpdated += result.updated();
            }

            if (!connection.isInitialSyncCompleted()) {
                LocalDate cursor = connection.getBackfillCursorDate();
                int emptyBackfillWindows = 0;
                if (cursor == null) {
                    LocalDate oldestSyncedDate = transactionRepository.findOldestPostedDateByConnectionId(connectionId);
                    cursor = oldestSyncedDate != null ? oldestSyncedDate : incrementalStartDate;
                }

                while (cursor.isAfter(HISTORY_CUTOFF_DATE) && canMakeRequest(connection)) {
                    LocalDate windowEnd = cursor;
                    LocalDate windowStart = windowEnd.minusDays(INITIAL_SYNC_DAYS);
                    int transactionsInWindow = 0;

                    consumeRequestQuota(connection, connectionId);
                    var historicalResponse = simpleFinClient.fetchAccounts(accessUrl, windowStart, windowEnd);

                    for (var sfAccount : historicalResponse.accounts()) {
                        transactionsInWindow += sfAccount.transactions().size();
                        Account account = createOrUpdateAccount(userId, connectionId, sfAccount);
                        accountsSynced++;

                        var result = syncTransactions(account, sfAccount.transactions());
                        transactionsAdded += result.added();
                        transactionsUpdated += result.updated();
                    }

                    cursor = windowStart;

                    if (transactionsInWindow == 0) {
                        emptyBackfillWindows++;
                        if (emptyBackfillWindows >= EMPTY_BACKFILL_WINDOWS_TO_COMPLETE) {
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
                } else if (!cursor.isAfter(HISTORY_CUTOFF_DATE)) {
                    connection.setInitialSyncCompleted(true);
                    connection.setBackfillCursorDate(null);
                } else {
                    connection.setBackfillCursorDate(cursor);
                }
            }

            int transfersDetected = transferDetectionService.detectTransfers(userId);

            connection.setInstitutionName(summarizeInstitutionNames(
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

        } catch (ApiException e) {
            log.warn("Sync failed for connection {}: {}", connectionId, e.getMessage());

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            throw e;
        } catch (Exception e) {
            log.error("Sync failed for connection {}", connectionId, e);

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            throw ApiException.internal("Sync failed: " + e.getMessage());
        }
    }

    public List<ConnectionDto> getConnections(Long userId) {
        return connectionRepository.findByUserId(userId).stream()
                .map(conn -> {
                    List<Account> accounts = accountRepository.findByConnectionId(conn.getId());
                    int accountCount = accounts.size();
                    String institutionName = summarizeInstitutionNames(
                            accounts.stream().map(Account::getInstitutionName).toList(),
                            conn.getInstitutionName()
                    );
                    return ConnectionDto.builder()
                            .id(conn.getId())
                            .institutionName(institutionName)
                            .lastSyncAt(conn.getLastSyncAt())
                            .syncStatus(conn.getSyncStatus())
                            .errorMessage(conn.getErrorMessage())
                            .accountCount(accountCount)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteConnection(Long userId, Long connectionId) {
        SimpleFinConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> ApiException.notFound("Connection not found"));

        accountRepository.deleteByConnectionId(connection.getId());
        connectionRepository.deleteById(connection.getId());
    }

    private boolean canMakeRequest(SimpleFinConnection connection) {
        if (connection.getRequestsResetAt() == null ||
                connection.getRequestsResetAt().isBefore(Instant.now())) {
            return true;
        }
        return connection.getRequestsToday() < MAX_DAILY_REQUESTS;
    }

    private LocalDate calculateStartDate(SimpleFinConnection connection) {
        if (connection.getLastSyncAt() == null) {
            return LocalDate.now().minusDays(INITIAL_SYNC_DAYS);
        }
        return connection.getLastSyncAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(OVERLAP_DAYS);
    }

    private void consumeRequestQuota(SimpleFinConnection connection, Long connectionId) {
        if (!canMakeRequest(connection)) {
            throw ApiException.tooManyRequests("Daily request limit reached. Try again tomorrow.");
        }

        connectionRepository.incrementRequestCount(connectionId);

        Instant now = Instant.now();
        if (connection.getRequestsResetAt() == null || connection.getRequestsResetAt().isBefore(now)) {
            connection.setRequestsToday(1);
            connection.setRequestsResetAt(now.plusSeconds(24 * 60 * 60));
            return;
        }

        connection.setRequestsToday(connection.getRequestsToday() + 1);
    }

    private String summarizeInstitutionNames(List<String> names, String fallback) {
        Set<String> unique = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                unique.add(name.trim());
            }
        }

        if (unique.isEmpty()) {
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
            return "Unknown institution";
        }

        if (unique.size() == 1) {
            return unique.iterator().next();
        }

        String first = unique.iterator().next();
        int remaining = unique.size() - 1;
        return first + " + " + remaining + " other" + (remaining == 1 ? "" : "s");
    }

    private Account createOrUpdateAccount(Long userId, Long connectionId,
                                           SimpleFinClient.SimpleFinAccount sfAccount) {
        Account account = accountRepository
                .findByConnectionIdAndExternalId(connectionId, sfAccount.id())
                .orElse(Account.builder()
                        .userId(userId)
                        .connectionId(connectionId)
                        .externalId(sfAccount.id())
                        .active(true)
                        .build());

        account.setName(sfAccount.name());
        account.setInstitutionName(sfAccount.institutionName());
        account.setAccountType(AccountType.valueOf(sfAccount.accountType()));
        account.setCurrency(sfAccount.currency() != null ? sfAccount.currency() : "USD");
        account.setCurrentBalance(sfAccount.balance());
        account.setAvailableBalance(sfAccount.availableBalance());
        account.setBalanceUpdatedAt(Instant.now());

        return accountRepository.save(account);
    }

    private SyncTransactionResult syncTransactions(Account account,
                                                    List<SimpleFinClient.SimpleFinTransaction> transactions) {
        int added = 0;
        int updated = 0;

        for (var sfTx : transactions) {
            var existing = transactionRepository
                    .findByAccountIdAndExternalId(account.getId(), sfTx.id());

            if (existing.isPresent()) {
                Transaction tx = existing.get();
                boolean changed = false;

                if (tx.isPending() != sfTx.pending()) {
                    tx.setPending(sfTx.pending());
                    changed = true;
                }
                if (!tx.getAmount().equals(sfTx.amount())) {
                    tx.setAmount(sfTx.amount());
                    changed = true;
                }

                if (changed) {
                    transactionRepository.save(tx);
                    updated++;
                }
            } else {
                Transaction tx = Transaction.builder()
                        .accountId(account.getId())
                        .externalId(sfTx.id())
                        .postedAt(sfTx.posted())
                        .transactedAt(sfTx.transacted())
                        .amount(sfTx.amount())
                        .pending(sfTx.pending())
                        .description(sfTx.description())
                        .payee(sfTx.payee())
                        .memo(sfTx.memo())
                        .build();

                Long categoryId = categorizationService.categorize(
                        account.getUserId(), sfTx.description(), sfTx.payee(), sfTx.memo());
                if (categoryId != null) {
                    tx.setCategoryId(categoryId);
                }

                transactionRepository.save(tx);
                added++;
            }
        }

        return new SyncTransactionResult(added, updated);
    }

    private record SyncTransactionResult(int added, int updated) {}
}
