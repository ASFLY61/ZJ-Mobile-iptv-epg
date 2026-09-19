package com.pukka.ydepg.moudule.catchup.presenter;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.pukka.ydepg.OTTApplication;
import com.pukka.ydepg.R;
import com.pukka.ydepg.common.CommonUtil;
import com.pukka.ydepg.common.constant.HttpConstant;
import com.pukka.ydepg.common.errorcode.ErrorCode;
import com.pukka.ydepg.common.http.HttpApi;
import com.pukka.ydepg.common.http.v6bean.v6node.AuthorizeResult;
import com.pukka.ydepg.common.http.v6bean.v6node.ChannelPlaybill;
import com.pukka.ydepg.common.http.v6bean.v6node.PlaybillLite;
import com.pukka.ydepg.common.http.v6bean.v6node.QueryChannel;
import com.pukka.ydepg.common.http.v6bean.v6node.QueryPlaybill;
import com.pukka.ydepg.common.http.v6bean.v6node.Result;
import com.pukka.ydepg.common.http.v6bean.v6request.PlayChannelRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryPlaybillListRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryVODListBySubjectRequest;
import com.pukka.ydepg.common.http.v6bean.v6response.NewUserChannelResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.PlayChannelResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.QueryPlaybillListResponse;
import com.pukka.ydepg.common.report.ubd.pbs.PbsUaService;
import com.pukka.ydepg.common.utils.CollectionUtil;
import com.pukka.ydepg.common.utils.ConfigUtil;
import com.pukka.ydepg.common.utils.DateUtil;
import com.pukka.ydepg.common.utils.LogUtil.SuperLog;
import com.pukka.ydepg.common.utils.fileutil.StringUtils;
import com.pukka.ydepg.common.utils.timeutil.DateCalendarUtils;
import com.pukka.ydepg.common.utils.uiutil.EpgToast;
import com.pukka.ydepg.common.utils.uiutil.Strings;
import com.pukka.ydepg.launcher.mvp.presenter.BasePresenter;
import com.pukka.ydepg.launcher.util.RxCallBack;
import com.pukka.ydepg.moudule.catchup.common.TVODDataUtil;
import com.pukka.ydepg.moudule.catchup.presenter.contract.TVODProgramListContract;
import com.pukka.ydepg.moudule.catchup.presenter.contract.TVODProgramListContract.View;
import com.pukka.ydepg.moudule.player.node.Program;
import com.pukka.ydepg.service.NtpTimeService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class TVODProgramListPresenter<T extends TVODProgramListContract.View> extends BasePresenter<T> implements TVODProgramListContract.Presenter {
    public static int DAY_COUNT = 6;
    private static final String TAG = "TVODProgramListPresenter";
    private RxCallBack<PlayChannelResponse> mPlayChannelCallback;
    private RxCallBack<QueryPlaybillListResponse> mPlaybillListCallback;

    @Override // com.pukka.ydepg.moudule.catchup.presenter.contract.TVODProgramListContract.Presenter
    public void createTVODDateList() {
        int iMin;
        List<String> arrayList = new ArrayList<>();
        List<String> arrayList2 = new ArrayList<>();
        List<String> arrayList3 = new ArrayList<>();
        arrayList.add(Strings.getInstance().getString(R.string.epglist_date_today));
        arrayList3.add(Strings.getInstance().getString(R.string.epglist_date_today));
        arrayList.add(Strings.getInstance().getString(R.string.epglist_date_yesterday));
        arrayList3.add(Strings.getInstance().getString(R.string.epglist_date_yesterday));
        int i = 0;
        for (int i2 = 0; i2 >= (-DAY_COUNT); i2--) {
            if (i2 <= -2) {
                String mothAndDayValue = DateUtil.getMothAndDayValue(i2);
                if (mothAndDayValue != null) {
                    arrayList.add(mothAndDayValue);
                }
                String chineseMothAndDayValue = DateUtil.getChineseMothAndDayValue(i2);
                if (chineseMothAndDayValue != null) {
                    arrayList3.add(chineseMothAndDayValue);
                }
            }
            if (DateUtil.getBeoreAfterDateValue(i2) != null) {
                arrayList2.add(DateUtil.getBeoreAfterDateValue(i2));
            }
        }
        if (OTTApplication.getContext().isNewFlagUser() && !OTTApplication.getContext().isNewUserON()) {
            NewUserChannelResponse newUserChannelResponse = OTTApplication.getContext().getmNewUserChannelResponse();
            if (newUserChannelResponse != null && !TextUtils.isEmpty(newUserChannelResponse.getLimitDays())) {
                try {
                    String limitDays = newUserChannelResponse.getLimitDays();
                    if (!TextUtils.isEmpty(limitDays)) {
                        i = Integer.parseInt(limitDays);
                    }
                } catch (Exception e) {
                    SuperLog.error(TAG, "Failed to get hide_date_count config: " + e.getMessage());
                }
            }
            if (i > 0 && (iMin = Math.min(i, arrayList.size())) > 0) {
                arrayList = arrayList.subList(iMin, arrayList.size());
                arrayList3 = arrayList3.subList(iMin, arrayList3.size());
                if (arrayList2.size() >= iMin) {
                    arrayList2 = arrayList2.subList(iMin, arrayList2.size());
                }
            }
        }
        if (this.mView != 0) {
            ((TVODProgramListContract.View) this.mView).onTVODDateList(arrayList2, arrayList, arrayList3);
        }
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.contract.TVODProgramListContract.Presenter
    public void playChannel(final Context context, final PlayChannelRequest playChannelRequest) {
        String edsURL;
        RxCallBack<PlayChannelResponse> rxCallBack = this.mPlayChannelCallback;
        if (rxCallBack != null) {
            rxCallBack.dispose();
            this.mPlayChannelCallback = null;
        }
        this.mPlayChannelCallback = new RxCallBack<PlayChannelResponse>(HttpConstant.PLAYCHANNEL, context) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODProgramListPresenter.1
            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onSuccess(PlayChannelResponse playChannelResponse) {
                PbsUaService.reportPlayChannel(playChannelRequest, playChannelResponse, null);
                if (playChannelResponse == null || playChannelResponse.getResult() == null) {
                    return;
                }
                Result result = playChannelResponse.getResult();
                String retCode = result.getRetCode();
                if (TextUtils.isEmpty(retCode)) {
                    return;
                }
                AuthorizeResult authorizeResult = playChannelResponse.getAuthorizeResult();
                if (retCode.equals(Result.RETCODE_OK)) {
                    String productID = authorizeResult != null ? authorizeResult.getProductID() : null;
                    String playURL = playChannelResponse.getPlayURL();
                    if (TextUtils.isEmpty(productID) && TextUtils.isEmpty(playURL)) {
                        return;
                    }
                    SuperLog.debug(TVODProgramListPresenter.TAG, "[playChannel] get playUrl success.");
                    TVODProgramListContract.View view = (TVODProgramListContract.View) TVODProgramListPresenter.this.mView;
                    String channelID = playChannelRequest.getChannelID();
                    String strSplicingPlayUrl = StringUtils.splicingPlayUrl(playURL);
                    String bookmark = playChannelResponse.getBookmark();
                    String mediaID = playChannelRequest.getMediaID();
                    if (TextUtils.isEmpty(productID)) {
                        productID = "";
                    }
                    view.onPlayChannelUrlSuccess(channelID, strSplicingPlayUrl, bookmark, mediaID, productID, playChannelRequest.getPlaybillID());
                    return;
                }
                SuperLog.error(TVODProgramListPresenter.TAG, "[playChannel] error code = " + retCode + " message = " + result.getRetMsg());
                if (authorizeResult != null) {
                    ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onPlayChannelUrlFailed(playChannelResponse);
                } else {
                    ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onPlayChannelUrlError();
                    TVODProgramListPresenter.this.handleErrorIncludeTimeOut(retCode, HttpConstant.PLAYCHANNEL, context);
                }
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                if (!OTTApplication.getContext().isUseHttps() || OTTApplication.getContext().isPlayChannelFailed()) {
                    if (TVODProgramListPresenter.this.mView != null) {
                        ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onPlayChannelUrlError();
                    }
                    PbsUaService.reportPlayChannel(playChannelRequest, null, th.getMessage());
                } else {
                    SuperLog.infoSDCardOptimize(TVODProgramListPresenter.TAG, "https request playChannel fail retry request!");
                    OTTApplication.getContext().setPlayChannelFailed(true);
                    TVODProgramListPresenter.this.playChannel(context, playChannelRequest);
                }
            }
        };
        PbsUaService.setStartStayTime();
        if (OTTApplication.getContext().isUseHttps() && !OTTApplication.getContext().isPlayChannelFailed()) {
            edsURL = ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURLHttp();
        } else {
            edsURL = ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL();
        }
        HttpApi.getInstance().getService().playChannel(edsURL + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.PLAYCHANNEL, playChannelRequest).compose(onCompose(((TVODProgramListContract.View) this.mView).bindToLife())).subscribe(this.mPlayChannelCallback);
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.contract.TVODProgramListContract.Presenter
    public void queryPlaybillList(Context context, final String str, final List list) {
        String strValueOf;
        String strValueOf2;
        long time;
        RxCallBack<QueryPlaybillListResponse> rxCallBack = this.mPlaybillListCallback;
        if (rxCallBack != null) {
            rxCallBack.dispose();
            this.mPlaybillListCallback = null;
        }
        final String str2 = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(NtpTimeService.queryNtpTime()));
        List<Program> tVODProgramList = TVODDataUtil.getInstance().getTVODProgramList(str + list.get(0).toString());
        if (!CollectionUtil.isEmpty(tVODProgramList)) {
            ((TVODProgramListContract.View) this.mView).onQueryPlayBillListSuccess(str, tVODProgramList.size(), resizeProgramList(tVODProgramList));
            return;
        }
        this.mPlaybillListCallback = new RxCallBack<QueryPlaybillListResponse>(context) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODProgramListPresenter.2
            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onSuccess(QueryPlaybillListResponse queryPlaybillListResponse) {
                String retCode = queryPlaybillListResponse.getResult().getRetCode();
                if (retCode.equals(Result.RETCODE_OK)) {
                    List<Program> program = TVODProgramListPresenter.this.parseProgram(str2.equals(str), str.equals(DateUtil.getBeoreAfterDateValue(-1)), queryPlaybillListResponse);
                    if (CollectionUtil.isEmpty(program)) {
                        ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onPlayBillListEmpty(str);
                        return;
                    }
                    TVODDataUtil.getInstance().setTVODProgramList(str + list.get(0).toString(), program);
                    ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onQueryPlayBillListSuccess(str, program.size(), TVODProgramListPresenter.this.resizeProgramList(program));
                    return;
                }
                EpgToast.showToast(OTTApplication.getContext(), ErrorCode.findError(HttpConstant.QUERYPLAYBILLLISTSTCPROPS, retCode).getMessage());
                ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onQueryPlaybillListFailed();
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                SuperLog.error(TVODProgramListPresenter.TAG, th);
                if (TVODProgramListPresenter.this.mView != null) {
                    ((TVODProgramListContract.View) TVODProgramListPresenter.this.mView).onQueryPlaybillListFailed();
                }
            }
        };
        if (str2.equals(str)) {
            time = DateCalendarUtils.getTime(str);
            strValueOf2 = String.valueOf(NtpTimeService.queryNtpTime());
        } else {
            long time2 = DateCalendarUtils.getTime(str);
            String date = DateCalendarUtils.formatDate(time2, "yyyyMMdd");
            if (OTTApplication.getContext().isNewFlagUser() && !OTTApplication.getContext().isNewUserON() && str.equals(DateUtil.getBeoreAfterDateValue(-1))) {
                strValueOf = String.valueOf(NtpTimeService.queryNtpTime() - 86400000);
            } else {
                strValueOf = String.valueOf(DateCalendarUtils.getEndTimeOfDay(date));
            }
            strValueOf2 = strValueOf;
            time = time2;
        }
        SuperLog.debug("QueryPlaybillListTAG", "[startTime:" + time + ",endTime:" + strValueOf2 + "]");
        QueryPlaybillListRequest queryPlaybillListRequest = new QueryPlaybillListRequest();
        QueryChannel queryChannel = new QueryChannel();
        queryChannel.setChannelIDs(list);
        queryChannel.setContentType("CHANNEL");
        queryChannel.setIsReturnAllMedia("1");
        QueryPlaybill queryPlaybill = new QueryPlaybill();
        queryPlaybill.setType("2");
        queryPlaybill.setStartTime(String.valueOf(time));
        queryPlaybill.setCount("100");
        queryPlaybill.setOffset("0");
        queryPlaybill.setIsFillProgram("1");
        queryPlaybill.setEndTime(strValueOf2);
        queryPlaybill.setSortType(QueryVODListBySubjectRequest.SortType.START_TIME_ASC);
        queryPlaybillListRequest.setNeedChannel("0");
        queryPlaybillListRequest.setQueryChannel(queryChannel);
        queryPlaybillListRequest.setQueryPlaybill(queryPlaybill);
        HttpApi.getInstance().getService().queryPlaybillList(ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL() + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.QUERYPLAYBILLLISTSTCPROPS, queryPlaybillListRequest).compose(onCompose(((TVODProgramListContract.View) this.mView).bindToLife())).subscribe(this.mPlaybillListCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<Program> parseProgram(boolean z, boolean z2, QueryPlaybillListResponse queryPlaybillListResponse) {
        List<ChannelPlaybill> channelPlaybills = queryPlaybillListResponse.getChannelPlaybills();
        if (CollectionUtil.isEmpty(channelPlaybills)) {
            return null;
        }
        List<PlaybillLite> playbillLites = channelPlaybills.get(0).getPlaybillLites();
        ArrayList arrayList = new ArrayList();
        if (z2 && OTTApplication.getContext().isNewFlagUser() && !OTTApplication.getContext().isNewUserON() && !CollectionUtil.isEmpty(playbillLites) && Long.parseLong(playbillLites.get(playbillLites.size() - 1).getEndTime()) > NtpTimeService.queryNtpTime() - 86400000) {
            playbillLites.remove(playbillLites.get(playbillLites.size() - 1));
        }
        int i = 0;
        for (PlaybillLite playbillLite : playbillLites) {
            if (z && i == playbillLites.size() - 1) {
                break;
            }
            i++;
            if (!playbillLite.getIsFillProgram().equals("1")) {
                Program program = new Program();
                program.setChannelID(playbillLite.getChannelID());
                program.setName(TextUtils.isEmpty(playbillLite.getName()) ? Strings.getInstance().getString(R.string.channel_playbill_name_empty) : playbillLite.getName());
                program.setId(playbillLite.getID());
                program.setStartTime(playbillLite.getStartTime());
                program.setEndTime(playbillLite.getEndTime());
                if (!CollectionUtil.isEmpty(playbillLite.getCustomFields())) {
                    List<String> customNamedParameterByKey = CommonUtil.getCustomNamedParameterByKey(playbillLite.getCustomFields(), "IsLookBack");
                    if (!CollectionUtil.isEmpty(customNamedParameterByKey)) {
                        program.setIsLookBack(customNamedParameterByKey.get(0));
                    } else {
                        program.setIsLookBack("");
                    }
                    List<String> customNamedParameterByKey2 = CommonUtil.getCustomNamedParameterByKey(playbillLite.getCustomFields(), "IsShield");
                    if (!CollectionUtil.isEmpty(customNamedParameterByKey2)) {
                        program.setIsShield(customNamedParameterByKey2.get(0));
                    } else {
                        program.setIsShield("");
                    }
                } else {
                    program.setIsLookBack("");
                    program.setIsShield("");
                }
                program.setIsBlackout(playbillLite.getIsBlackout());
                arrayList.add(program);
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<ArrayMap<Integer, List<Program>>> resizeProgramList(List<Program> list) {
        ArrayList arrayList = new ArrayList();
        int size = list.size();
        int i = ((size + 26) - 1) / 26;
        ArrayList arrayList2 = new ArrayList();
        for (int i2 = 1; i2 <= i; i2++) {
            if (i == 1) {
                try {
                    arrayList2.add(0, list.subList(0, size));
                } catch (Exception e) {
                    SuperLog.error(TAG, e);
                }
            } else if (i2 == i) {
                int i3 = i2 - 1;
                arrayList2.add(i3, list.subList(i3 * 26, size));
            } else {
                int i4 = i2 - 1;
                arrayList2.add(i4, list.subList(i4 * 26, i2 * 26));
            }
        }
        for (int i5 = 0; i5 < arrayList2.size(); i5++) {
            List list2 = (List) arrayList2.get(i5);
            ArrayList arrayList3 = new ArrayList();
            ArrayList arrayList4 = new ArrayList();
            for (int i6 = 0; i6 < list2.size(); i6++) {
                Program program = (Program) list2.get(i6);
                if (i6 < 13) {
                    arrayList3.add(program);
                } else if (i6 < 26) {
                    arrayList4.add(program);
                }
            }
            ArrayMap arrayMap = new ArrayMap();
            arrayMap.put(0, arrayList3);
            arrayMap.put(1, arrayList4);
            arrayList.add(i5, arrayMap);
        }
        return arrayList;
    }
}
