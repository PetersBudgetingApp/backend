package com.peter.budget.service.simplefin;

import com.peter.budget.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SimpleFinClientTest {

    /**
     * Tests for the internal URL-validation and parsing logic of SimpleFinClient.
     * The exchangeSetupToken / fetchAccounts methods depend on WebClient calls
     * which are hard to mock in a unit test without a full server, so we focus
     * on the testable private helpers via reflection.
     */

    // --- validateSimpleFinUri tests ---

    @Test
    void validateSimpleFinUriRejectsNullUrl() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri(null, false));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void validateSimpleFinUriRejectsBlankUrl() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("  ", false));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void validateSimpleFinUriRejectsHttpScheme() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("http://bridge.simplefin.org/data", false));
        assertTrue(ex.getMessage().contains("HTTPS"));
    }

    @Test
    void validateSimpleFinUriRejectsNonSimpleFinHost() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("https://evil.com/data", false));
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void validateSimpleFinUriRejectsFragments() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("https://bridge.simplefin.org/data#fragment", false));
        assertTrue(ex.getMessage().contains("fragments"));
    }

    @Test
    void validateSimpleFinUriRejectsClaimUrlWithCredentials() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("https://user:pass@bridge.simplefin.org/claim", false));
        assertTrue(ex.getMessage().contains("must not include credentials"));
    }

    @Test
    void validateSimpleFinUriRejectsAccessUrlWithoutCredentials() {
        ApiException ex = assertThrows(ApiException.class,
                () -> invokeValidateUri("https://bridge.simplefin.org/data", true));
        assertTrue(ex.getMessage().contains("missing credentials"));
    }

    @Test
    void validateSimpleFinUriAcceptsValidClaimUrl() throws Exception {
        Object result = invokeValidateUri("https://bridge.simplefin.org/claim/token123", false);
        assertNotNull(result);
    }

    @Test
    void validateSimpleFinUriAcceptsValidAccessUrl() throws Exception {
        Object result = invokeValidateUri("https://user:pass@bridge.simplefin.org/data", true);
        assertNotNull(result);
    }

    @Test
    void validateSimpleFinUriAcceptsSubdomainOfSimplefin() throws Exception {
        Object result = invokeValidateUri("https://user:pass@beta.bridge.simplefin.org/data", true);
        assertNotNull(result);
    }

    // --- isAllowedSimpleFinHost tests ---

    @Test
    void isAllowedSimpleFinHostAcceptsExactMatch() throws Exception {
        assertTrue(invokeIsAllowedHost("simplefin.org"));
    }

    @Test
    void isAllowedSimpleFinHostAcceptsSubdomain() throws Exception {
        assertTrue(invokeIsAllowedHost("bridge.simplefin.org"));
    }

    @Test
    void isAllowedSimpleFinHostRejectsOtherDomain() throws Exception {
        assertFalse(invokeIsAllowedHost("evil.com"));
    }

    @Test
    void isAllowedSimpleFinHostIsCaseInsensitive() throws Exception {
        assertTrue(invokeIsAllowedHost("SIMPLEFIN.ORG"));
        assertTrue(invokeIsAllowedHost("Bridge.SimpleFin.Org"));
    }

    // --- parseAccountType tests ---

    @Test
    void parseAccountTypeHandlesChecking() throws Exception {
        assertEquals("CHECKING", invokeParseAccountType("checking"));
    }

    @Test
    void parseAccountTypeHandlesSavings() throws Exception {
        assertEquals("SAVINGS", invokeParseAccountType("savings"));
    }

    @Test
    void parseAccountTypeHandlesCreditVariants() throws Exception {
        assertEquals("CREDIT_CARD", invokeParseAccountType("credit"));
        assertEquals("CREDIT_CARD", invokeParseAccountType("credit card"));
        assertEquals("CREDIT_CARD", invokeParseAccountType("creditcard"));
    }

    @Test
    void parseAccountTypeHandlesLoan() throws Exception {
        assertEquals("LOAN", invokeParseAccountType("loan"));
        assertEquals("LOAN", invokeParseAccountType("mortgage"));
    }

    @Test
    void parseAccountTypeHandlesInvestment() throws Exception {
        assertEquals("INVESTMENT", invokeParseAccountType("investment"));
        assertEquals("INVESTMENT", invokeParseAccountType("brokerage"));
    }

    @Test
    void parseAccountTypeHandlesNullAndUnknown() throws Exception {
        assertEquals("OTHER", invokeParseAccountType(null));
        assertEquals("OTHER", invokeParseAccountType("something_else"));
    }

    // --- parseAccountsResponse tests ---

    @Test
    void parseAccountsResponseHandlesNullResponse() throws Exception {
        SimpleFinClient.SimpleFinAccountsResponse result = invokeParseAccountsResponse(null);
        assertNotNull(result);
        assertTrue(result.accounts().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parseAccountsResponseHandlesEmptyAccounts() throws Exception {
        SimpleFinClient.SimpleFinAccountsResponse result = invokeParseAccountsResponse(Map.of());
        assertNotNull(result);
        assertTrue(result.accounts().isEmpty());
    }

    @Test
    void parseAccountsResponseParsesAccountCorrectly() throws Exception {
        Map<String, Object> accountData = Map.of(
                "id", "acc-123",
                "name", "My Checking",
                "currency", "USD",
                "balance", "1500.50",
                "org", Map.of("name", "Test Bank"),
                "transactions", List.of()
        );

        Map<String, Object> response = Map.of(
                "accounts", List.of(accountData)
        );

        SimpleFinClient.SimpleFinAccountsResponse result = invokeParseAccountsResponse(response);

        assertEquals(1, result.accounts().size());
        SimpleFinClient.SimpleFinAccount account = result.accounts().get(0);
        assertEquals("acc-123", account.id());
        assertEquals("My Checking", account.name());
        assertEquals("Test Bank", account.institutionName());
        assertEquals("USD", account.currency());
        assertEquals(new BigDecimal("1500.50"), account.balance());
    }

    @Test
    void parseAccountsResponseParsesTransactions() throws Exception {
        Map<String, Object> txData = Map.of(
                "id", "tx-1",
                "posted", 1700000000,
                "amount", "-25.50",
                "description", "Coffee shop",
                "pending", false
        );

        Map<String, Object> accountData = Map.of(
                "id", "acc-1",
                "name", "Checking",
                "balance", "100.00",
                "transactions", List.of(txData)
        );

        Map<String, Object> response = Map.of(
                "accounts", List.of(accountData)
        );

        SimpleFinClient.SimpleFinAccountsResponse result = invokeParseAccountsResponse(response);

        assertEquals(1, result.accounts().size());
        List<SimpleFinClient.SimpleFinTransaction> txs = result.accounts().get(0).transactions();
        assertEquals(1, txs.size());
        assertEquals("tx-1", txs.get(0).id());
        assertEquals(new BigDecimal("-25.50"), txs.get(0).amount());
        assertEquals("Coffee shop", txs.get(0).description());
        assertFalse(txs.get(0).pending());
    }

    @Test
    void parseAccountsResponseIncludesErrors() throws Exception {
        Map<String, Object> response = Map.of(
                "accounts", List.of(),
                "errors", List.of("Connection timed out")
        );

        SimpleFinClient.SimpleFinAccountsResponse result = invokeParseAccountsResponse(response);
        assertEquals(1, result.errors().size());
        assertEquals("Connection timed out", result.errors().get(0));
    }

    // --- Reflection helpers ---

    private Object invokeValidateUri(String url, boolean requireCredentials) throws Exception {
        SimpleFinClient client = createClient();
        Method method = SimpleFinClient.class.getDeclaredMethod("validateSimpleFinUri", String.class, boolean.class);
        method.setAccessible(true);
        try {
            return method.invoke(client, url, requireCredentials);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof ApiException apiEx) {
                throw apiEx;
            }
            throw e;
        }
    }

    private boolean invokeIsAllowedHost(String host) throws Exception {
        SimpleFinClient client = createClient();
        Method method = SimpleFinClient.class.getDeclaredMethod("isAllowedSimpleFinHost", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(client, host);
    }

    private String invokeParseAccountType(String type) throws Exception {
        SimpleFinClient client = createClient();
        Method method = SimpleFinClient.class.getDeclaredMethod("parseAccountType", String.class);
        method.setAccessible(true);
        return (String) method.invoke(client, type);
    }

    private SimpleFinClient.SimpleFinAccountsResponse invokeParseAccountsResponse(Map<String, Object> response) throws Exception {
        SimpleFinClient client = createClient();
        Method method = SimpleFinClient.class.getDeclaredMethod("parseAccountsResponse", Map.class);
        method.setAccessible(true);
        try {
            return (SimpleFinClient.SimpleFinAccountsResponse) method.invoke(client, response);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    private SimpleFinClient createClient() {
        return new SimpleFinClient(org.springframework.web.reactive.function.client.WebClient.builder());
    }
}
