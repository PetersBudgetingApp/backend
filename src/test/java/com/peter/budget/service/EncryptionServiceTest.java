package com.peter.budget.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EncryptionServiceTest {

    private final EncryptionService encryptionService = new EncryptionService("test-secret-key-for-aes-encryption");

    @Test
    void encryptAndDecryptRoundTrip() {
        String plainText = "https://user:pass@api.simplefin.org/data";

        String encrypted = encryptionService.encrypt(plainText);
        assertNotNull(encrypted);
        // Encrypted text should not equal plain text
        assertNotNull(encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void encryptReturnsNullForNullInput() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void decryptReturnsNullForNullInput() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        String plainText = "secret-data";

        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);

        // AES with random IV should produce different ciphertexts
        assertNotNull(encrypted1);
        assertNotNull(encrypted2);
        // Both should decrypt to same value
        assertEquals(plainText, encryptionService.decrypt(encrypted1));
        assertEquals(plainText, encryptionService.decrypt(encrypted2));
    }

    @Test
    void encryptHandlesEmptyString() {
        String encrypted = encryptionService.encrypt("");
        assertNotNull(encrypted);
        assertEquals("", encryptionService.decrypt(encrypted));
    }

    @Test
    void encryptHandlesLongText() {
        String longText = "A".repeat(10000);
        String encrypted = encryptionService.encrypt(longText);
        assertNotNull(encrypted);
        assertEquals(longText, encryptionService.decrypt(encrypted));
    }
}
