package com.peter.budget.service.simplefin;

import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.entity.Account;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.model.enums.SyncStatus;
import com.peter.budget.repository.AccountRepository;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import com.peter.budget.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleFinConnectionSetupServiceTest {

    private static final long USER_ID = 7L;
    private static final long CONNECTION_ID = 42L;
    private static final String SETUP_TOKEN = "dGVzdC10b2tlbg==";
    private static final String ACCESS_URL = "https://user:pass@bridge.simplefin.org/data";
    private static final String ENCRYPTED_URL = "encrypted-url-value";

    @Mock
    private SimpleFinClient simpleFinClient;
    @Mock
    private SimpleFinConnectionRepository connectionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private SimpleFinSyncSupport syncSupport;

    @InjectMocks
    private SimpleFinConnectionSetupService setupService;

    @Captor
    private ArgumentCaptor<SimpleFinConnection> connectionCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(encryptionService.encrypt(ACCESS_URL)).thenReturn(ENCRYPTED_URL);
    }

    @Test
    void setupConnectionExchangesTokenAndCreatesConnection() {
        SimpleFinClient.SimpleFinAccount sfAccount = new SimpleFinClient.SimpleFinAccount(
                "ext-1", "Checking", "My Bank", "USD",
                new BigDecimal("1000.00"), new BigDecimal("900.00"), "CHECKING", List.of()
        );
        SimpleFinClient.SimpleFinAccountsResponse response =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(sfAccount), List.of());

        when(simpleFinClient.exchangeSetupToken(SETUP_TOKEN)).thenReturn(ACCESS_URL);
        when(simpleFinClient.fetchAccounts(ACCESS_URL)).thenReturn(response);
        when(syncSupport.summarizeInstitutionNames(any(), any())).thenReturn("My Bank");

        when(connectionRepository.save(any(SimpleFinConnection.class))).thenAnswer(invocation -> {
            SimpleFinConnection conn = invocation.getArgument(0);
            conn.setId(CONNECTION_ID);
            return conn;
        });

        when(accountRepository.findByConnectionId(CONNECTION_ID))
                .thenReturn(List.of(Account.builder().id(1L).institutionName("My Bank").build()));
        when(accountRepository.countByConnectionId(CONNECTION_ID)).thenReturn(1);

        ConnectionDto result = setupService.setupConnection(USER_ID, SETUP_TOKEN);

        assertNotNull(result);
        assertEquals(CONNECTION_ID, result.getId());
        assertEquals("My Bank", result.getInstitutionName());
        assertEquals(SyncStatus.SUCCESS, result.getSyncStatus());
        assertEquals(1, result.getAccountCount());

        verify(simpleFinClient).exchangeSetupToken(SETUP_TOKEN);
        verify(simpleFinClient).fetchAccounts(ACCESS_URL);
        verify(encryptionService).encrypt(ACCESS_URL);
        verify(syncSupport).createOrUpdateAccount(eq(USER_ID), eq(CONNECTION_ID), eq(sfAccount));
        verify(connectionRepository, times(2)).save(any(SimpleFinConnection.class));
    }

    @Test
    void setupConnectionSetsInitialConnectionFieldsCorrectly() {
        SimpleFinClient.SimpleFinAccountsResponse response =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of());

        when(simpleFinClient.exchangeSetupToken(SETUP_TOKEN)).thenReturn(ACCESS_URL);
        when(simpleFinClient.fetchAccounts(ACCESS_URL)).thenReturn(response);
        when(syncSupport.summarizeInstitutionNames(any(), any())).thenReturn("Bank");

        // Capture status at time of each save to handle mutable object
        java.util.List<com.peter.budget.model.enums.SyncStatus> savedStatuses = new java.util.ArrayList<>();
        when(connectionRepository.save(any(SimpleFinConnection.class))).thenAnswer(invocation -> {
            SimpleFinConnection conn = invocation.getArgument(0);
            savedStatuses.add(conn.getSyncStatus());
            conn.setId(CONNECTION_ID);
            return conn;
        });

        when(accountRepository.findByConnectionId(CONNECTION_ID)).thenReturn(List.of());
        when(accountRepository.countByConnectionId(CONNECTION_ID)).thenReturn(0);

        setupService.setupConnection(USER_ID, SETUP_TOKEN);

        assertEquals(2, savedStatuses.size());
        // First save should have PENDING status
        assertEquals(com.peter.budget.model.enums.SyncStatus.PENDING, savedStatuses.get(0));
        // Second save should have SUCCESS status
        assertEquals(com.peter.budget.model.enums.SyncStatus.SUCCESS, savedStatuses.get(1));
    }

    @Test
    void setupConnectionCreatesAccountsForEachSimpleFinAccount() {
        SimpleFinClient.SimpleFinAccount acct1 = new SimpleFinClient.SimpleFinAccount(
                "ext-1", "Checking", "Bank A", "USD",
                new BigDecimal("500.00"), null, "CHECKING", List.of()
        );
        SimpleFinClient.SimpleFinAccount acct2 = new SimpleFinClient.SimpleFinAccount(
                "ext-2", "Savings", "Bank A", "USD",
                new BigDecimal("2000.00"), null, "SAVINGS", List.of()
        );
        SimpleFinClient.SimpleFinAccountsResponse response =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(acct1, acct2), List.of());

        when(simpleFinClient.exchangeSetupToken(SETUP_TOKEN)).thenReturn(ACCESS_URL);
        when(simpleFinClient.fetchAccounts(ACCESS_URL)).thenReturn(response);
        when(syncSupport.summarizeInstitutionNames(any(), any())).thenReturn("Bank A");
        when(connectionRepository.save(any(SimpleFinConnection.class))).thenAnswer(invocation -> {
            SimpleFinConnection conn = invocation.getArgument(0);
            conn.setId(CONNECTION_ID);
            return conn;
        });
        when(accountRepository.findByConnectionId(CONNECTION_ID)).thenReturn(List.of());
        when(accountRepository.countByConnectionId(CONNECTION_ID)).thenReturn(2);

        ConnectionDto result = setupService.setupConnection(USER_ID, SETUP_TOKEN);

        assertEquals(2, result.getAccountCount());
        verify(syncSupport).createOrUpdateAccount(USER_ID, CONNECTION_ID, acct1);
        verify(syncSupport).createOrUpdateAccount(USER_ID, CONNECTION_ID, acct2);
    }

    @Test
    void setupConnectionUpdatesSyncStatusToSuccessOnCompletion() {
        SimpleFinClient.SimpleFinAccountsResponse response =
                new SimpleFinClient.SimpleFinAccountsResponse(List.of(), List.of());

        when(simpleFinClient.exchangeSetupToken(SETUP_TOKEN)).thenReturn(ACCESS_URL);
        when(simpleFinClient.fetchAccounts(ACCESS_URL)).thenReturn(response);
        when(syncSupport.summarizeInstitutionNames(any(), any())).thenReturn("Bank");
        when(connectionRepository.save(any(SimpleFinConnection.class))).thenAnswer(invocation -> {
            SimpleFinConnection conn = invocation.getArgument(0);
            conn.setId(CONNECTION_ID);
            return conn;
        });
        when(accountRepository.findByConnectionId(CONNECTION_ID)).thenReturn(List.of());
        when(accountRepository.countByConnectionId(CONNECTION_ID)).thenReturn(0);

        setupService.setupConnection(USER_ID, SETUP_TOKEN);

        verify(connectionRepository, times(2)).save(connectionCaptor.capture());
        SimpleFinConnection secondSave = connectionCaptor.getAllValues().get(1);
        assertEquals(SyncStatus.SUCCESS, secondSave.getSyncStatus());
        assertNotNull(secondSave.getLastSyncAt());
    }
}
