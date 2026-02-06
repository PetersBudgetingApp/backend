package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SimpleFinClient {

    private final WebClient webClient;

    public SimpleFinClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String exchangeSetupToken(String setupToken) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(setupToken);
            String claimUrl = new String(decoded);

            log.info("Exchanging setup token at claim URL");

            String accessUrl = webClient.post()
                    .uri(claimUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (accessUrl == null || accessUrl.isBlank()) {
                throw ApiException.badRequest("Failed to exchange setup token - empty response");
            }

            return accessUrl.trim();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid setup token format");
        } catch (WebClientResponseException e) {
            log.error("SimpleFin API error during token exchange: {}", e.getMessage());
            throw ApiException.badRequest("Failed to exchange setup token: " + e.getMessage());
        }
    }

    public SimpleFinAccountsResponse fetchAccounts(String accessUrl) {
        return fetchAccounts(accessUrl, null, null);
    }

    public SimpleFinAccountsResponse fetchAccounts(String accessUrl, LocalDate startDate, LocalDate endDate) {
        try {
            URI baseUri = URI.create(accessUrl);
            String credentials = baseUri.getUserInfo();
            String baseUrl = baseUri.getScheme() + "://" + baseUri.getHost() +
                    (baseUri.getPort() > 0 ? ":" + baseUri.getPort() : "") +
                    baseUri.getPath();

            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("/accounts");

            boolean hasParams = false;
            if (startDate != null) {
                long startTimestamp = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
                urlBuilder.append("?start-date=").append(startTimestamp);
                hasParams = true;
            }
            if (endDate != null) {
                long endTimestamp = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
                urlBuilder.append(hasParams ? "&" : "?").append("end-date=").append(endTimestamp);
            }

            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes());

            log.debug("Fetching accounts from SimpleFin");

            Map<String, Object> response = webClient.get()
                    .uri(urlBuilder.toString())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseAccountsResponse(response);
        } catch (WebClientResponseException e) {
            log.error("SimpleFin API error during account fetch: {} - {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 403) {
                throw ApiException.unauthorized("SimpleFin access token is invalid or expired");
            }
            throw ApiException.internal("Failed to fetch accounts from SimpleFin");
        } catch (Exception e) {
            log.error("Error fetching accounts from SimpleFin", e);
            throw ApiException.internal("Failed to fetch accounts: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private SimpleFinAccountsResponse parseAccountsResponse(Map<String, Object> response) {
        if (response == null) {
            return new SimpleFinAccountsResponse(List.of(), List.of());
        }

        List<Map<String, Object>> accountsData = (List<Map<String, Object>>) response.get("accounts");
        if (accountsData == null) {
            return new SimpleFinAccountsResponse(List.of(), List.of());
        }

        List<SimpleFinAccount> accounts = accountsData.stream()
                .map(this::parseAccount)
                .toList();

        List<String> errors = response.get("errors") != null ?
                (List<String>) response.get("errors") : List.of();

        return new SimpleFinAccountsResponse(accounts, errors);
    }

    @SuppressWarnings("unchecked")
    private SimpleFinAccount parseAccount(Map<String, Object> data) {
        Map<String, Object> org = (Map<String, Object>) data.get("org");
        String institutionName = org != null ? (String) org.get("name") : null;

        List<Map<String, Object>> transactionsData =
                (List<Map<String, Object>>) data.get("transactions");

        List<SimpleFinTransaction> transactions = transactionsData != null ?
                transactionsData.stream().map(this::parseTransaction).toList() :
                List.of();

        String balanceStr = (String) data.get("balance");
        String availableStr = (String) data.get("available-balance");

        return new SimpleFinAccount(
                (String) data.get("id"),
                (String) data.get("name"),
                institutionName,
                (String) data.get("currency"),
                balanceStr != null ? new java.math.BigDecimal(balanceStr) : java.math.BigDecimal.ZERO,
                availableStr != null ? new java.math.BigDecimal(availableStr) : null,
                parseAccountType((String) data.get("type")),
                transactions
        );
    }

    private SimpleFinTransaction parseTransaction(Map<String, Object> data) {
        Number postedNum = (Number) data.get("posted");
        Instant posted = postedNum != null ?
                Instant.ofEpochSecond(postedNum.longValue()) : Instant.now();

        Number transactedNum = (Number) data.get("transacted_at");
        Instant transacted = transactedNum != null ?
                Instant.ofEpochSecond(transactedNum.longValue()) : null;

        String amountStr = (String) data.get("amount");
        java.math.BigDecimal amount = amountStr != null ?
                new java.math.BigDecimal(amountStr) : java.math.BigDecimal.ZERO;

        Object pendingObj = data.get("pending");
        boolean pending = pendingObj != null && Boolean.TRUE.equals(pendingObj);

        return new SimpleFinTransaction(
                (String) data.get("id"),
                posted,
                transacted,
                amount,
                pending,
                (String) data.get("description"),
                (String) data.get("payee"),
                (String) data.get("memo")
        );
    }

    private String parseAccountType(String type) {
        if (type == null) return "OTHER";
        return switch (type.toLowerCase()) {
            case "checking" -> "CHECKING";
            case "savings" -> "SAVINGS";
            case "credit", "credit card", "creditcard" -> "CREDIT_CARD";
            case "loan", "mortgage" -> "LOAN";
            case "investment", "brokerage" -> "INVESTMENT";
            default -> "OTHER";
        };
    }

    public record SimpleFinAccountsResponse(
            List<SimpleFinAccount> accounts,
            List<String> errors
    ) {}

    public record SimpleFinAccount(
            String id,
            String name,
            String institutionName,
            String currency,
            java.math.BigDecimal balance,
            java.math.BigDecimal availableBalance,
            String accountType,
            List<SimpleFinTransaction> transactions
    ) {}

    public record SimpleFinTransaction(
            String id,
            Instant posted,
            Instant transacted,
            java.math.BigDecimal amount,
            boolean pending,
            String description,
            String payee,
            String memo
    ) {}
}
