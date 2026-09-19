package com.chinamobile.middleware.auth.Encode;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;

/* JADX INFO: loaded from: classes.dex */
public class AESUtils {
    public static String encryption(String str, String str2) {
        return new AES(Mode.ECB, Padding.PKCS5Padding, str2.getBytes()).encryptHex(str);
    }

    public static String decryption(String str, String str2) {
        return new AES(Mode.ECB, Padding.PKCS5Padding, str2.getBytes()).decryptStr(str);
    }

    public static void main(String[] strArr) {
        System.out.println("明文：808080");
        System.out.println("密钥：YOUR_STBACCOUNT_AES_KEY");
        String strEncryption = encryption("808080", "YOUR_STBACCOUNT_AES_KEY");
        System.out.println("加密：" + strEncryption);
        String strDecryption = decryption("YOUR_CIPHERTEXT_HEX", "YOUR_STBACCOUNT_AES_KEY");
        System.out.println("解密：" + strDecryption);
    }
}
