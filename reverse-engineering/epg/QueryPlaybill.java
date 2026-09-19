package com.pukka.ydepg.common.http.v6bean.v6node;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import com.google.gson.annotations.SerializedName;
import com.pukka.ydepg.common.utils.ZJVRoute;
import java.io.Serializable;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class QueryPlaybill implements Serializable {

    @SerializedName("count")
    private String count;

    @SerializedName(ZJVRoute.ActionUrlKeyType.END_TIME)
    private String endTime;

    @SerializedName("extensionFields")
    private List<NamedParameter> extensionFields;

    @SerializedName("isFillProgram")
    private String isFillProgram;

    @SerializedName("mustIncluded")
    private String mustIncluded;

    @SerializedName(TypedValues.CycleType.S_WAVE_OFFSET)
    private String offset;

    @SerializedName("playbillExcluder")
    private PlaybillExcluder playbillExcluder;

    @SerializedName("playbillFilter")
    private PlaybillFilter playbillFilter;

    @SerializedName("sortType")
    private String sortType;

    @SerializedName(ZJVRoute.ActionUrlKeyType.START_TIME)
    private String startTime;

    @SerializedName("type")
    private String type;

    public String getType() {
        return this.type;
    }

    public void setType(String str) {
        this.type = str;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String str) {
        this.startTime = str;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String str) {
        this.endTime = str;
    }

    public String getMustIncluded() {
        return this.mustIncluded;
    }

    public void setMustIncluded(String str) {
        this.mustIncluded = str;
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

    public String getIsFillProgram() {
        return this.isFillProgram;
    }

    public void setIsFillProgram(String str) {
        this.isFillProgram = str;
    }

    public PlaybillFilter getPlaybillFilter() {
        return this.playbillFilter;
    }

    public void setPlaybillFilter(PlaybillFilter playbillFilter) {
        this.playbillFilter = playbillFilter;
    }

    public PlaybillExcluder getPlaybillExcluder() {
        return this.playbillExcluder;
    }

    public void setPlaybillExcluder(PlaybillExcluder playbillExcluder) {
        this.playbillExcluder = playbillExcluder;
    }

    public String getSortType() {
        return this.sortType;
    }

    public void setSortType(String str) {
        this.sortType = str;
    }

    public List<NamedParameter> getExtensionFields() {
        return this.extensionFields;
    }

    public void setExtensionFields(List<NamedParameter> list) {
        this.extensionFields = list;
    }
}
