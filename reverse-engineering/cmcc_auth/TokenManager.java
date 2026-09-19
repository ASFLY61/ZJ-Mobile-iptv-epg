package com.chinamobile.middleware.auth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class TokenManager {
    private static final int COUNT = 45;
    public static final String INFO_KEY_ERROR = "error";
    public static final String JAR = "jar";
    public static final String MESSAGE_ERRORCODE = "ErrorCode";
    public static final String MESSAGE_HOST = "Host";
    public static final String MESSAGE_HTTPREQUESTTYPE = "HttpRequestType";
    private static final int SERVICE_BINDED = 2;
    private static final int SERVICE_BINDING = 1;
    private static final int SERVICE_UNBINDED = 0;
    private static final int TIMEINTERVEL = 100;
    public static final String TOKEN_ACTION = "cn.10086.action.HDC_LOGIN_STATUS_UPDATE";
    public static final String TYPE = "type";
    private IAuth mAuth;
    private Context mContext;
    private volatile int mServiceStatus;
    public final String TAG = "TokenManager_20170717";
    private String action = "service.aidl.ACTION";
    private ServiceConnection mConn = new ServiceConnection() { // from class: com.chinamobile.middleware.auth.TokenManager.1
        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            TokenManager.this.mAuth = null;
            TokenManager.this.mServiceStatus = 0;
            Log.d("TokenManager_20170717", "onServiceDisconnected");
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TokenManager.this.mAuth = IAuth.Stub.asInterface(iBinder);
            if (TokenManager.this.mAuth == null) {
                TokenManager.this.mServiceStatus = 0;
            } else {
                TokenManager.this.mServiceStatus = 2;
            }
            Log.d("TokenManager_20170717", "onServiceConnected, mServiceStatus = " + TokenManager.this.mServiceStatus);
        }
    };

    public TokenManager(Context context) {
        this.mServiceStatus = 0;
        this.mContext = context;
        this.mServiceStatus = 1;
        new Thread(new Runnable() { // from class: com.chinamobile.middleware.auth.TokenManager.2
            @Override // java.lang.Runnable
            public void run() {
                TokenManager.this.bind();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bind() {
        boolean zBindService;
        if (this.mServiceStatus == 2) {
            return;
        }
        Log.d("TokenManager_20170717", "Bind start!");
        Intent intent = new Intent(this.action);
        try {
            intent.setPackage(BuildConfig.APPLICATION_ID);
            zBindService = this.mContext.bindService(intent, this.mConn, 1);
            try {
                this.mServiceStatus = 1;
            } catch (Exception e) {
                e = e;
                e.printStackTrace();
            }
        } catch (Exception e2) {
            e = e2;
            zBindService = false;
        }
        if (!zBindService) {
            unbind();
            this.mServiceStatus = 0;
        }
        Log.d("TokenManager_20170717", "bind result =" + zBindService + ", mServiceStatus = " + this.mServiceStatus);
    }

    public synchronized void stop() {
        Log.d("TokenManager_20170717", "stop");
        this.mServiceStatus = 1;
        unbind();
    }

    private void unbind() {
        if (this.mServiceStatus != 0) {
            Log.d("TokenManager_20170717", " unbind");
            try {
                try {
                    this.mContext.unbindService(this.mConn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                this.mServiceStatus = 0;
            }
        }
    }

    /* JADX INFO: Removed unreachable split cross block B:60:0x0096 */
    /* JADX INFO: Removed unreachable split cross block B:63:0x0096 */
    /* JADX WARN: Code duplicated, block: B:31:0x0051 A[Catch: all -> 0x0099, TRY_LEAVE, TryCatch #2 {, blocks: (B:3:0x0001, B:5:0x000d, B:6:0x000f, B:8:0x0013, B:9:0x001d, B:12:0x0024, B:18:0x002f, B:24:0x003b, B:26:0x003f, B:27:0x0042, B:28:0x0044, B:21:0x0034, B:29:0x0047, B:31:0x0051, B:48:0x0098, B:33:0x005b, B:35:0x0066, B:37:0x0071, B:38:0x007d, B:43:0x0084, B:45:0x008c), top: B:59:0x0001, inners: #0, #1 }] */
    /* JADX WARN: Code duplicated, block: B:35:0x0066 A[Catch: all -> 0x0081, Exception -> 0x0083, RemoteException -> 0x008b, TryCatch #3 {RemoteException -> 0x008b, blocks: (B:33:0x005b, B:35:0x0066, B:37:0x0071, B:38:0x007d), top: B:61:0x005b, outer: #0 }] */
    /* JADX WARN: Code duplicated, block: B:37:0x0071 A[Catch: all -> 0x0081, Exception -> 0x0083, RemoteException -> 0x008b, TryCatch #3 {RemoteException -> 0x008b, blocks: (B:33:0x005b, B:35:0x0066, B:37:0x0071, B:38:0x007d), top: B:61:0x005b, outer: #0 }] */
    /* JADX WARN: Code duplicated, block: B:38:0x007d A[Catch: all -> 0x0081, Exception -> 0x0083, RemoteException -> 0x008b, TRY_LEAVE, TryCatch #3 {RemoteException -> 0x008b, blocks: (B:33:0x005b, B:35:0x0066, B:37:0x0071, B:38:0x007d), top: B:61:0x005b, outer: #0 }] */
    /* JADX WARN: Code duplicated, block: B:61:0x005b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public synchronized CMCCAuthResult getTokenResult() {
        CMCCAuthResult cMCCAuthResult;
        String token;
        Map infos;
        Log.i("TokenManager_20170717", "getTokenResult:   这是个新打的jar包");
        if (this.mAuth == null) {
            this.mServiceStatus = 0;
        }
        if (this.mServiceStatus == 0) {
            Log.d("TokenManager_20170717", "getTokenResult bind!");
            bind();
        }
        if (this.mServiceStatus == 1) {
            for (int i = 0; this.mServiceStatus != 2 && i < 45; i++) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (i > 15) {
                    if (this.mAuth == null) {
                        this.mServiceStatus = 0;
                    } else {
                        this.mServiceStatus = 2;
                    }
                }
            }
            cMCCAuthResult = new CMCCAuthResult();
            if (this.mServiceStatus != 2) {
                cMCCAuthResult.setStatusCode(1);
                cMCCAuthResult.setToken(null);
                unbind();
            } else {
                try {
                    try {
                        token = this.mAuth.getToken();
                        cMCCAuthResult.setToken(token);
                        if (token == null) {
                            cMCCAuthResult.setStatusCode(2);
                            infos = this.mAuth.getInfos();
                            if (infos != null) {
                                cMCCAuthResult.setErrorCode((String) infos.get(INFO_KEY_ERROR));
                            }
                        } else {
                            cMCCAuthResult.setStatusCode(0);
                        }
                    } catch (RemoteException e2) {
                        e2.printStackTrace();
                        cMCCAuthResult.setStatusCode(3);
                        cMCCAuthResult.setToken(null);
                    }
                } catch (Exception e3) {
                    e3.printStackTrace();
                    cMCCAuthResult.setStatusCode(2);
                }
            }
        } else {
            cMCCAuthResult = new CMCCAuthResult();
            if (this.mServiceStatus != 2) {
                cMCCAuthResult.setStatusCode(1);
                cMCCAuthResult.setToken(null);
                unbind();
            } else {
                token = this.mAuth.getToken();
                cMCCAuthResult.setToken(token);
                if (token == null) {
                    cMCCAuthResult.setStatusCode(2);
                    infos = this.mAuth.getInfos();
                    if (infos != null) {
                        cMCCAuthResult.setErrorCode((String) infos.get(INFO_KEY_ERROR));
                    }
                } else {
                    cMCCAuthResult.setStatusCode(0);
                }
            }
        }
        throw th;
        return cMCCAuthResult;
    }

    /* JADX WARN: Code duplicated, block: B:33:0x0056 A[Catch: all -> 0x0073, TRY_LEAVE, TryCatch #1 {, blocks: (B:3:0x0001, B:5:0x001b, B:8:0x0020, B:10:0x0024, B:11:0x0027, B:14:0x002e, B:20:0x0039, B:26:0x0045, B:28:0x0049, B:29:0x004c, B:30:0x004e, B:23:0x003e, B:31:0x0051, B:33:0x0056, B:45:0x006f, B:35:0x005b, B:40:0x0065, B:42:0x006a), top: B:57:0x0001, inners: #0, #3 }] */
    /* JADX WARN: Code duplicated, block: B:58:0x005b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public synchronized int reportTokenStatus(byte[] bArr) {
        int i;
        Log.d("TokenManager_20170717", "reportTokenStatus mServiceStatus = " + this.mServiceStatus);
        if (bArr != null && bArr.length == 2) {
            if (this.mServiceStatus == 0) {
                bind();
            }
            if (this.mServiceStatus == 1) {
                for (int i2 = 0; this.mServiceStatus != 2 && i2 < 45; i2++) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (i2 > 15) {
                        if (this.mAuth == null) {
                            this.mServiceStatus = 0;
                        } else {
                            this.mServiceStatus = 2;
                        }
                    }
                }
                i = 3;
                if (this.mServiceStatus != 2) {
                    unbind();
                    i = 1;
                } else {
                    try {
                        try {
                            this.mAuth.reportTokenStatus(bArr);
                            i = 0;
                        } catch (RemoteException e2) {
                            e2.printStackTrace();
                        }
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                return i;
            }
            i = 3;
            if (this.mServiceStatus != 2) {
                unbind();
                i = 1;
            } else {
                this.mAuth.reportTokenStatus(bArr);
                i = 0;
            }
            return i;
            throw th;
        }
        return 4;
    }

    public void startCMCCAuth() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, "com.chinamobile.middleware.auth.loginui"));
        intent.putExtra(TYPE, "jar");
        try {
            this.mContext.startActivity(intent);
        } catch (Exception e) {
            Log.d("TokenManager_20170717", "error:  " + e.toString());
        }
    }
}
