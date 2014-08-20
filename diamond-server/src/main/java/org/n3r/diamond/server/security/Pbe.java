package org.n3r.diamond.server.security;

import org.apache.commons.codec.Charsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.bind.DatatypeConverter;

public class Pbe {
    private static final byte[] SALT = {
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
            (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    private static final String ALGORITHM = "PBEWITHMD5andDES";

    public static void main(String[] args) throws Exception {
        String originalPassword = "secret";
        System.out.println("Original password: " + originalPassword);
        String encryptedPassword = encrypt(originalPassword, "mypass");
        System.out.println("Encrypted password: " + encryptedPassword);
        String decryptedPassword = decrypt(encryptedPassword, "mypass");
        System.out.println("Decrypted password: " + decryptedPassword);
    }

    public static String encrypt(String property, String password) {
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
            byte[] bytes = pbeCipher.doFinal(property.getBytes(Charsets.UTF_8));
            return DatatypeConverter.printBase64Binary(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String property, String password) {
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
            Cipher pbeCipher = Cipher.getInstance(ALGORITHM);
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
            byte[] bytes = DatatypeConverter.parseBase64Binary(property);
            return new String(pbeCipher.doFinal(bytes), Charsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
