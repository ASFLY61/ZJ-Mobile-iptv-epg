package com.chinamobile.middleware.auth.http;

import com.google.gson.annotations.SerializedName;

/* JADX INFO: loaded from: classes.dex */
public class QueryStbAccountResponse {

    @SerializedName("Result")
    private String Result;

    @SerializedName("errorParams")
    private String errorParams;

    @SerializedName("stbAccount")
    private String stbAccount;

    @SerializedName("stbPassword")
    private String stbPassword;

    public String getResult() {
        return this.Result;
    }

    public void setResult(String str) {
        this.Result = str;
    }

    public String getErrorParams() {
        return this.errorParams;
    }

    public void setErrorParams(String str) {
        this.errorParams = str;
    }

    public String getStbAccount() {
        return this.stbAccount;
    }

    public void setStbAccount(String str) {
        this.stbAccount = str;
    }

    public String getStbPassword() {
        return this.stbPassword;
    }

    public void setStbPassword(String str) {
        this.stbPassword = str;
    }
}
