package com.pukka.ydepg.common.http.v6bean.v6node;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import com.google.gson.annotations.SerializedName;
import com.pukka.ydepg.launcher.Constant;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class QueryChannel {

    @SerializedName("channelFilter")
    private ChannelFilter channelFilter;

    @SerializedName("channelIDs")
    private List<String> channelIDs;

    @SerializedName("channelNOs")
    private List<String> channelNOs;

    @SerializedName(Constant.CONTENT_TYPE)
    private String contentType;

    @SerializedName("count")
    private String count;

    @SerializedName("extensionFields")
    private List<NamedParameter> extensionFields;

    @SerializedName("isReturnAllMedia")
    private String isReturnAllMedia;

    @SerializedName(TypedValues.CycleType.S_WAVE_OFFSET)
    private String offset;

    @SerializedName("subjectID")
    private String subjectID;

    public String getIsReturnAllMedia() {
        return this.isReturnAllMedia;
    }

    public void setIsReturnAllMedia(String str) {
        this.isReturnAllMedia = str;
    }

    public String getSubjectID() {
        return this.subjectID;
    }

    public void setSubjectID(String str) {
        this.subjectID = str;
    }

    public String getCount() {
        return this.count;
    }

    public void setCount(String str) {
        this.count = str;
    }

    public String getOffset() {
        return this.offset;
    }

    public void setOffset(String str) {
        this.offset = str;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String str) {
        this.contentType = str;
    }

    public ChannelFilter getChannelFilter() {
        return this.channelFilter;
    }

    public void setChannelFilter(ChannelFilter channelFilter) {
        this.channelFilter = channelFilter;
    }

    public List<String> getChannelIDs() {
        return this.channelIDs;
    }

    public void setChannelIDs(List<String> list) {
        this.channelIDs = list;
    }

    public List<String> getChannelNOs() {
        return this.channelNOs;
    }

    public void setChannelNOs(List<String> list) {
        this.channelNOs = list;
    }

    public List<NamedParameter> getExtensionFields() {
        return this.extensionFields;
    }

    public void setExtensionFields(List<NamedParameter> list) {
        this.extensionFields = list;
    }
}
