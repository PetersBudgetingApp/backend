package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.dto.SyncResultDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimpleFinSyncService {

    private final SimpleFinConnectionSetupService setupService;
    private final SimpleFinSyncOrchestrator syncOrchestrator;
    private final SimpleFinConnectionRepository connectionRepository;
    private final AccountRepository accountRepository;
    private final SimpleFinSyncSupport syncSupport;

    @Transactional
    public ConnectionDto setupConnection(Long userId, String setupToken) {
        return setupService.setupConnection(userId, setupToken);
    }

    @Transactional
    public SyncResultDto syncConnection(Long userId, Long connectionId) {
        return syncConnection(userId, connectionId, false);
    }

    @Transactional
    public SyncResultDto syncConnection(Long userId, Long connectionId, boolean fullSync) {
        return syncOrchestrator.syncConnection(userId, connectionId, fullSync);
    }

    public List<ConnectionDto> getConnections(Long userId) {
        return connectionRepository.findByUserId(userId).stream()
                .map(connection -> {
                    List<Account> accounts = accountRepository.findByConnectionId(connection.getId());
                    int accountCount = accounts.size();
                    String institutionName = syncSupport.summarizeInstitutionNames(
                            accounts.stream().map(Account::getInstitutionName).toList(),
                            connection.getInstitutionName()
                    );
                    return ConnectionDto.builder()
                            .id(connection.getId())
                            .institutionName(institutionName)
                            .lastSyncAt(connection.getLastSyncAt())
                            .syncStatus(connection.getSyncStatus())
                            .errorMessage(connection.getErrorMessage())
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
}
