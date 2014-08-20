package org.n3r.diamond.server.security;


import org.apache.commons.codec.Charsets;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

// refer:
// http://www.reindel.com/articles/asymmetric-public-key-encryption-using-rsa-java-and-openssl.txt
public class Rsa {
    private static final String ALGORITHM = "RSA";
    private static final String RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding";

    public static String decrypt(String value, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] valueBytes = DatatypeConverter.parseBase64Binary(value);

            byte[] decrypted = cipher.doFinal(valueBytes);
            return new String(decrypted, Charsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String encrypt(String value, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] valueBytes = value.getBytes(Charsets.UTF_8);

            byte[] encrypted = cipher.doFinal(valueBytes);
            return DatatypeConverter.printBase64Binary(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Key getPublicKey(String pubkey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            byte[] decodedPubKey = DatatypeConverter.parseBase64Binary(pubkey);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPubKey);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static Key getPrivateKey(String prikey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            byte[] decodedPriKey = DatatypeConverter.parseBase64Binary(prikey);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodedPriKey);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] generatePublicKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            byte[] encodedPublicKey = keyPair.getPublic().getEncoded();
            byte[] encodedPrivateKey = keyPair.getPrivate().getEncoded();
            return new String[]{
                    DatatypeConverter.printBase64Binary(encodedPublicKey),
                    DatatypeConverter.printBase64Binary(encodedPrivateKey)
            };
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String[] keys = generatePublicKey();
        String publicKey = keys[0];
        System.out.println(publicKey);
        String privateKey = keys[1];
        System.out.println(privateKey);

        String helloworld = encrypt("helloworld", getPrivateKey(privateKey));
        System.out.println(helloworld);

        String decrypt = decrypt(helloworld, getPublicKey(publicKey));
        System.out.println(decrypt);
    }

}
