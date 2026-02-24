package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import com.peter.budget.model.entity.SimpleFinConnection;
import com.peter.budget.repository.SimpleFinConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SimpleFinSyncPolicy {

    private static final int MAX_DAILY_REQUESTS = 24;
    private static final int INITIAL_SYNC_DAYS = 60;
    private static final int OVERLAP_DAYS = 3;
    private static final int MAX_CATCHUP_DAYS = 90;
    private static final int EMPTY_BACKFILL_WINDOWS_TO_COMPLETE = 12;
    private static final LocalDate HISTORY_CUTOFF_DATE = LocalDate.of(1970, 1, 1);

    private final SimpleFinConnectionRepository connectionRepository;

    public int initialSyncDays() {
        return INITIAL_SYNC_DAYS;
    }

    public int emptyBackfillWindowsToComplete() {
        return EMPTY_BACKFILL_WINDOWS_TO_COMPLETE;
    }

    public LocalDate historyCutoffDate() {
        return HISTORY_CUTOFF_DATE;
    }

    public boolean canMakeRequest(SimpleFinConnection connection) {
        if (connection.getRequestsResetAt() == null ||
                connection.getRequestsResetAt().isBefore(Instant.now())) {
            return true;
        }
        return connection.getRequestsToday() < MAX_DAILY_REQUESTS;
    }

    public LocalDate calculateStartDate(SimpleFinConnection connection) {
        if (connection.getLastSyncAt() == null) {
            return LocalDate.now().minusDays(INITIAL_SYNC_DAYS);
        }
        return connection.getLastSyncAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(OVERLAP_DAYS);
    }

    /**
     * Extends the sync start date backwards if any account in the connection
     * has a stale {@code balanceUpdatedAt} (e.g. its institution was logged out
     * for several syncs and recently re-authenticated).  The returned date is
     * never earlier than {@code MAX_CATCHUP_DAYS} ago.
     */
    public LocalDate adjustStartDateForStaleAccounts(LocalDate normalStartDate, Instant oldestAccountBalanceUpdate) {
        if (oldestAccountBalanceUpdate == null) {
            return normalStartDate;
        }
        LocalDate staleDate = oldestAccountBalanceUpdate
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(OVERLAP_DAYS);
        LocalDate maxCatchupDate = LocalDate.now().minusDays(MAX_CATCHUP_DAYS);

        if (staleDate.isBefore(normalStartDate)) {
            return staleDate.isBefore(maxCatchupDate) ? maxCatchupDate : staleDate;
        }
        return normalStartDate;
    }

    public void consumeRequestQuota(SimpleFinConnection connection, Long connectionId) {
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
}
