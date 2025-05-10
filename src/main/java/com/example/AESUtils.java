package com.example;

import javax.crypto.Cipher;
import javax.crypto.BadPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class AESUtils {
    private static final byte[] AES_KEY = "0123456789abcdef".getBytes(); // Must be 16 bytes
    
    /**
     * Encrypts the given data using AES.
     */
    public static byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts the given encrypted data using AES.
     */
    public static byte[] decrypt(byte[] encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            System.out.println("Decryption successful, first 16 bytes: " + 
                Arrays.toString(Arrays.copyOf(decryptedData, 16)));
            return decryptedData;
        } catch (BadPaddingException e) {
            System.out.println("Padding error! Possible key mismatch or corrupted data.");
            return null;
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            return null;
        }
    }
}