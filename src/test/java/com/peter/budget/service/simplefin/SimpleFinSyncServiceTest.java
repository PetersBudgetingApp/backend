package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.dto.SyncResultDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.enums.SyncStatus;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleFinSyncServiceTest {

    private static final long USER_ID = 7L;
    private static final long CONNECTION_ID = 10L;

    @Mock
    private SimpleFinConnectionSetupService setupService;
    @Mock
    private SimpleFinSyncOrchestrator syncOrchestrator;
    @Mock
    private SimpleFinConnectionRepository connectionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SimpleFinSyncSupport syncSupport;

    @InjectMocks
    private SimpleFinSyncService syncService;

    @Test
    void setupConnectionDelegatesToSetupService() {
        ConnectionDto expected = ConnectionDto.builder()
                .id(CONNECTION_ID).institutionName("Chase").build();
        when(setupService.setupConnection(USER_ID, "setup-token")).thenReturn(expected);

        ConnectionDto result = syncService.setupConnection(USER_ID, "setup-token");

        assertEquals(CONNECTION_ID, result.getId());
        verify(setupService).setupConnection(USER_ID, "setup-token");
    }

    @Test
    void syncConnectionDelegatesToOrchestrator() {
        SyncResultDto expected = SyncResultDto.builder()
                .success(true).accountsSynced(2).transactionsAdded(10).build();
        when(syncOrchestrator.syncConnection(USER_ID, CONNECTION_ID)).thenReturn(expected);

        SyncResultDto result = syncService.syncConnection(USER_ID, CONNECTION_ID);

        assertEquals(true, result.isSuccess());
        assertEquals(2, result.getAccountsSynced());
        verify(syncOrchestrator).syncConnection(USER_ID, CONNECTION_ID);
    }

    @Test
    void getConnectionsReturnsConnectionDtos() {
        SimpleFinConnection conn = SimpleFinConnection.builder()
                .id(CONNECTION_ID)
                .userId(USER_ID)
                .institutionName("Default Bank")
                .syncStatus(SyncStatus.SUCCESS)
                .lastSyncAt(Instant.now())
                .build();

        Account acct1 = Account.builder()
                .id(1L).connectionId(CONNECTION_ID).institutionName("Chase").build();
        Account acct2 = Account.builder()
                .id(2L).connectionId(CONNECTION_ID).institutionName("Chase").build();

        when(connectionRepository.findByUserId(USER_ID)).thenReturn(List.of(conn));
        when(accountRepository.findByConnectionId(CONNECTION_ID)).thenReturn(List.of(acct1, acct2));
        when(syncSupport.summarizeInstitutionNames(List.of("Chase", "Chase"), "Default Bank"))
                .thenReturn("Chase");

        List<ConnectionDto> result = syncService.getConnections(USER_ID);

        assertEquals(1, result.size());
        assertEquals("Chase", result.get(0).getInstitutionName());
        assertEquals(2, result.get(0).getAccountCount());
        assertEquals(SyncStatus.SUCCESS, result.get(0).getSyncStatus());
    }

    @Test
    void getConnectionsReturnsEmptyListWhenNoConnections() {
        when(connectionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<ConnectionDto> result = syncService.getConnections(USER_ID);

        assertEquals(0, result.size());
    }

    @Test
    void deleteConnectionDeletesConnectionAndAccounts() {
        SimpleFinConnection conn = SimpleFinConnection.builder()
                .id(CONNECTION_ID).userId(USER_ID).build();

        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(conn));

        syncService.deleteConnection(USER_ID, CONNECTION_ID);

        verify(accountRepository).deleteByConnectionId(CONNECTION_ID);
        verify(connectionRepository).deleteById(CONNECTION_ID);
    }

    @Test
    void deleteConnectionThrowsNotFoundWhenMissing() {
        when(connectionRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> syncService.deleteConnection(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
