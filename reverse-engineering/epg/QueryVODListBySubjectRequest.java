package com.pukka.ydepg.common.http.v6bean.v6request;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import com.google.gson.annotations.SerializedName;
import com.pukka.ydepg.common.http.v6bean.v6node.NamedParameter;
import com.pukka.ydepg.common.http.v6bean.v6node.VODExcluder;
import com.pukka.ydepg.common.http.v6bean.v6node.VODFilter;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class QueryVODListBySubjectRequest {

    @SerializedName("VODExcluder")
    private VODExcluder VODExcluder;

    @SerializedName("VODFilter")
    private VODFilter VODFilter;

    @SerializedName("count")
    private String count;

    @SerializedName("extensionFields")
    private List<NamedParameter> extensionFields;

    @SerializedName(TypedValues.CycleType.S_WAVE_OFFSET)
    private String offset;

    @SerializedName("sortType")
    private String sortType;

    @SerializedName("subjectID")
    private String subjectID;

    /* JADX INFO: loaded from: classes3.dex */
    public interface SortType {
        public static final String AVERAGE_SCORE_ASC = "AVGSCORE:ASC";
        public static final String AVERAGE_SCORE_DESC = "AVGSCORE:DESC";
        public static final String CNTARRANGE = "CNTARRANGE";
        public static final String CONTENT_ARRANGE_ASC = "CNTARRANGE:ASC";
        public static final String CONTENT_ARRANGE_DESC = "CNTARRANGE:DESC";
        public static final String PLAY_TIMES_ASC = "PLAYTIMES:ASC";
        public static final String PLAY_TIMES_DESC = "PLAYTIMES:DESC";
        public static final String START_TIME_ASC = "STARTTIME:ASC";
        public static final String START_TIME_DESC = "STARTTIME:DESC";
        public static final String VODNAME_ASC = "VODNAME:ASC";
        public static final String VODNAME_DESC = "VODNAME:DESC";
    }

    public String getSubjectID() {
        return this.subjectID;
    }

    public void setSubjectID(String str) {
        this.subjectID = str;
    }

    public void setSortType(String str) {
        this.sortType = str;
    }

    public void setCount(String str) {
        this.count = str;
    }

    public void setOffset(String str) {
        this.offset = str;
    }

    public void setVODFilter(VODFilter vODFilter) {
        this.VODFilter = vODFilter;
    }

    public void setVODExcluder(VODExcluder vODExcluder) {
        this.VODExcluder = vODExcluder;
    }

    public void setExtensionFields(List<NamedParameter> list) {
        this.extensionFields = list;
    }

    public String getSortType() {
        return this.sortType;
    }

    public String getCount() {
        return this.count;
    }

    public String getOffset() {
        return this.offset;
    }

    public VODFilter getVODFilter() {
        return this.VODFilter;
    }

    public VODExcluder getVODExcluder() {
        return this.VODExcluder;
    }

    public List<NamedParameter> getExtensionFields() {
        return this.extensionFields;
    }
}
