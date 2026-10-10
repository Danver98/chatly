package com.danver.messengerserver.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class Encryption {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // Authentication tag length
    private static final int IV_LENGTH_BYTE = 12; // Recommended IV length for GCM

    private final byte[] secretKey;

    @Autowired
    public Encryption(Environment env) {
        this.secretKey = Objects.requireNonNull(env.getProperty("secrets.encryption-util")).getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String input) throws Exception {
        // 1. Generate a random IV
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);

        // 2. Initialize Cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

        // 3. Encrypt the string
        byte[] cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

        // 4. Combine IV and CipherText (needed for decryption)
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        // 5. Encode to URL-safe Base64 for string representation
        return Base64.getUrlEncoder().encodeToString(combined);
    }

    public String decrypt(String base64Ciphertext) throws Exception {
        // 1. Decode Base64 into bytes
        byte[] decoded = Base64.getUrlDecoder().decode(base64Ciphertext);

        // 2. Extract IV from the beginning of the array
        byte[] iv = new byte[IV_LENGTH_BYTE];
        System.arraycopy(decoded, 0, iv, 0, iv.length);

        // 3. Extract the rest part - cyphered text (with authentication tag)
        int ciphertextLength = decoded.length - IV_LENGTH_BYTE;
        byte[] cipherText = new byte[ciphertextLength];
        System.arraycopy(decoded, IV_LENGTH_BYTE, cipherText, 0, ciphertextLength);

        // 4. Init Cipher for decryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

        // 5. Decrypt data and return it
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
}

