package com.sectrend.buildscan.result;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RsaEncryptedUtils {


    private static final String PUBLIC_KEY_STR = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhRmd55Icf" +
            "gcPXAtW9l7lHuTqfgn8FmQtT3yi4bHFJD8SbV/6l5ZeS64K5L5O1cfRjIURDKsOgEb6/Pmch" +
            "wHiKpXbJxidFkyjnF/xc1iQPyNfQk7+Prcyqaev5eZm6rUor1ofdBoBEWFOz5LAFKcJ3hBh3" +
            "jxoAwxaPvjhD6wQ+qHpXleZpiD9Af17lsr4Xq4xOJr2Uu1Zm8wWRhMdaSIPmvBAhaMdQuHJz" +
            "OaouJsvhiMqsxTiT5Pyn+UuT2ptqEYMMYSuy9Kc0MSjTFtFaB5W1YHugH17qOsfic2YaqbJY" +
            "/RySrex0ZpADCpt4kXVO1/tf+JRTlAPNBNKuml7pmoSyQIDAQAB";

    private static final String PRIVATE_KEY_STR = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCFGZ3nkhx+Bw9cC1b2XuUe5Op+CfwWZC1P" +
            "fKLhscUkPxJtX/qXll5Lrgrkvk7Vx9GMhREMqw6ARvr8+ZyHAeIqldsnGJ0WTKOcX/FzWJA/I19CTv4+tzKpp6/l5mbqtSiv" +
            "Wh90GgERYU7PksAUpwneEGHePGgDDFo++OEPrBD6oeleV5mmIP0B/XuWyvherjE4mvZS7VmbzBZGEx1pIg+" +
            "a8ECFox1C4cnM5qi4my+GIyqzFOJPk/Kf5S5Pam2oRgwxhK7L0pzQxKNMW0VoHlbVge6AfXuo6x+JzZhqpslj9H" +
            "JKt7HRmkAMKm3iRdU7X+1/4lFOUA80E0q6aXumahLJAgMBAAECggEAZh3OSUGPfJ2cC" +
            "illEeaicP5+bi7o6qD3JnngWbpTH0kIsUOm9jBWrkeccf0UbO4+dkoNV6PRn1dV70r" +
            "u7aHjTHrmxesbcmVgTXRKwg1btiVES5Jhe+qAAyv1RzRrF33f9hD1+tSMpzH6DkVv" +
            "+yhg6K+29pNv1Y7fmlrGCYkhCbiWsfIhnfoMIJaqr88YIZMeBzMDR6xEV9Ks7wtg2hc" +
            "j6wpvZrL4ejOLqyi6lT5bvdyKPvzozoWwbtrW3Bd9TDkB0yV+yYKdFKxEiBzX+DDiROj9" +
            "CbLSy+iHndWMr+PI+036VPsfv25L1ICUNrs9FA09fQbJg8Mn63mooIbqumHAgQKBgQDn9Dp" +
            "GGfM8dTR5zw71CocUtu7dxUoOYJ+YedeXI0sOBfboSk71HLIjEM4+XcGe4ewI6kBj7PVk5QBR" +
            "kh1OeUIaUUj8kEm/uFKaPbKdvaTvU4MeMfKHh6zA3ZE1fZZbYu+Oo8WkLwPgKpyldtIToAJKI" +
            "P8IaTW96373jtXXWWGBGQKBgQCS5e2CodWHL+i+yQtFWwJ14luDGOO8R6MtjcpITE5T7yadcY" +
            "EY/HOxeVcWLduKYMDIOrqJ8hEPI+CCtXVwQqa93dqX+J3SZvFS94wOY7meoNE7FHIYT4SAQPRV" +
            "57XyPKzSACEHHDceU02y9TzxgVOpUOfNmq24SL1Hi68eZA3lMQKBgGwgBwIimqY2JI4bnWdQzw" +
            "My+0pZ61mkZQEY2wzTIOuakCxcZ04PYGLENMARyG4d9n95YyaxkPrFXU6pBAxOrifCeHlzcU1z" +
            "yN2poRezDa2aimaK9fmDn14Qat46etqC1hTx9vHAENhwRLFFIEyRrD+N/hSXpruviXIxHTHTso" +
            "YRAoGAGYsp1cFWY/+MtUA4WYMN1nnZ993oG+FJGq7BMfky1Z2MVWxbSoD7jSzQW+b1egaA/1BXB" +
            "420MbHHZHMxTKgKXpGpZSuyJdgItqidDhOBP3gvadqQTHHnVHX3BskX9lteodWr6JSbcQaDSE8" +
            "kncojRhnvdO0ksBrV7w73EI3NbMECgYEA5mhgmGL0CrZot7JAQxfvbmpQbBcwB3zMyaJfq/1PR" +
            "+9TDr8HarZ49CD1rYPXXMa5hD4P2iZFzQzpgmRP/L2ipTvH44o2FQdxZneNgBB5OkMonNoEBxo" +
            "pW9EqSBFq/Z5UOIXa+l47C5tc0SKK1mzPo66QNtrxwwjDGKYhdJHzkgU=";

    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    private static void loadKeyPairFromString() throws InvalidKeySpecException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] publicKeyBytes = Base64.getDecoder().decode(extractKeyBytes(PUBLIC_KEY_STR));
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            publicKey = keyFactory.generatePublic(publicKeySpec);

            byte[] privateKeyBytes = Base64.getDecoder().decode(extractKeyBytes(PRIVATE_KEY_STR));
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static String extractKeyBytes(String key) {
        // 去除起始标识和结尾标识
        key = key.replace("-----BEGIN PUBLIC KEY-----", "");
        key = key.replace("-----END PUBLIC KEY-----", "");
        key = key.replace("-----BEGIN PRIVATE KEY-----", "");
        key = key.replace("-----END PRIVATE KEY-----", "");

        // 去除换行符和空格
        key = key.replaceAll("\\s+", "");

        return key;
    }

    private static byte[] encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String decrypt(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }


    /**
     * 解密方法
     *
     * @param password
     * @return
     */
    public static String passwordDecrypt(String password) {
        byte[] decode = Base64.getDecoder().decode(password);
        String decryptedData = null;
        try {
            loadKeyPairFromString();
            decryptedData = decrypt(decode);
        } catch (Exception e) {
            log.error("解密失败", e);
        }
        return decryptedData;
    }

    /**
     * 加密
     *
     * @param password
     * @return
     */
    public static String passwordEncrypt(String password) {

        byte[] encryptedData = new byte[0];
        try {
            loadKeyPairFromString();
            encryptedData = encrypt(password);
        } catch (Exception e) {
            log.error("加密失败", e);
        }
        return Base64.getEncoder().encodeToString(encryptedData);
    }
}
