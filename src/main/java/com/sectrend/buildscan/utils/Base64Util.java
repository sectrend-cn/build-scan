package com.sectrend.buildscan.utils;
import org.apache.commons.codec.binary.Base64;

public class Base64Util {


    /**
     * Base64 加密
     * @param str
     * @return
     * @throws Exception
     */
    public static String EncryptBase64(String str) throws Exception {
        Base64 base64 = new Base64();
        String base64str = new String(base64.encode(str
                .getBytes("utf-8")), "utf-8");
        base64str = base64str.replace("\n", "").replace("\r", "")
                .replace('+', '-').replace('/', '_');
        return base64str;
    }

    /**
     * Base64 加密
     * @param bytes
     * @return
     * @throws Exception
     */
    public static String EncryptBase64(byte[] bytes) throws Exception {
        Base64 base64 = new Base64();
        String base64str = new String(base64.encode(bytes), "utf-8");
        base64str = base64str.replace("\n", "").replace("\r", "")
                .replace('+', '-').replace('/', '_');
        return base64str;
    }

    /**
     * Base64 解密
     * @param str
     * @return
     * @throws Exception
     */
    public static String DecryptBase64(String str) throws Exception {
        Base64 base64 = new Base64();
        byte[] bytes = base64.decode(str.replace('-', '+')
                .replace('_', '/').getBytes("utf-8"));
        return new String(bytes, "utf-8");
    }


/*    public static void main(String[] args) throws Exception {
        System.out.println(Base64Util.EncryptBase64(Base64Util.EncryptBase64("1qaz@WSX")));
    }*/



}
