package com.chinamobile.middleware.auth.impl;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.chinamobile.middleware.auth.AuthManagerInterface;
import com.chinamobile.middleware.auth.DataObject;
import com.chinamobile.middleware.auth.R;
import com.chinamobile.middleware.auth.config.Config;
import com.chinamobile.middleware.auth.contentprovider.IDataSaver;
import com.chinamobile.middleware.auth.service.MainAuthService;
import com.chinamobile.middleware.auth.tools.AuthLogger;
import com.chinamobile.middleware.auth.tools.HttpClient4Utils;
import com.chinamobile.middleware.auth.tools.HttpDigestTools;
import com.chinamobile.middleware.auth.tools.HttpResult;
import com.chinamobile.middleware.auth.tools.Md5Util;
import com.chinamobile.middleware.auth.tools.NetServiceException;
import com.chinamobile.middleware.auth.tools.PropManager;
import com.chinamobile.middleware.auth.tools.SeverServiceException;
import com.chinamobile.middleware.auth.tools.UrlManager;
import com.chinamobile.middleware.auth.tools.XMLParseCls;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import org.apache.http.Header;
import org.apache.http.conn.ConnectTimeoutException;

/* JADX INFO: loaded from: classes.dex */
public class AuthManagerImpl implements AuthManagerInterface {
    public static final String HEART_BIT_URL = "http://YOUR_EDS_HOST:PORT/EDS/XML/HeartBit";
    private static final String LOGIN_BY_USIM_SERVER_URL = "";
    public static final String REFRESH_TOKEN_URL = "http://YOUR_EDS_HOST:PORT/EDS/XML/RefreshToken";
    private static final String REQUEST_USIM_SERVER_URL = "";
    private static final String TAG = "AuthManagerImpl";
    private AuthLogger log = AuthLogger.getLogger(getClass());
    private Context mContext;
    MyHandler mHandler;
    private HttpClient4Utils mHttpClient4Utils;
    private String mStbId;
    private UrlManager mUrlManager;
    private XMLParseCls mXMLParseCls;
    private static String URL = "http://218.206.177.138:3690";
    private static String SEND_PHONE_NUMBER_SERVER_URL = URL + "/device-server-service/userinfo/verifycode";
    private static String REQUEST_REGISTER_INFO_SERVER_URL = URL + "/device-server-service/userinfo";
    private static String REQUEST_REGISTER_STB_SERVER_URL = URL + "/device-server-service/userinfo/init";
    private static String LOGIN_BY_PHONE_SERVER_URL = URL + "/device-server-service/pub/authentication_initial_challenge_v1";
    private static String LOGIN_SERVER_URL = URL + "/device-server-service/pub/authentication_initial";
    private static String LOGIN_BY_PHONE_SERVER_URL_BACKUP = "";
    private static String REQUEST_REGISTER_STB_SERVER_URL_BACKUP = "";
    public static boolean isSuccess = false;
    private static int count = 0;
    private static int mHeaderUrlType = 0;
    private static int mRegisterUrlType = 0;

    public static void init() {
        URL = Config.SERVERURL;
        REQUEST_REGISTER_STB_SERVER_URL = URL + "/EDS/STBRegister";
        LOGIN_BY_PHONE_SERVER_URL = URL + "/EDS/pub/authentication_initial_challenge_v1";
        LOGIN_SERVER_URL = URL + "/EDS/pub/authentication_initial";
        LOGIN_BY_PHONE_SERVER_URL_BACKUP = "http://YOUR_EDS_HOST2:PORT/EDS/pub/authentication_initial_challenge_v1";
        REQUEST_REGISTER_STB_SERVER_URL_BACKUP = "http://YOUR_EDS_HOST2:PORT/EDS/STBRegister";
    }

