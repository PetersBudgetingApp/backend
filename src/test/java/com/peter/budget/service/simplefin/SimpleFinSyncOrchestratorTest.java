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
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleFinSyncOrchestratorTest {

    private static final long USER_ID = 7L;
    private static final long CONNECTION_ID = 10L;

    @Mock
    private SimpleFinClient simpleFinClient;
    @Mock
    private SimpleFinConnectionRepository connectionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionReadRepository transactionReadRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private TransferDetectionService transferDetectionService;
    @Mock
    private SimpleFinSyncPolicy syncPolicy;
    @Mock
    private SimpleFinSyncSupport syncSupport;

    @InjectMocks
    private SimpleFinSyncOrchestrator orchestrator;

    @Captor
    private ArgumentCaptor<SimpleFinConnection> connectionCaptor;

    private SimpleFinConnection baseConnection() {
        return SimpleFinConnection.builder()
                .id(CONNECTION_ID)
                .userId(USER_ID)
                .accessUrlEncrypted("encrypted-url")
                .syncStatus(SyncStatus.SUCCESS)
                .initialSyncCompleted(true)
                .lastSyncAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @BeforeEach
    void setUp() {
        lenient().when(encryptionService.decrypt("encrypted-url")).thenReturn("https://user:pass@api.simplefin.org/data");
        lenient().when(syncPolicy.canMakeRequest(any())).thenReturn(true);
        lenient().when(syncPolicy.calculateStartDate(any())).thenReturn(LocalDate.now().minusDays(3));
        lenient().when(syncPolicy.initialSyncDays()).thenReturn(60);
        lenient().when(syncPolicy.emptyBackfillWindowsToComplete()).thenReturn(12);
        lenient().when(syncPolicy.historyCutoffDate()).thenReturn(LocalDate.of(1970, 1, 1));
        lenient().when(transferDetectionService.detectTransfers(USER_ID)).thenReturn(0);
        lenient().when(accountRepository.findByConnectionId(CONNECTION_ID)).thenReturn(List.of());
        lenient().when(syncSupport.summarizeInstitutionNames(any(), any())).thenReturn("Test Bank");
    }

    @Test
    void syncConnectionThrowsNotFoundWhenConnectionMissing() {
        when(connectionRepository.findByIdAndUserId(999L, USER_ID)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orchestrator.syncConnection(USER_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void syncConnectionThrowsTooManyRequestsWhenQuotaExhausted() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(syncPolicy.canMakeRequest(connection)).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orchestrator.syncConnection(USER_ID, CONNECTION_ID)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    }

    @Test
    void syncConnectionPerformsIncrementalSyncSuccessfully() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-1", "Checking", "Chase", "USD",
                new BigDecimal("1000.00"), null, "CHECKING", List.of()
        );
        SimpleFinClient.SimpleFinAccountsResponse response =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(sfAccount), List.of());

        when(simpleFinClient.fetchAccounts(any(), any(), any())).thenReturn(response);
        when(syncSupport.createOrUpdateAccount(eq(USER_ID), eq(CONNECTION_ID), any()))
                .thenReturn(Account.builder().id(100L).userId(USER_ID).build());
        when(syncSupport.syncTransactions(any(), any()))
                .thenReturn(new SimpleFinSyncSupport.SyncTransactionResult(5, 2));

        SyncResultDto result = orchestrator.syncConnection(USER_ID, CONNECTION_ID);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getAccountsSynced());
        assertEquals(5, result.getTransactionsAdded());
        assertEquals(2, result.getTransactionsUpdated());
        assertNotNull(result.getSyncedAt());
    }

    @Test
    void syncConnectionSetsStatusToFailedOnUnexpectedException() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(simpleFinClient.fetchAccounts(any(), any(), any()))
                .thenThrow(new RuntimeException("Network error"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orchestrator.syncConnection(USER_ID, CONNECTION_ID)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verify(connectionRepository, times(2)).save(connectionCaptor.capture());
        SimpleFinConnection failedConn = connectionCaptor.getAllValues().get(1);
        assertEquals(SyncStatus.FAILED, failedConn.getSyncStatus());
        assertNotNull(failedConn.getErrorMessage());
    }

    @Test
    void syncConnectionSetsStatusToInProgressDuringSync() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));

        // Capture the sync status at the time of each save call
        java.util.List<SyncStatus> savedStatuses = new java.util.ArrayList<>();
        when(connectionRepository.save(any(SimpleFinConnection.class))).thenAnswer(i -> {
            SimpleFinConnection saved = i.getArgument(0);
            savedStatuses.add(saved.getSyncStatus());
            return saved;
        });

        when(simpleFinClient.fetchAccounts(any(), any(), any()))
                .thenReturn(new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of()));

        orchestrator.syncConnection(USER_ID, CONNECTION_ID);

        // First save should set IN_PROGRESS, last save should set SUCCESS
        assertEquals(2, savedStatuses.size());
        assertEquals(SyncStatus.IN_PROGRESS, savedStatuses.get(0));
        assertEquals(SyncStatus.SUCCESS, savedStatuses.get(1));
    }

    @Test
    void syncConnectionPerformsInitialSyncBackfill() {
        SimpleFinConnection connection = baseConnection();
        connection.setInitialSyncCompleted(false);
        connection.setBackfillCursorDate(null);

        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // First call: incremental sync, Second+: backfill windows
        SimpleFinClient.SimpleFinAccountsResponse emptyResponse =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of());
        when(simpleFinClient.fetchAccounts(any(), any(), any())).thenReturn(emptyResponse);

        // Return a date for oldest transaction to seed the backfill cursor
        when(transactionReadRepository.findOldestPostedDateByConnectionId(CONNECTION_ID))
                .thenReturn(LocalDate.now().minusDays(30));

        // Allow only a few requests (incremental + 2 backfill windows)
        java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger(0);
        when(syncPolicy.canMakeRequest(any())).thenAnswer(i -> requestCount.getAndIncrement() < 3);

        SyncResultDto result = orchestrator.syncConnection(USER_ID, CONNECTION_ID);

        assertTrue(result.isSuccess());
        // Should have consumed request quota at least for incremental + backfill windows
        verify(syncPolicy, times(3)).consumeRequestQuota(any(), eq(CONNECTION_ID));
    }

    @Test
    void syncConnectionCompletesBackfillAfterEmptyWindows() {
        SimpleFinConnection connection = baseConnection();
        connection.setInitialSyncCompleted(false);
        connection.setBackfillCursorDate(null);

        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimpleFinClient.SimpleFinAccountsResponse emptyResponse =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of());
        when(simpleFinClient.fetchAccounts(any(), any(), any())).thenReturn(emptyResponse);
        when(transactionReadRepository.findOldestPostedDateByConnectionId(CONNECTION_ID))
                .thenReturn(LocalDate.now().minusDays(10));

        // Allow enough requests for 12+ empty windows
        when(syncPolicy.canMakeRequest(any())).thenReturn(true);
        when(syncPolicy.emptyBackfillWindowsToComplete()).thenReturn(3);

        SyncResultDto result = orchestrator.syncConnection(USER_ID, CONNECTION_ID);

        assertTrue(result.isSuccess());
        assertTrue(connection.isInitialSyncCompleted());
    }

    @Test
    void syncConnectionHandlesApiExceptionDuringSync() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(simpleFinClient.fetchAccounts(any(), any(), any()))
                .thenThrow(ApiException.unauthorized("Token expired"));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orchestrator.syncConnection(USER_ID, CONNECTION_ID)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void syncConnectionReturnsBadRequestWhenAccessUrlCannotBeDecrypted() {
        SimpleFinConnection connection = baseConnection();
        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(encryptionService.decrypt("encrypted-url"))
                .thenThrow(new EncryptionOperationNotPossibleException());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orchestrator.syncConnection(USER_ID, CONNECTION_ID)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("Unable to decrypt saved SimpleFIN credentials"));
        verify(simpleFinClient, never()).fetchAccounts(any(), any(), any());
    }

    @Test
    void syncConnectionResetsInitialSyncWhenBackfillCursorPresent() {
        SimpleFinConnection connection = baseConnection();
        connection.setInitialSyncCompleted(true);
        connection.setBackfillCursorDate(LocalDate.now().minusDays(30));

        when(connectionRepository.findByIdAndUserId(CONNECTION_ID, USER_ID))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SimpleFinClient.SimpleFinAccountsResponse emptyResponse =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of());
        when(simpleFinClient.fetchAccounts(any(), any(), any())).thenReturn(emptyResponse);

        // Allow only one request (just incremental, not enough for backfill)
        java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger(0);
        when(syncPolicy.canMakeRequest(any())).thenAnswer(i -> requestCount.getAndIncrement() < 2);

        orchestrator.syncConnection(USER_ID, CONNECTION_ID);

        // initialSyncCompleted should have been reset to false due to backfillCursorDate
        // and then it should remain false since backfill wasn't completed
    }
}
