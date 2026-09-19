package com.chinamobile.middleware.auth.tools;

import java.security.SecureRandom;

/* JADX INFO: loaded from: classes.dex */
public class HttpDigestTools {
    public static final String CNONCE = "cnonce";
    public static final int DIGEST = 401;
    public static final int HTTP_CONNECT_TIMEOUT = 20000;
    public static final int HTTP_OK = 200;
    public static final String NONCE = "nonce";
    public static final String PASSWORD = "password";
    public static final String QOP = "qop";
    public static final String REALM = "realm";
    public static final String RESPONSE = "response";
    public static final String URI = "uri";
    public static final String USERNAME = "username";

    public static String countCN(int i) {
        String upperCase = Long.toHexString(i).toUpperCase();
        StringBuffer stringBuffer = new StringBuffer();
        if (upperCase.length() < 8) {
            int length = 8 - upperCase.length();
            for (int i2 = 0; i2 <= length; i2++) {
                stringBuffer.append(0);
            }
        }
        return ((Object) stringBuffer) + upperCase;
    }

    public static int cnonce() {
        return new SecureRandom().nextInt();
    }
}
