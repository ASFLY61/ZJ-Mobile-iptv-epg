package com.pukka.ydepg.common.http.v6bean.v6response;

import com.google.gson.annotations.SerializedName;
import com.pukka.ydepg.common.http.v6bean.v6node.ChannelPlaybill;
import com.pukka.ydepg.common.http.v6bean.v6node.NamedParameter;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class QueryPlaybillListResponse extends BaseResponse {

    @SerializedName("channelPlaybills")
    private List<ChannelPlaybill> channelPlaybills;

    @SerializedName("extensionFields")
    private List<NamedParameter> extensionFields;

    @SerializedName("playbillVersion")
    private String playbillVersion;

    @SerializedName("total")
    private String total;

    public String getTotal() {
        return this.total;
    }

    public void setTotal(String str) {
        this.total = str;
    }

    public String getPlaybillVersion() {
        return this.playbillVersion;
    }

    public void setPlaybillVersion(String str) {
        this.playbillVersion = str;
    }

    public List<ChannelPlaybill> getChannelPlaybills() {
        return this.channelPlaybills;
    }

    public void setChannelPlaybills(List<ChannelPlaybill> list) {
        this.channelPlaybills = list;
    }

    public List<NamedParameter> getExtensionFields() {
        return this.extensionFields;
    }

    public void setExtensionFields(List<NamedParameter> list) {
        this.extensionFields = list;
    }
}
