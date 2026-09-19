package com.chinamobile.middleware.auth.tools;

import android.support.v4.view.accessibility.AccessibilityEventCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* JADX INFO: loaded from: classes.dex */
public class Md5Util {
    protected static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    protected static MessageDigest messagedigest;
    byte[] buffer = null;

    static {
        messagedigest = null;
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getFileMD5String(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        System.out.println("......buffer=" + this.buffer);
        if (this.buffer == null) {
            this.buffer = new byte[AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START];
        }
        while (true) {
            int i = fileInputStream.read(this.buffer);
            if (i > 0) {
                messagedigest.update(this.buffer, 0, i);
            } else {
                fileInputStream.close();
                return bufferToHex(messagedigest.digest());
            }
        }
    }

    public static String getMD5String(String str) {
        return getMD5String(str.getBytes());
    }

    public static String getMD5String(byte[] bArr) {
        messagedigest.update(bArr);
        return bufferToHex(messagedigest.digest());
    }

    private static String bufferToHex(byte[] bArr) {
        return bufferToHex(bArr, 0, bArr.length);
    }

    private static String bufferToHex(byte[] bArr, int i, int i2) {
        StringBuffer stringBuffer = new StringBuffer(i2 * 2);
        int i3 = i2 + i;
        while (i < i3) {
            appendHexPair(bArr[i], stringBuffer);
            i++;
        }
        return stringBuffer.toString();
    }

    private static void appendHexPair(byte b, StringBuffer stringBuffer) {
        char[] cArr = hexDigits;
        char c = cArr[(b & 240) >> 4];
        char c2 = cArr[b & 15];
        stringBuffer.append(c);
        stringBuffer.append(c2);
    }

    public static boolean checkPassword(String str, String str2) {
        return getMD5String(str).equals(str2);
    }

    public static void main(String[] strArr) throws IOException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String fileMD5String = new Md5Util().getFileMD5String(new File("d:/zgm9004_vortex-ota-91000070-sign.zip"));
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        System.out.println("md5:" + fileMD5String + "\n time:" + ((jCurrentTimeMillis2 - jCurrentTimeMillis) / 1000) + "s");
    }
}
