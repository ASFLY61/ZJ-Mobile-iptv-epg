package com.chinamobile.middleware.auth.Encode;

import android.util.Log;
import cn.hutool.core.util.CharsetUtil;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* JADX INFO: loaded from: classes.dex */
public class AESUtil {
    private byte[] iv;
    private int length = 0;

    private void main2() {
    }

    public static String parseByte2HexStr(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer();
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            stringBuffer.append(hexString.toUpperCase());
        }
        return stringBuffer.toString();
    }

    public static byte[] parseHexStr2Byte(String str) {
        if (str.length() < 1) {
            return null;
        }
        byte[] bArr = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            int i2 = i * 2;
            int i3 = i2 + 1;
            bArr[i] = (byte) ((Integer.parseInt(str.substring(i2, i3), 16) * 16) + Integer.parseInt(str.substring(i3, i2 + 2), 16));
        }
        return bArr;
    }

    public String Encrypt(String str, String str2) {
        try {
            byte[] bytes = str.getBytes(CharsetUtil.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(parseHexStr2Byte(str2), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, secretKeySpec);
            byte[] iv = cipher.getIV();
            System.out.println("加密生成IV-Str:" + new String(iv));
            System.out.println("iv位数:" + iv.length);
            byte[] bArrDoFinal = cipher.doFinal(bytes);
            byte[] bArr = new byte[iv.length + bytes.length + 16];
            this.length = iv.length;
            this.iv = iv;
            System.arraycopy(iv, 0, bArr, 0, iv.length);
            System.arraycopy(bArrDoFinal, 0, bArr, iv.length, bArrDoFinal.length);
            System.out.println("encryptData:" + new String(bArrDoFinal));
            System.out.println("message:" + new String(bArr));
            return Base64.getEncoder().encodeToString(bArr);
        } catch (Exception unused) {
            return null;
        }
    }

    public String Decrypt(String str, String str2) {
        try {
            System.out.println("Decrypt: length :" + this.length);
            byte[] bArrDecode = Base64.getDecoder().decode(str);
            System.out.println("Decrypt: " + new String(bArrDecode));
            byte[] hexStr2Byte = parseHexStr2Byte(str2);
            GCMParameterSpec gCMParameterSpec = new GCMParameterSpec(128, this.iv);
            String str3 = new String(gCMParameterSpec.getIV());
            System.out.println("解密分解出IV-Str:" + str3);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, new SecretKeySpec(hexStr2Byte, "AES"), gCMParameterSpec);
            return new String(cipher.doFinal(bArrDecode, this.length, bArrDecode.length - this.length));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getIVbytes(String str) {
        byte[] bArr = new byte[12];
        System.arraycopy(Base64.getDecoder().decode(str), 0, bArr, 0, 12);
        Log.i("onResponse", "获取密文中的IV-Str:" + new String(bArr));
        return bArr;
    }

    public static void main() {
        try {
            File file = new File("/data/data/com.chinamobile.middleware.auth/data");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            String strSubstring = SHA256Util.Encrypt("7+ZzX7/O3LW4bdBwohVJlw==0cd908d99bc8343b754c1a14bd53d916", "SHA-256").substring(0, 16);
            System.out.println("main:AES_ECB:secretKey :" + strSubstring);
            byte[] bArrEncryption = encryption("808080", strSubstring);
            String strEncode = URLEncoder.encode(new String(bArrEncryption));
            System.out.println("main:AES_ECB:" + URLEncoder.encode(new String(bArrEncryption)));
            byte[] bArrDecryption = decryption(URLDecoder.decode(strEncode).getBytes(), strSubstring);
            System.out.println("main:AES_ECB decryption:" + new String(bArrDecryption));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] decryption(byte[] bArr, String str) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(str.getBytes(CharsetUtil.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, secretKeySpec);
            return cipher.doFinal(bArr);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] encryption(String str, String str2) {
        if (str2 == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(1, new SecretKeySpec(str2.getBytes(CharsetUtil.UTF_8), "AES"));
            return cipher.doFinal(str.getBytes("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return sb.toString();
    }
}
