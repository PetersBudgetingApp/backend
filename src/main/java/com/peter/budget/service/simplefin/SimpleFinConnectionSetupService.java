package com.peter.budget.service.simplefin;

import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.enums.SyncStatus;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SimpleFinConnectionSetupService {

    private final SimpleFinClient simpleFinClient;
    private final SimpleFinConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final EncryptionService encryptionService;
    private final SimpleFinSyncSupport syncSupport;

    @Transactional
    public ConnectionDto setupConnection(Long userId, String setupToken) {
        String accessUrl = simpleFinClient.exchangeSetupToken(setupToken);
        var initialResponse = simpleFinClient.fetchAccounts(accessUrl);

        String institutionName = syncSupport.summarizeInstitutionNames(
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
            Account account = syncSupport.createOrUpdateAccount(userId, connection.getId(), sfAccount);
            syncSupport.syncTransactions(account, sfAccount.transactions());
        }

        connection.setInstitutionName(syncSupport.summarizeInstitutionNames(
                accountRepository.findByConnectionId(connection.getId()).stream()
                        .map(Account::getInstitutionName)
                        .toList(),
                connection.getInstitutionName()
        ));
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
}
