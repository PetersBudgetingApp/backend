package com.peter.budget.service;

import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final AES256TextEncryptor encryptor;

    public EncryptionService(@Value("${encryption.secret}") String secret) {
        this.encryptor = new AES256TextEncryptor();
        this.encryptor.setPassword(secret);
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        return encryptor.decrypt(encryptedText);
    }
}
