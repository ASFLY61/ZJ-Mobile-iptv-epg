package com.pukka.ydepg.common.http.v6bean.v6node;

import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;
import com.networkbench.nbslens.nbsnativecrashlib.m;
import com.pukka.ydepg.common.utils.ZJVRoute;
import com.pukka.ydepg.launcher.Constant;
import com.pukka.ydepg.moudule.player.ui.OnDemandVideoActivity;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class PlaybillLite extends Metadata {

    @SerializedName("CUTVStatus")
    private String CUTVStatus;

    @SerializedName(OnDemandVideoActivity.CHANNELID)
    private String channelID;

    @SerializedName(m.v)
    protected String code;

    @SerializedName("customFields")
    private List<NamedParameter> customFields;

    @SerializedName(ZJVRoute.ActionUrlKeyType.END_TIME)
    private String endTime;

    @SerializedName("hasRecordingPVR")
    private String hasRecordingPVR;

    @SerializedName("isBlackout")
    private String isBlackout;

    @SerializedName("isCPVR")
    private String isCPVR;

    @SerializedName("isCUTV")
    private String isCUTV;

    @SerializedName("isFillProgram")
    private String isFillProgram;

    @SerializedName("isInstantRestart")
    private String isInstantRestart;

    @SerializedName(Constant.IS_LOCKED)
    private String isLocked;

    @SerializedName("IsLookBack")
    private String isLookBack;

    @SerializedName("isNPVR")
    private String isNPVR;

    @SerializedName("IsShield")
    private String isShield;

    @SerializedName("playTimes")
    private String playTimes;

    @SerializedName("playbillSeries")
    private PlaybillSeries playbillSeries;

    @SerializedName("rating")
    private Rating rating;

    @SerializedName("recmExplain")
    private String recmExplain;

    @SerializedName("reminderStatus")
    private String reminderStatus;

    @SerializedName(ZJVRoute.ActionUrlKeyType.START_TIME)
    private String startTime;

    public List<NamedParameter> getCustomFields() {
        return this.customFields;
    }

    public void setCustomFields(List<NamedParameter> list) {
        this.customFields = list;
    }

    public String getIsLookBack() {
        return this.isLookBack;
    }

    public void setIsLookBack(String str) {
        this.isLookBack = str;
    }

    public String getIsShield() {
        return this.isShield;
    }

    public void setIsShield(String str) {
        this.isShield = str;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public String getID() {
        return this.ID;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public void setID(String str) {
        this.ID = str;
    }

    public String getChannelID() {
        return this.channelID;
    }

    public void setChannelID(String str) {
        this.channelID = str;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public String getName() {
        return this.name;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public void setName(String str) {
        this.name = str;
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

    public Rating getRating() {
        return this.rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public Picture getPicture() {
        return this.picture;
    }

    @Override // com.pukka.ydepg.common.http.v6bean.v6node.Metadata
    public void setPicture(Picture picture) {
        this.picture = picture;
    }

    public String getIsCUTV() {
        return this.isCUTV;
    }

    public void setIsCUTV(String str) {
        this.isCUTV = str;
    }

    public String getCUTVStatus() {
        return this.CUTVStatus;
    }

    public void setCUTVStatus(String str) {
        this.CUTVStatus = str;
    }

    public PlaybillSeries getPlaybillSeries() {
        return this.playbillSeries;
    }

    public void setPlaybillSeries(PlaybillSeries playbillSeries) {
        this.playbillSeries = playbillSeries;
    }

    public String getIsLocked() {
        return this.isLocked;
    }

    public void setIsLocked(String str) {
        this.isLocked = str;
    }

    public String getReminderStatus() {
        return this.reminderStatus;
    }

    public void setReminderStatus(String str) {
        this.reminderStatus = str;
    }

    public String getRecmExplain() {
        return this.recmExplain;
    }

    public void setRecmExplain(String str) {
        this.recmExplain = str;
    }

    public String getPlayTimes() {
        return TextUtils.isEmpty(this.playTimes) ? "0" : this.playTimes;
    }

    public void setPlayTimes(String str) {
        this.playTimes = str;
    }

    public String getIsFillProgram() {
        return this.isFillProgram;
    }

    public void setIsFillProgram(String str) {
        this.isFillProgram = str;
    }

    public String getIsNPVR() {
        return this.isNPVR;
    }

    public void setIsNPVR(String str) {
        this.isNPVR = str;
    }

    public String getIsCPVR() {
        return this.isCPVR;
    }

    public void setIsCPVR(String str) {
        this.isCPVR = str;
    }

    public String getHasRecordingPVR() {
        return this.hasRecordingPVR;
    }

    public void setHasRecordingPVR(String str) {
        this.hasRecordingPVR = str;
    }

    public String getIsInstantRestart() {
        return this.isInstantRestart;
    }

    public void setIsInstantRestart(String str) {
        this.isInstantRestart = str;
    }

    public String getIsBlackout() {
        return this.isBlackout;
    }

    public void setIsBlackout(String str) {
        this.isBlackout = str;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String str) {
        this.code = str;
    }
}
