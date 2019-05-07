package com.gennlife.fs.common.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by wangyiyan on 2019/3/18
 */
public class RSAEncrypt {
    //公钥字符串
    private static String publicKeyString = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALFEfxe/oLp63kwfLhcPUo39jRkonhQJ5uZO9THFP3+vgTXQLHpm4UJxALnGih+yekjpDuzVo5ELW5WDbF59/EMCAwEAAQ==";
    // 得到私钥字符串
    private static String privateKeyString = "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAsUR/F7+gunreTB8uFw9Sjf2NGSieFAnm5k71McU/f6+BNdAsembhQnEAucaKH7J6SOkO7NWjkQtblYNsXn38QwIDAQABAkAQ33Ixcn38AgHywO0EKOM0vLOXd3REeATQWyefiyTWJFekqvH71nygKOSuZq61dvIdw5wcTrP6r8EjTtLmnHvxAiEA7EaU6IGaNDw4CzljnOoetFW707Zl2dDKIT6ixqRU1z0CIQDAENvyWsfld5dlUfTlpvDIyyJR85eVfhx23nilgQ9ZfwIhAJk0PxPwDYw3S+PDR5sUl+o4+TyTNcGhx5783VFOdxDFAiBwISeXbQJo6BHeGCPmczj9sQIfYBwuLYsGpsk+roM8lwIhALx5jpltNWJqcObglTeTjf0nNrhlNWVba2LWzU/PcZHG";

    public static void main(String[] args) throws Exception {
        //加密字符串
        String message = "ls123456";
//        String messageEn = encrypt(message);
//        System.out.println(message + "\t加密后的字符串为:" + messageEn);
        String messageDe = decrypt("p1WrzVOM4FG5bfADvR4swRmVV0PCBrz/DlxoKCom15HldhiN7+Cwrer4KDFYgiPHdgzBUpkw0aCS6BuprbMT5g==");
        System.out.println("还原后的字符串为:" + messageDe);
    }

    /**
     * RSA加密
     * @param str 待价密字符串
     * @return 加密后的字符串
     */
    public static String encrypt(String str){
        String outStr = null;
        try {
            //base64编码的公钥
            byte[] decoded = Base64.decodeBase64(publicKeyString);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            //RSA加密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outStr;
    }


    /**
     * RSA 解密
     * @param str 加密的字符串
     * @return 解密后的密码
     */
    public static String decrypt(String str){
        String outStr ="";
        try {
            //64位解码加密后的字符串
            byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
            //base64编码的私钥
            byte[] decoded = Base64.decodeBase64(privateKeyString);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            //RSA解密
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            outStr = new String(cipher.doFinal(inputByte));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outStr;
    }

}