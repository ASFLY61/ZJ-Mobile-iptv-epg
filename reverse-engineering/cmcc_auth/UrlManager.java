package com.chinamobile.middleware.auth.tools;

import cn.hutool.core.util.StrUtil;
import com.chinamobile.middleware.auth.service.MainAuthService;

/* JADX INFO: loaded from: classes.dex */
public class UrlManager {
    private String mHost;
    private String mPort;
    private final String AUTH_SUFFIX = "/EPG/XML/authentication_initial";
    private final String HEARTBIT_SUFFIX = "/EPG/XML/HeartBit";
    private final String REFRESHTOKEN_SUFFIX = "/EPG/XML/RefreshToken";
    private final String HEADER_SUFFIX = "/EPG/XML/authentication_initial_challenge_v1";
    private final String REGISTER_SUFFIX = "/EPG/XML/STBRegister";

    public void setUrlData(String str, String str2) {
        this.mHost = str;
        this.mPort = str2;
        MainAuthService.LogMessage("Host = " + this.mHost + ", port =" + this.mPort);
    }

    public String getHost() {
        return this.mHost;
    }

    public String getPort() {
        return this.mPort;
    }

    public String getAuthUrl() {
        return "http://" + this.mHost + StrUtil.COLON + this.mPort + "/EPG/XML/authentication_initial";
    }

    public String getHearbitUrl() {
        return "http://" + this.mHost + StrUtil.COLON + this.mPort + "/EPG/XML/HeartBit";
    }

    public String getRefreshTokenUrl() {
        return "http://" + this.mHost + StrUtil.COLON + this.mPort + "/EPG/XML/RefreshToken";
    }

    public String getHeaderUrl() {
        MainAuthService.LogMessage("getHeaderUrl Host = " + this.mHost + ", port =" + this.mPort);
        String str = this.mHost;
        if (str == null || str.equals("")) {
            return null;
        }
        return "http://" + this.mHost + StrUtil.COLON + this.mPort + "/EPG/XML/authentication_initial_challenge_v1";
    }

    public String getRegisterUrl() {
        String str = this.mHost;
        if (str == null || str.equals("")) {
            return null;
        }
        return "http://" + this.mHost + StrUtil.COLON + this.mPort + "/EPG/XML/STBRegister";
    }
}