    public AuthManagerImpl(Context context, IDataSaver iDataSaver, UrlManager urlManager) {
        init();
        this.mUrlManager = urlManager;
        if (this.mUrlManager == null) {
            this.mUrlManager = new UrlManager();
        }
        this.mContext = context;
        this.mHttpClient4Utils = new HttpClient4Utils();
        this.mStbId = PropManager.getSystemProperties("ro.serialno");
        this.mXMLParseCls = new XMLParseCls();
        this.mHandler = new MyHandler();
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void requestUSIM(String str, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("IMSI", str);
            map.put("STBID", this.mStbId);
            resultCallBack.onResult(2, this.mHttpClient4Utils.postMethodString("", this.mXMLParseCls.getPostInfo("CardAuthentication", map), false), null);
        } catch (Exception e) {
            this.log.d(e.toString());
            e.printStackTrace();
        }
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void loginByUSIM(String str, String str2, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("RES", str2);
            map.put("STBID", this.mStbId);
            map.put("IMSI", str);
            resultCallBack.onResult(1, this.mHttpClient4Utils.postMethodString("", this.mXMLParseCls.getPostInfo("CardAuthentication", map), false), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void sendPhoneNumber(String str, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("MobileNumber", str);
            map.put("STBID", this.mStbId);
            String strPostMethodString = this.mHttpClient4Utils.postMethodString(SEND_PHONE_NUMBER_SERVER_URL, this.mXMLParseCls.getPostInfo("VerifyReq", map), false);
            this.log.d("======sendPhoneNumber====" + strPostMethodString);
            resultCallBack.onResult(1, strPostMethodString, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void loginByPhoneNum(String str, String str2, String str3, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("MobileNumber", str);
            map.put("VerifyCode", str2);
            map.put("STBID", str3);
            resultCallBack.onResult(1, this.mHttpClient4Utils.postMethodString(REQUEST_REGISTER_INFO_SERVER_URL, this.mXMLParseCls.getPostInfo("UserInfoReq", map), false), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MyHandler extends Handler {
        MyHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 0) {
                new AlertDialog.Builder(AuthManagerImpl.this.mContext).setTitle("提示").setMessage((String) message.obj).setIcon(R.drawable.ic_launcher).create().show();
            }
            super.handleMessage(message);
        }
    }

    public String getHeaderUrl(int i) {
        String headerUrl;
        if (i == 1) {
            headerUrl = LOGIN_BY_PHONE_SERVER_URL_BACKUP;
        } else {
            headerUrl = i == 2 ? this.mUrlManager.getHeaderUrl() : null;
        }
        if (headerUrl != null) {
            return headerUrl;
        }
        mHeaderUrlType = 0;
        return LOGIN_BY_PHONE_SERVER_URL;
    }

    /* JADX WARN: Code duplicated, block: B:138:0x038e  */
    /* JADX WARN: Code duplicated, block: B:141:0x039f  */
    /* JADX WARN: Code duplicated, block: B:144:0x03b3  */
    /* JADX WARN: Code duplicated, block: B:150:0x03c6  */
    /* JADX WARN: Code duplicated, block: B:162:0x0403  */
    /* JADX WARN: Code duplicated, block: B:172:0x0435  */
    /* JADX WARN: Unreachable blocks removed: 2, instructions: 4 */
    /* JADX WARN: Unreachable blocks removed: 2, instructions: 6 */
    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void login(String str, String str2, AuthManagerInterface.ResultCallBack resultCallBack) {
        int i;
        ConnectTimeoutException connectTimeoutException;
        Object obj;
        int i2;
        TimeoutException timeoutException;
        Object obj2;
        UnknownHostException unknownHostException;
        Object obj3;
        ConnectException connectException;
        Object obj4;
        int i3;
        NetServiceException netServiceException;
        Object obj5;
        String str3;
        Exception exc;
        SeverServiceException severServiceException;
        int i4;
        int i5;
        int i6;
        int i7;
        Exception e;
        String str4 = "";
        HashMap<String, String> map = new HashMap<>();
        HashMap map2 = new HashMap();
        HttpResult httpResult = new HttpResult();
        try {
            try {
                map.put("STBID", this.mStbId);
                String postInfo = this.mXMLParseCls.getPostInfo("AuthenticationRequest", map);
                String headerUrl = getHeaderUrl(mHeaderUrlType);
                httpResult.setHttpType(HttpResult.TYPE_authentication_initial_challenge);
                try {
                    Header[] headerArrPostMethodHeader = this.mHttpClient4Utils.postMethodHeader(headerUrl, "", this.mUrlManager, false, httpResult);
                    mHeaderUrlType = 0;
                    String value = "";
                    for (Header header : headerArrPostMethodHeader) {
                        try {
                            value = header.getValue();
                        } catch (NetServiceException e2) {
                            resultCallBack = resultCallBack;
                            i = 1;
                            obj5 = null;
                            netServiceException = e2;
                            netServiceException.printStackTrace();
                            i7 = mHeaderUrlType;
                            if (i7 != 0) {
                                mHeaderUrlType = i7 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(5, obj5, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (ConnectException e3) {
                            connectException = e3;
                            resultCallBack = resultCallBack;
                            i3 = 2;
                            i = 1;
                            obj4 = null;
                            connectException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i3, obj4, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (UnknownHostException e4) {
                            resultCallBack = resultCallBack;
                            i = 1;
                            obj3 = null;
                            unknownHostException = e4;
                            unknownHostException.printStackTrace();
                            i6 = mHeaderUrlType;
                            if (i6 != 0) {
                                mHeaderUrlType = i6 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(7, obj3, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (TimeoutException e5) {
                            resultCallBack = resultCallBack;
                            i = 1;
                            obj2 = null;
                            timeoutException = e5;
                            timeoutException.printStackTrace();
                            i5 = mHeaderUrlType;
                            if (i5 != 0) {
                                mHeaderUrlType = i5 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(2, obj2, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (ConnectTimeoutException e6) {
                            connectTimeoutException = e6;
                            resultCallBack = resultCallBack;
                            i2 = 2;
                            i = 1;
                            obj = null;
                            connectTimeoutException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i2, obj, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        }
                    }
                    this.log.d("得到的header===" + value);
                    HashMap map3 = new HashMap();
                    String[] strArrSplit = value.substring(7, value.length()).split(",");
                    int length = strArrSplit.length;
                    int i8 = 0;
                    while (i8 < length) {
                        try {
                            try {
                                String str5 = strArrSplit[i8];
                                String[] strArrSplit2 = str5.split("=");
                                String[] strArr = strArrSplit;
                                int length2 = strArrSplit2.length;
                                int i9 = length;
                                int i10 = 0;
                                while (i10 < length2) {
                                    String str6 = strArrSplit2[i10];
                                    int i11 = length2;
                                    String[] strArrSplit3 = str5.split("\"");
                                    String str7 = str5;
                                    if (strArrSplit3.length == 1) {
                                        try {
                                            map3.put(strArrSplit2[0], str4);
                                            str3 = str4;
                                        } catch (Exception e7) {
                                            exc = e7;
                                            str3 = str4;
                                            i = 1;
                                            exc.printStackTrace();
                                            i4 = mHeaderUrlType;
                                            if (i4 != 0) {
                                                mHeaderUrlType = i4 + i;
                                                mHeaderUrlType %= 3;
                                            }
                                            if (httpResult.getHttpType() != HttpResult.TYPE_authentication_initial_challenge) {
                                                resultCallBack.onResult(6, null, httpResult);
                                            } else {
                                                resultCallBack.onResult(6, null, httpResult);
                                            }
                                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                                        }
                                    } else {
                                        str3 = str4;
                                        try {
                                            map3.put(strArrSplit2[0], strArrSplit3[1]);
                                        } catch (Exception e8) {
                                            e = e8;
                                            exc = e;
                                            i = 1;
                                            exc.printStackTrace();
                                            i4 = mHeaderUrlType;
                                            if (i4 != 0) {
                                                mHeaderUrlType = i4 + i;
                                                mHeaderUrlType %= 3;
                                            }
                                            if (httpResult.getHttpType() != HttpResult.TYPE_authentication_initial_challenge && httpResult.getHttpCode() > 0) {
                                                resultCallBack.onResult(i, new DataObject(str3, this.mUrlManager), httpResult);
                                            } else {
                                                resultCallBack.onResult(6, null, httpResult);
                                            }
                                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                                        }
                                    }
                                    i10++;
                                    str4 = str3;
                                    length2 = i11;
                                    str5 = str7;
                                }
                                i8++;
                                strArrSplit = strArr;
                                length = i9;
                            } catch (Exception e9) {
                                e = e9;
                                str3 = str4;
                                exc = e;
                                i = 1;
                                exc.printStackTrace();
                                i4 = mHeaderUrlType;
                                if (i4 != 0) {
                                    mHeaderUrlType = i4 + i;
                                    mHeaderUrlType %= 3;
                                }
                                if (httpResult.getHttpType() != HttpResult.TYPE_authentication_initial_challenge) {
                                    resultCallBack.onResult(6, null, httpResult);
                                } else {
                                    resultCallBack.onResult(6, null, httpResult);
                                }
                                MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                            }
                        } catch (NetServiceException e10) {
                            resultCallBack = resultCallBack;
                            netServiceException = e10;
                            i = 1;
                            obj5 = null;
                            netServiceException.printStackTrace();
                            i7 = mHeaderUrlType;
                            if (i7 != 0) {
                                mHeaderUrlType = i7 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(5, obj5, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (SeverServiceException e11) {
                            resultCallBack = resultCallBack;
                            severServiceException = e11;
                            i = 1;
                            severServiceException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i, new DataObject(severServiceException.getMessage(), this.mUrlManager), httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (ConnectException e12) {
                            resultCallBack = resultCallBack;
                            connectException = e12;
                            i3 = 2;
                            i = 1;
                            obj4 = null;
                            connectException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i3, obj4, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (UnknownHostException e13) {
                            resultCallBack = resultCallBack;
                            unknownHostException = e13;
                            i = 1;
                            obj3 = null;
                            unknownHostException.printStackTrace();
                            i6 = mHeaderUrlType;
                            if (i6 != 0) {
                                mHeaderUrlType = i6 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(7, obj3, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (TimeoutException e14) {
                            resultCallBack = resultCallBack;
                            timeoutException = e14;
                            i = 1;
                            obj2 = null;
                            timeoutException.printStackTrace();
                            i5 = mHeaderUrlType;
                            if (i5 != 0) {
                                mHeaderUrlType = i5 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(2, obj2, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        } catch (ConnectTimeoutException e15) {
                            resultCallBack = resultCallBack;
                            connectTimeoutException = e15;
                            i2 = 2;
                            i = 1;
                            obj = null;
                            connectTimeoutException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i2, obj, httpResult);
                            MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                        }
                    }
                    str3 = str4;
                    try {
                        String str8 = (String) map3.get(HttpDigestTools.REALM);
                        String str9 = (String) map3.get(HttpDigestTools.NONCE);
                        String str10 = (String) map3.get(HttpDigestTools.QOP);
                        String str11 = "Digest realm=\"" + str8 + "\",nonce=\"" + str9 + "\",opaque=\"" + ((String) map3.get("opaque")) + "\",algorithm=\"" + ((String) map3.get("algorithm")) + "\",qop=\"" + str10 + "\"";
                        String strValueOf = String.valueOf(HttpDigestTools.countCN(1));
                        String mD5String = Md5Util.getMD5String(String.valueOf(HttpDigestTools.cnonce()));
                        String mD5String2 = Md5Util.getMD5String(str + StrUtil.COLON + str8 + StrUtil.COLON + XMLParseCls.getBASE64(str2));
                        StringBuilder sb = new StringBuilder();
                        sb.append("POST:/EDS/pub/authentication_initial:");
                        sb.append(Md5Util.getMD5String(postInfo));
                        String mD5String3 = Md5Util.getMD5String(sb.toString());
                        String mD5String4 = Md5Util.getMD5String(mD5String2 + StrUtil.COLON + str9 + StrUtil.COLON + strValueOf + StrUtil.COLON + mD5String + StrUtil.COLON + str10 + StrUtil.COLON + mD5String3);
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(str11);
                        sb2.append(", userid=\"");
                        sb2.append(str);
                        sb2.append("\",nc=\"");
                        sb2.append(strValueOf);
                        sb2.append("\",cnonce=\"");
                        sb2.append(mD5String);
                        sb2.append("\",response=\"");
                        sb2.append(mD5String4);
                        sb2.append("\"");
                        String string = sb2.toString();
                        this.log.d("请求的header===" + string);
                        this.log.d("HA1=MD5( username: realm: password)的值：" + mD5String2);
                        this.log.d("MD5（entity-body）的值：" + Md5Util.getMD5String(postInfo));
                        this.log.d("HA2=MD5(method: URI：MD5（entity-body）的值：" + mD5String3);
                        this.log.d("最后response的值：" + mD5String4);
                        map2.put("Authorization", string);
                        Header[] header2 = this.mHttpClient4Utils.getHeader(map2);
                        for (Header header3 : header2) {
                        }
                        String str12 = LOGIN_SERVER_URL;
                        String authUrl = this.mUrlManager.getAuthUrl();
                        this.log.d("request url======" + authUrl);
                        httpResult.setHttpType(HttpResult.TYPE_authentication_initial);
                        String strPostMethodString = this.mHttpClient4Utils.postMethodString(header2, authUrl, postInfo, httpResult);
                        this.log.d("result======" + strPostMethodString);
                        DataObject dataObject = new DataObject(strPostMethodString, this.mUrlManager);
                        resultCallBack = resultCallBack;
                        i = 1;
                        try {
                            resultCallBack.onResult(1, dataObject, httpResult);
                        } catch (NetServiceException e16) {
                            e = e16;
                            netServiceException = e;
                            obj5 = null;
                            netServiceException.printStackTrace();
                            i7 = mHeaderUrlType;
                            if (i7 != 0) {
                                mHeaderUrlType = i7 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(5, obj5, httpResult);
                        } catch (SeverServiceException e17) {
                            e = e17;
                            severServiceException = e;
                            severServiceException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i, new DataObject(severServiceException.getMessage(), this.mUrlManager), httpResult);
                        } catch (ConnectException e18) {
                            e = e18;
                            connectException = e;
                            i3 = 2;
                            obj4 = null;
                            connectException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i3, obj4, httpResult);
                        } catch (UnknownHostException e19) {
                            e = e19;
                            unknownHostException = e;
                            obj3 = null;
                            unknownHostException.printStackTrace();
                            i6 = mHeaderUrlType;
                            if (i6 != 0) {
                                mHeaderUrlType = i6 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(7, obj3, httpResult);
                        } catch (TimeoutException e20) {
                            e = e20;
                            timeoutException = e;
                            obj2 = null;
                            timeoutException.printStackTrace();
                            i5 = mHeaderUrlType;
                            if (i5 != 0) {
                                mHeaderUrlType = i5 + i;
                                mHeaderUrlType %= 3;
                            }
                            resultCallBack.onResult(2, obj2, httpResult);
                        } catch (ConnectTimeoutException e21) {
                            e = e21;
                            connectTimeoutException = e;
                            i2 = 2;
                            obj = null;
                            connectTimeoutException.printStackTrace();
                            mHeaderUrlType += i;
                            mHeaderUrlType %= 3;
                            resultCallBack.onResult(i2, obj, httpResult);
                        } catch (Exception e22) {
                            e = e22;
                            exc = e;
                            exc.printStackTrace();
                            i4 = mHeaderUrlType;
                            if (i4 != 0) {
                                mHeaderUrlType = i4 + i;
                                mHeaderUrlType %= 3;
                            }
                            if (httpResult.getHttpType() != HttpResult.TYPE_authentication_initial_challenge) {
                                resultCallBack.onResult(6, null, httpResult);
                            } else {
                                resultCallBack.onResult(6, null, httpResult);
                            }
                        }
                    } catch (NetServiceException e23) {
                        e = e23;
                        resultCallBack = resultCallBack;
                        i = 1;
                        netServiceException = e;
                        obj5 = null;
                        netServiceException.printStackTrace();
                        i7 = mHeaderUrlType;
                        if (i7 != 0) {
                            mHeaderUrlType = i7 + i;
                            mHeaderUrlType %= 3;
                        }
                        resultCallBack.onResult(5, obj5, httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (SeverServiceException e24) {
                        e = e24;
                        resultCallBack = resultCallBack;
                        i = 1;
                        severServiceException = e;
                        severServiceException.printStackTrace();
                        mHeaderUrlType += i;
                        mHeaderUrlType %= 3;
                        resultCallBack.onResult(i, new DataObject(severServiceException.getMessage(), this.mUrlManager), httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (ConnectException e25) {
                        e = e25;
                        resultCallBack = resultCallBack;
                        i = 1;
                        connectException = e;
                        i3 = 2;
                        obj4 = null;
                        connectException.printStackTrace();
                        mHeaderUrlType += i;
                        mHeaderUrlType %= 3;
                        resultCallBack.onResult(i3, obj4, httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (UnknownHostException e26) {
                        e = e26;
                        resultCallBack = resultCallBack;
                        i = 1;
                        unknownHostException = e;
                        obj3 = null;
                        unknownHostException.printStackTrace();
                        i6 = mHeaderUrlType;
                        if (i6 != 0) {
                            mHeaderUrlType = i6 + i;
                            mHeaderUrlType %= 3;
                        }
                        resultCallBack.onResult(7, obj3, httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (TimeoutException e27) {
                        e = e27;
                        resultCallBack = resultCallBack;
                        i = 1;
                        timeoutException = e;
                        obj2 = null;
                        timeoutException.printStackTrace();
                        i5 = mHeaderUrlType;
                        if (i5 != 0) {
                            mHeaderUrlType = i5 + i;
                            mHeaderUrlType %= 3;
                        }
                        resultCallBack.onResult(2, obj2, httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (ConnectTimeoutException e28) {
                        e = e28;
                        resultCallBack = resultCallBack;
                        i = 1;
                        connectTimeoutException = e;
                        i2 = 2;
                        obj = null;
                        connectTimeoutException.printStackTrace();
                        mHeaderUrlType += i;
                        mHeaderUrlType %= 3;
                        resultCallBack.onResult(i2, obj, httpResult);
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    } catch (Exception e29) {
                        e = e29;
                        resultCallBack = resultCallBack;
                        i = 1;
                        exc = e;
                        exc.printStackTrace();
                        i4 = mHeaderUrlType;
                        if (i4 != 0) {
                            mHeaderUrlType = i4 + i;
                            mHeaderUrlType %= 3;
                        }
                        if (httpResult.getHttpType() != HttpResult.TYPE_authentication_initial_challenge) {
                            resultCallBack.onResult(6, null, httpResult);
                        } else {
                            resultCallBack.onResult(6, null, httpResult);
                        }
                        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
                    }
                } catch (NetServiceException e30) {
                    e = e30;
                    resultCallBack = resultCallBack;
                } catch (ConnectException e31) {
                    e = e31;
                    resultCallBack = resultCallBack;
                } catch (UnknownHostException e32) {
                    e = e32;
                    resultCallBack = resultCallBack;
                } catch (TimeoutException e33) {
                    e = e33;
                    resultCallBack = resultCallBack;
                } catch (ConnectTimeoutException e34) {
                    e = e34;
                    resultCallBack = resultCallBack;
                }
            } catch (SeverServiceException e35) {
                e = e35;
                resultCallBack = resultCallBack;
            } catch (Exception e36) {
                e = e36;
                resultCallBack = resultCallBack;
                str3 = "";
            }
        } catch (NetServiceException e37) {
            resultCallBack = resultCallBack;
            i = 1;
            netServiceException = e37;
            obj5 = null;
        } catch (ConnectException e38) {
            resultCallBack = resultCallBack;
            i = 1;
            connectException = e38;
            obj4 = null;
            i3 = 2;
        } catch (UnknownHostException e39) {
            resultCallBack = resultCallBack;
            i = 1;
            unknownHostException = e39;
            obj3 = null;
        } catch (TimeoutException e40) {
            resultCallBack = resultCallBack;
            i = 1;
            timeoutException = e40;
            obj2 = null;
        } catch (ConnectTimeoutException e41) {
            resultCallBack = resultCallBack;
            i = 1;
            connectTimeoutException = e41;
            obj = null;
            i2 = 2;
        }
        MainAuthService.LogMessage("mHeaderUrlType = " + mHeaderUrlType);
    }

    public String getRegisterUrl(int i) {
        String registerUrl;
        if (i == 1) {
            registerUrl = REQUEST_REGISTER_STB_SERVER_URL_BACKUP;
        } else {
            registerUrl = i == 2 ? this.mUrlManager.getRegisterUrl() : null;
        }
        if (registerUrl != null) {
            return registerUrl;
        }
        mRegisterUrlType = 0;
        return REQUEST_REGISTER_STB_SERVER_URL;
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void requestRegisterInfo(String str, AuthManagerInterface.ResultCallBack resultCallBack) {
        HttpResult httpResult = new HttpResult();
        HashMap<String, String> map = new HashMap<>();
        try {
            map.put("STBID", str);
            String postInfo = this.mXMLParseCls.getPostInfo("STBRegisterReq", map);
            String registerUrl = getRegisterUrl(mRegisterUrlType);
            httpResult.setHttpType(HttpResult.TYPE_STBRegister);
            String strPostMethodString = this.mHttpClient4Utils.postMethodString(registerUrl, postInfo, false, httpResult);
            this.log.d("register result======" + strPostMethodString);
            resultCallBack.onResult(1, strPostMethodString, httpResult);
            mRegisterUrlType = 0;
        } catch (NetServiceException e) {
            e.printStackTrace();
            int i = mRegisterUrlType;
            if (i != 0) {
                mRegisterUrlType = i + 1;
                mRegisterUrlType %= 3;
            }
            resultCallBack.onResult(5, null, httpResult);
        } catch (SeverServiceException e2) {
            e2.printStackTrace();
            mRegisterUrlType++;
            mRegisterUrlType %= 3;
            resultCallBack.onResult(1, new String(e2.getMessage()), httpResult);
        } catch (ConnectException e3) {
            e3.printStackTrace();
            mRegisterUrlType++;
            mRegisterUrlType %= 3;
            resultCallBack.onResult(2, null, httpResult);
        } catch (UnknownHostException e4) {
            e4.printStackTrace();
            int i2 = mRegisterUrlType;
            if (i2 != 0) {
                mRegisterUrlType = i2 + 1;
                mRegisterUrlType %= 3;
            }
            resultCallBack.onResult(7, null, httpResult);
        } catch (TimeoutException unused) {
            int i3 = mRegisterUrlType;
            if (i3 != 0) {
                mRegisterUrlType = i3 + 1;
                mRegisterUrlType %= 3;
            }
            resultCallBack.onResult(2, null, httpResult);
        } catch (ConnectTimeoutException e5) {
            e5.printStackTrace();
            mRegisterUrlType++;
            mRegisterUrlType %= 3;
            resultCallBack.onResult(2, null, httpResult);
        } catch (Exception e6) {
            e6.printStackTrace();
            int i4 = mRegisterUrlType;
            if (i4 != 0) {
                mRegisterUrlType = i4 + 1;
                mRegisterUrlType %= 3;
            }
            resultCallBack.onResult(6, null, httpResult);
        }
        MainAuthService.LogMessage(" mRegisterUrlType = " + mRegisterUrlType);
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void heartBit(String str, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap map = new HashMap();
        try {
            map.put("Authorization", "OAuth2 " + str);
            map.put("Content_Type", "text/x-markdown; charset=utf-8");
            map.put("charset", CharsetUtil.UTF_8);
            Header[] header = this.mHttpClient4Utils.getHeader(map);
            for (Header header2 : header) {
            }
            String strPostMethodString = this.mHttpClient4Utils.postMethodString(header, this.mUrlManager.getHearbitUrl(), "");
            this.log.d("url======http://YOUR_EDS_HOST:PORT/EDS/XML/HeartBit");
            this.log.d("result======" + strPostMethodString);
            resultCallBack.onResult(1, strPostMethodString, null);
        } catch (NetServiceException e) {
            e.printStackTrace();
            resultCallBack.onResult(5, null, null);
        } catch (TimeoutException e2) {
            e2.printStackTrace();
            resultCallBack.onResult(2, null, null);
        } catch (Exception e3) {
            e3.printStackTrace();
            resultCallBack.onResult(6, null, null);
        }
    }

    @Override // com.chinamobile.middleware.auth.AuthManagerInterface
    public void refreshToken(String str, AuthManagerInterface.ResultCallBack resultCallBack) {
        HashMap map = new HashMap();
        try {
            map.put("Authorization", "OAuth2 " + str);
            map.put("Content_Type", "text/x-markdown; charset=utf-8");
            map.put("charset", CharsetUtil.UTF_8);
            Header[] header = this.mHttpClient4Utils.getHeader(map);
            for (Header header2 : header) {
            }
            String refreshTokenUrl = this.mUrlManager.getRefreshTokenUrl();
            String strPostMethodString = this.mHttpClient4Utils.postMethodString(header, refreshTokenUrl, "");
            this.log.d("url======" + refreshTokenUrl);
            this.log.d("result======" + strPostMethodString);
            resultCallBack.onResult(1, strPostMethodString, null);
        } catch (NetServiceException e) {
            e.printStackTrace();
            resultCallBack.onResult(5, null, null);
        } catch (TimeoutException e2) {
            e2.printStackTrace();
            resultCallBack.onResult(2, null, null);
        } catch (Exception e3) {
            e3.printStackTrace();
            resultCallBack.onResult(6, null, null);
        }
    }
}
