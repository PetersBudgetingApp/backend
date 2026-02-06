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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleFinSyncService {

    private static final int MAX_DAILY_REQUESTS = 24;
    private static final int INITIAL_SYNC_DAYS = 90;
    private static final int OVERLAP_DAYS = 3;

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

        String institutionName = null;
        if (!initialResponse.accounts().isEmpty()) {
            institutionName = initialResponse.accounts().get(0).institutionName();
        }

        SimpleFinConnection connection = SimpleFinConnection.builder()
                .userId(userId)
                .accessUrlEncrypted(encryptionService.encrypt(accessUrl))
                .institutionName(institutionName)
                .syncStatus(SyncStatus.PENDING)
                .requestsToday(1)
                .requestsResetAt(Instant.now().plusSeconds(24 * 60 * 60))
                .build();

        connection = connectionRepository.save(connection);

        for (var sfAccount : initialResponse.accounts()) {
            createOrUpdateAccount(userId, connection.getId(), sfAccount);
        }

        connection.setSyncStatus(SyncStatus.SUCCESS);
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

        if (!canMakeRequest(connection)) {
            throw ApiException.tooManyRequests("Daily request limit reached. Try again tomorrow.");
        }

        connection.setSyncStatus(SyncStatus.IN_PROGRESS);
        connectionRepository.save(connection);

        try {
            String accessUrl = encryptionService.decrypt(connection.getAccessUrlEncrypted());

            LocalDate startDate = calculateStartDate(connection);
            LocalDate endDate = LocalDate.now();

            var response = simpleFinClient.fetchAccounts(accessUrl, startDate, endDate);

            connectionRepository.incrementRequestCount(connectionId);

            int accountsSynced = 0;
            int transactionsAdded = 0;
            int transactionsUpdated = 0;

            for (var sfAccount : response.accounts()) {
                Account account = createOrUpdateAccount(userId, connectionId, sfAccount);
                accountsSynced++;

                var result = syncTransactions(account, sfAccount.transactions());
                transactionsAdded += result.added();
                transactionsUpdated += result.updated();
            }

            int transfersDetected = transferDetectionService.detectTransfers(userId);

            connection.setSyncStatus(SyncStatus.SUCCESS);
            connection.setLastSyncAt(Instant.now());
            connection.setErrorMessage(null);
            connectionRepository.save(connection);

            return SyncResultDto.builder()
                    .success(true)
                    .message("Sync completed successfully")
                    .accountsSynced(accountsSynced)
                    .transactionsAdded(transactionsAdded)
                    .transactionsUpdated(transactionsUpdated)
                    .transfersDetected(transfersDetected)
                    .syncedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Sync failed for connection {}", connectionId, e);

            connection.setSyncStatus(SyncStatus.FAILED);
            connection.setErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            return SyncResultDto.builder()
                    .success(false)
                    .message("Sync failed: " + e.getMessage())
                    .syncedAt(Instant.now())
                    .build();
        }
    }

    public List<ConnectionDto> getConnections(Long userId) {
        return connectionRepository.findByUserId(userId).stream()
                .map(conn -> {
                    int accountCount = accountRepository.countByConnectionId(conn.getId());
                    return ConnectionDto.builder()
                            .id(conn.getId())
                            .institutionName(conn.getInstitutionName())
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
