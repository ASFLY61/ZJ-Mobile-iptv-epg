package com.chinamobile.middleware.auth.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import java.io.File;
import java.net.URLDecoder;

/* JADX INFO: loaded from: classes.dex */
public class DataProcessing {
    private Context mContext;

    public static native boolean decryptAes(byte[] bArr);

    public DataProcessing(Context context) {
        this.mContext = context;
    }

    @SuppressLint({"SdCardPath"})
    public String decrypt(String str) {
        try {
            File file = new File("/data/data/com.chinamobile.middleware.auth/data");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            decryptAes(Base64.decode(URLDecoder.decode(str), 2));
            byte[] bytes = Tools.getBytes("/data/data/com.chinamobile.middleware.auth/data");
            byte[] bArr = new byte[bytes.length - bytes[bytes.length - 1]];
            System.arraycopy(bytes, 0, bArr, 0, bArr.length);
            return new String(bArr);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
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

    static {
        try {
            System.loadLibrary("dataprocessing");
        } catch (UnsatisfiedLinkError unused) {
            System.err.println("WARNING: Could not load library!");
        }
    }

    public static boolean isSdCardExist() {
        return Environment.getExternalStorageState().equals("mounted");
    }
}
