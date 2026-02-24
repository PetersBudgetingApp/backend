package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SimpleFinSyncPolicyTest {

    @Mock
    private SimpleFinConnectionRepository connectionRepository;

    @InjectMocks
    private SimpleFinSyncPolicy syncPolicy;

    @Test
    void initialSyncDaysReturns60() {
        assertEquals(60, syncPolicy.initialSyncDays());
    }

    @Test
    void emptyBackfillWindowsToCompleteReturns12() {
        assertEquals(12, syncPolicy.emptyBackfillWindowsToComplete());
    }

    @Test
    void historyCutoffDateReturns1970() {
        assertEquals(LocalDate.of(1970, 1, 1), syncPolicy.historyCutoffDate());
    }

    @Test
    void canMakeRequestReturnsTrueWhenResetAtIsNull() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .requestsResetAt(null)
                .requestsToday(0)
                .build();

        assertTrue(syncPolicy.canMakeRequest(connection));
    }

    @Test
    void canMakeRequestReturnsTrueWhenResetTimeHasPassed() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .requestsResetAt(Instant.now().minusSeconds(3600))
                .requestsToday(99)
                .build();

        assertTrue(syncPolicy.canMakeRequest(connection));
    }

    @Test
    void canMakeRequestReturnsTrueWhenBelowDailyLimit() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .requestsResetAt(Instant.now().plusSeconds(3600))
                .requestsToday(10)
                .build();

        assertTrue(syncPolicy.canMakeRequest(connection));
    }

    @Test
    void canMakeRequestReturnsFalseWhenAtDailyLimit() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .requestsResetAt(Instant.now().plusSeconds(3600))
                .requestsToday(24)
                .build();

        assertFalse(syncPolicy.canMakeRequest(connection));
    }

    @Test
    void calculateStartDateReturnsInitialSyncWindowWhenNeverSynced() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .lastSyncAt(null)
                .build();

        LocalDate result = syncPolicy.calculateStartDate(connection);

        assertEquals(LocalDate.now().minusDays(60), result);
    }

    @Test
    void calculateStartDateReturnsLastSyncMinusOverlap() {
        Instant lastSync = LocalDate.of(2026, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC);
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .lastSyncAt(lastSync)
                .build();

        LocalDate result = syncPolicy.calculateStartDate(connection);

        assertEquals(LocalDate.of(2026, 1, 12), result); // 15 - 3 days overlap
    }

    @Test
    void adjustStartDateReturnsNormalDateWhenNoStaleAccounts() {
        LocalDate normalDate = LocalDate.of(2026, 2, 20);

        LocalDate result = syncPolicy.adjustStartDateForStaleAccounts(normalDate, null);

        assertEquals(normalDate, result);
    }

    @Test
    void adjustStartDateExtendsWindowForStaleAccount() {
        // Normal window starts Feb 20, but an account was last updated on Feb 10
        LocalDate normalDate = LocalDate.of(2026, 2, 20);
        Instant staleUpdate = LocalDate.of(2026, 2, 10).atStartOfDay().toInstant(ZoneOffset.UTC);

        LocalDate result = syncPolicy.adjustStartDateForStaleAccounts(normalDate, staleUpdate);

        // Should extend to stale date minus 3 days overlap = Feb 7
        assertEquals(LocalDate.of(2026, 2, 7), result);
    }

    @Test
    void adjustStartDateDoesNotExtendWhenAccountIsRecent() {
        // Normal window starts Feb 15, account was updated on Feb 20 (more recent)
        LocalDate normalDate = LocalDate.of(2026, 2, 15);
        Instant recentUpdate = LocalDate.of(2026, 2, 20).atStartOfDay().toInstant(ZoneOffset.UTC);

        LocalDate result = syncPolicy.adjustStartDateForStaleAccounts(normalDate, recentUpdate);

        assertEquals(normalDate, result);
    }

    @Test
    void adjustStartDateCapsAtMaxCatchupDays() {
        // Normal window starts recently, but account is extremely stale (6 months ago)
        LocalDate normalDate = LocalDate.now().minusDays(3);
        Instant veryStaleUpdate = LocalDate.now().minusDays(180).atStartOfDay().toInstant(ZoneOffset.UTC);

        LocalDate result = syncPolicy.adjustStartDateForStaleAccounts(normalDate, veryStaleUpdate);

        // Should cap at 90 days ago, not 183 days ago
        assertEquals(LocalDate.now().minusDays(90), result);
    }

    @Test
    void consumeRequestQuotaIncrementsCountAndSetsResetTime() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .id(1L)
                .requestsResetAt(null)
                .requestsToday(0)
                .build();

        syncPolicy.consumeRequestQuota(connection, 1L);

        verify(connectionRepository).incrementRequestCount(1L);
        assertEquals(1, connection.getRequestsToday());
        assertTrue(connection.getRequestsResetAt().isAfter(Instant.now()));
    }

    @Test
    void consumeRequestQuotaIncrementsExistingCount() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .id(1L)
                .requestsResetAt(Instant.now().plusSeconds(3600))
                .requestsToday(5)
                .build();

        syncPolicy.consumeRequestQuota(connection, 1L);

        verify(connectionRepository).incrementRequestCount(1L);
        assertEquals(6, connection.getRequestsToday());
    }

    @Test
    void consumeRequestQuotaThrowsTooManyRequestsWhenAtLimit() {
        SimpleFinConnection connection = SimpleFinConnection.builder()
                .id(1L)
                .requestsResetAt(Instant.now().plusSeconds(3600))
                .requestsToday(24)
                .build();

        ApiException exception = assertThrows(
                ApiException.class,
                () -> syncPolicy.consumeRequestQuota(connection, 1L)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    }
}
