package com.pukka.ydepg.moudule.catchup.presenter;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import com.networkbench.agent.impl.instrumentation.NBSRunnableInspect;
import com.networkbench.agent.impl.instrumentation.NBSRunnableInstrumentation;
import com.pukka.ydepg.OTTApplication;
import com.pukka.ydepg.common.CommonUtil;
import com.pukka.ydepg.common.constant.HttpConstant;
import com.pukka.ydepg.common.http.HttpApi;
import com.pukka.ydepg.common.http.v6bean.v6node.AuthorizeResult;
import com.pukka.ydepg.common.http.v6bean.v6node.QueryChannel;
import com.pukka.ydepg.common.http.v6bean.v6node.QueryPlaybill;
import com.pukka.ydepg.common.http.v6bean.v6node.Result;
import com.pukka.ydepg.common.http.v6bean.v6node.Subject;
import com.pukka.ydepg.common.http.v6bean.v6request.PlayChannelRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryChannelStcPropsBySubjectRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryChannelSubjectListRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryPlaybillListRequest;
import com.pukka.ydepg.common.http.v6bean.v6request.QueryVODListBySubjectRequest;
import com.pukka.ydepg.common.http.v6bean.v6response.PBSRemixRecommendResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.PlayChannelResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.QueryChannelStcPropsBySubjectResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.QueryChannelSubjectListResponse;
import com.pukka.ydepg.common.http.v6bean.v6response.QueryPlaybillListResponse;
import com.pukka.ydepg.common.report.ubd.pbs.PbsUaService;
import com.pukka.ydepg.common.utils.CollectionUtil;
import com.pukka.ydepg.common.utils.ConfigUtil;
import com.pukka.ydepg.common.utils.JsonParse;
import com.pukka.ydepg.common.utils.LogUtil.SuperLog;
import com.pukka.ydepg.common.utils.RxApiManager;
import com.pukka.ydepg.common.utils.datautil.SharedPreferenceUtil;
import com.pukka.ydepg.common.utils.fileutil.StringUtils;
import com.pukka.ydepg.launcher.mvp.presenter.BasePresenter;
import com.pukka.ydepg.launcher.session.SessionService;
import com.pukka.ydepg.launcher.util.RxCallBack;
import com.pukka.ydepg.moudule.catchup.common.Constant;
import com.pukka.ydepg.moudule.catchup.common.TVODDataUtil;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;

/* JADX INFO: loaded from: classes3.dex */
public class TVODPresenter extends BasePresenter<TVODContract.View> implements TVODContract.Presenter {
    private static final String TAG = TVODPresenter.class.getName();
    private Context mContext;

    public TVODPresenter(Context context) {
        this.mContext = context;
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.TVODContract.Presenter
    public void queryChannelSubjectList() {
        String terminalConfigurationValue;
        QueryChannelSubjectListRequest queryChannelSubjectListRequest = new QueryChannelSubjectListRequest();
        if (OTTApplication.getContext().isNewFlagUser() && OTTApplication.getContext().isNewUserON()) {
            terminalConfigurationValue = SessionService.getInstance().getSession().getTerminalConfigurationValue(Constant.TVOD_SUBJECT_ID_MIGU);
        } else {
            terminalConfigurationValue = SessionService.getInstance().getSession().getTerminalConfigurationValue(Constant.TVOD_SUBJECT_ID);
        }
        queryChannelSubjectListRequest.setSubjectID(terminalConfigurationValue);
        queryChannelSubjectListRequest.setOffset("0");
        queryChannelSubjectListRequest.setCount("50");
        RxCallBack<QueryChannelSubjectListResponse> rxCallBack = new RxCallBack<QueryChannelSubjectListResponse>(HttpConstant.QUERYCHANNELSUBJECTLIST, this.mContext) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.1
            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onSuccess(final QueryChannelSubjectListResponse queryChannelSubjectListResponse) {
                if (TVODPresenter.this.mView == null) {
                    new Thread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.1.1
                        public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                        @Override // java.lang.Runnable
                        public void run() {
                            NBSRunnableInstrumentation.preRunMethod(this);
                            SuperLog.infoSDCardOptimize(TVODPresenter.TAG, "[HomePageDisplay][Get channel info][Interface calling][Name：QueryChannelSubjectList][ParaOut：]");
                            TVODPresenter.this.queryPBSRemixRecommend(queryChannelSubjectListResponse);
                            NBSRunnableInstrumentation.sufRunMethod(this);
                        }
                    }, "HeartBeatExecute").start();
                } else {
                    TVODPresenter.this.queryPBSRemixRecommend(queryChannelSubjectListResponse);
                }
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                SuperLog.error(TVODPresenter.TAG, th);
                if (TVODPresenter.this.mView != null) {
                    ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListFailed();
                }
            }
        };
        RxApiManager.get().add(HttpConstant.QUERYCHANNELSUBJECTLIST, rxCallBack);
        SuperLog.infoSDCardOptimize(TAG, "[HomePageDisplay][Get channel info][Interface calling][Name：QueryChannelSubjectList][ParaIn：subjectID=" + queryChannelSubjectListRequest.getSubjectID() + "]");
        HttpApi.getInstance().getService().queryChannelSubjectList(ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL() + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.QUERYCHANNELSUBJECTLIST, queryChannelSubjectListRequest).subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(rxCallBack);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<Subject> getSubjectListFromResponse(QueryChannelSubjectListResponse queryChannelSubjectListResponse) {
        Result result = queryChannelSubjectListResponse.getResult();
        if (TextUtils.isEmpty(result.getRetCode()) || !Result.RETCODE_OK.equals(result.getRetCode()) || queryChannelSubjectListResponse.getSubjects() == null || queryChannelSubjectListResponse.getSubjects().size() <= 0) {
            return null;
        }
        return queryChannelSubjectListResponse.getSubjects();
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.TVODContract.Presenter
    public void queryChannelStcPropsBySubject(String str, final int i) {
        QueryChannelStcPropsBySubjectRequest queryChannelStcPropsBySubjectRequest = new QueryChannelStcPropsBySubjectRequest();
        queryChannelStcPropsBySubjectRequest.setSubjectID(str);
        queryChannelStcPropsBySubjectRequest.setOffset("0");
        queryChannelStcPropsBySubjectRequest.setCount("1000");
        queryChannelStcPropsBySubjectRequest.setSortType("ORDERINDEX:DESC");
        RxCallBack<QueryChannelStcPropsBySubjectResponse> rxCallBack = new RxCallBack<QueryChannelStcPropsBySubjectResponse>(HttpConstant.QUERYCHANNELSTCPROPSBYSUBJECT, this.mContext) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.2
            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onSuccess(QueryChannelStcPropsBySubjectResponse queryChannelStcPropsBySubjectResponse) {
                if (TVODPresenter.this.mView != null) {
                    SuperLog.infoSDCardOptimize(TVODPresenter.TAG, "[LivePlay][Get TV guide info][Interface calling][Name：catchup QueryChannelStcPropsBySubject][ParaOut：]");
                    if (queryChannelStcPropsBySubjectResponse.getChannelDetails() == null || queryChannelStcPropsBySubjectResponse.getChannelDetails().size() == 0) {
                        ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelStcPropsBySubjectFailed();
                    } else {
                        ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelStcPropsBySubjectSuccess(TVODDataUtil.getInstance().filter4KChannel(queryChannelStcPropsBySubjectResponse.getChannelDetails()), i);
                    }
                }
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                if (TVODPresenter.this.mView != null) {
                    ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelStcPropsBySubjectFailed();
                }
            }
        };
        RxApiManager.get().add(HttpConstant.QUERYCHANNELSUBJECTLIST, rxCallBack);
        SuperLog.infoSDCardOptimize(TAG, "[LivePlay][Get TV guide info][Interface calling][catchup Name：QueryChannelStcPropsBySubject][ParaIn：subjectID=" + queryChannelStcPropsBySubjectRequest.getSubjectID() + "]");
        HttpApi.getInstance().getService().queryChannelStcPropsBySubject(ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL() + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.QUERYCHANNELSTCPROPSBYSUBJECT, queryChannelStcPropsBySubjectRequest).subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(rxCallBack);
    }

    public void queryPBSRemixRecommend(final QueryChannelSubjectListResponse queryChannelSubjectListResponse) {
        final ArrayList arrayList = new ArrayList();
        StringBuffer stringBuffer = new StringBuffer(HttpConstant.PBS_RemixRecommend_URL);
        stringBuffer.append("?");
        stringBuffer.append("offset=");
        stringBuffer.append("0");
        stringBuffer.append("&count=");
        stringBuffer.append("100");
        stringBuffer.append("&version=");
        stringBuffer.append(CommonUtil.getVersionName());
        stringBuffer.append("&appointedIds=");
        stringBuffer.append("rewatching25");
        stringBuffer.append("&vt=");
        stringBuffer.append("9");
        stringBuffer.append("&zjNewUserFlag=");
        stringBuffer.append(OTTApplication.getContext().isNewFlagUser() ? "1" : "0");
        String regionCode = CommonUtil.getRegionCode();
        if (!TextUtils.isEmpty(regionCode)) {
            stringBuffer.append("&regionCode=");
            stringBuffer.append(regionCode);
        }
        String accountName = SessionService.getInstance().getSession().getAccountName();
        if (!TextUtils.isEmpty(accountName)) {
            stringBuffer.append("&billID=");
            stringBuffer.append(accountName);
        }
        String userId = SessionService.getInstance().getSession().getUserId();
        if (!TextUtils.isEmpty(userId)) {
            stringBuffer.append("&userID=");
            stringBuffer.append(userId);
        }
        SharedPreferenceUtil.getInstance().getSeesionId().contains("JSESSIONID=");
        SuperLog.debug(TAG, "stb_remixRecommend Url=" + stringBuffer.toString());
        HttpApi.getInstance().getService().sendGetRequest(stringBuffer.toString(), CommonUtil.getPbsRecommendHeaders()).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io()).subscribe(new Observer<ResponseBody>() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3
            @Override // io.reactivex.Observer
            public void onComplete() {
            }

            @Override // io.reactivex.Observer
            public void onSubscribe(Disposable disposable) {
            }

            @Override // io.reactivex.Observer
            public void onNext(ResponseBody responseBody) {
                try {
                    String strString = responseBody.string();
                    SuperLog.debug(TVODPresenter.TAG, "stb_remixRecommend responseData = " + strString);
                    PBSRemixRecommendResponse pBSRemixRecommendResponse = (PBSRemixRecommendResponse) JsonParse.json2Object(strString, PBSRemixRecommendResponse.class);
                    List<Subject> subjectListFromResponse = TVODPresenter.this.getSubjectListFromResponse(queryChannelSubjectListResponse);
                    if (pBSRemixRecommendResponse != null) {
                        if (!CollectionUtil.isEmpty(pBSRemixRecommendResponse.getRecommends())) {
                            for (int i = 0; i < pBSRemixRecommendResponse.getRecommends().size(); i++) {
                                if (!CollectionUtil.isEmpty(pBSRemixRecommendResponse.getRecommends().get(i).getOther())) {
                                    for (int i2 = 0; i2 < pBSRemixRecommendResponse.getRecommends().get(i).getOther().size(); i2++) {
                                        if (pBSRemixRecommendResponse.getRecommends().get(i).getOther().get(i2).getStringURL() != null && !TextUtils.isEmpty(pBSRemixRecommendResponse.getRecommends().get(i).getOther().get(i2).getStringURL()) && pBSRemixRecommendResponse.getRecommends().get(i).getSceneType().equals("9")) {
                                            for (String str : pBSRemixRecommendResponse.getRecommends().get(i).getOther().get(i2).getStringURL().split(",")) {
                                                Subject subject = new Subject();
                                                subject.setID(str);
                                                arrayList.add(subject);
                                            }
                                        }
                                    }
                                }
                            }
                            ArrayList arrayList2 = new ArrayList();
                            if (subjectListFromResponse != null && subjectListFromResponse.size() > 0) {
                                for (int i3 = 0; i3 < subjectListFromResponse.size(); i3++) {
                                    if (arrayList != null && arrayList.size() > 0) {
                                        for (int i4 = 0; i4 < arrayList.size(); i4++) {
                                            if (subjectListFromResponse.get(i3).getID().equals(((Subject) arrayList.get(i4)).getID())) {
                                                arrayList2.add(subjectListFromResponse.get(i3));
                                            }
                                        }
                                    }
                                }
                            }
                            TVODDataUtil.getInstance().setListSubject(arrayList2);
                            TVODDataUtil.getInstance().createCacheData();
                            if (TVODPresenter.this.mView != null) {
                                ((Activity) TVODPresenter.this.mContext).runOnUiThread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3.1
                                    public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                                    @Override // java.lang.Runnable
                                    public void run() {
                                        NBSRunnableInstrumentation.preRunMethod(this);
                                        ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListSuccess();
                                        NBSRunnableInstrumentation.sufRunMethod(this);
                                    }
                                });
                                return;
                            }
                            return;
                        }
                        TVODDataUtil.getInstance().setListSubject(TVODDataUtil.getInstance().getUserRealTVODSubjectID(subjectListFromResponse));
                        TVODDataUtil.getInstance().createCacheData();
                        if (TVODPresenter.this.mView != null) {
                            ((Activity) TVODPresenter.this.mContext).runOnUiThread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3.2
                                public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                                @Override // java.lang.Runnable
                                public void run() {
                                    NBSRunnableInstrumentation.preRunMethod(this);
                                    ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListSuccess();
                                    NBSRunnableInstrumentation.sufRunMethod(this);
                                }
                            });
                            return;
                        }
                        return;
                    }
                    TVODDataUtil.getInstance().setListSubject(TVODDataUtil.getInstance().getUserRealTVODSubjectID(subjectListFromResponse));
                    TVODDataUtil.getInstance().createCacheData();
                    if (TVODPresenter.this.mView != null) {
                        ((Activity) TVODPresenter.this.mContext).runOnUiThread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3.3
                            public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                            @Override // java.lang.Runnable
                            public void run() {
                                NBSRunnableInstrumentation.preRunMethod(this);
                                ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListSuccess();
                                NBSRunnableInstrumentation.sufRunMethod(this);
                            }
                        });
                    }
                } catch (IOException e) {
                    SuperLog.error(TVODPresenter.TAG, "stb_remixRecommend fail,queryPBSRemixRecommend");
                    SuperLog.error(TVODPresenter.TAG, e.getMessage());
                    TVODDataUtil.getInstance().setListSubject(TVODDataUtil.getInstance().getUserRealTVODSubjectID(TVODPresenter.this.getSubjectListFromResponse(queryChannelSubjectListResponse)));
                    TVODDataUtil.getInstance().createCacheData();
                    if (TVODPresenter.this.mView != null) {
                        ((Activity) TVODPresenter.this.mContext).runOnUiThread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3.4
                            public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                            @Override // java.lang.Runnable
                            public void run() {
                                NBSRunnableInstrumentation.preRunMethod(this);
                                ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListSuccess();
                                NBSRunnableInstrumentation.sufRunMethod(this);
                            }
                        });
                    }
                }
            }

            @Override // io.reactivex.Observer
            public void onError(Throwable th) {
                SuperLog.error(TVODPresenter.TAG, "stb_remixRecommend fail,queryPBSRemixRecommend");
                SuperLog.error(TVODPresenter.TAG, th.getMessage());
                TVODDataUtil.getInstance().setListSubject(TVODDataUtil.getInstance().getUserRealTVODSubjectID(TVODPresenter.this.getSubjectListFromResponse(queryChannelSubjectListResponse)));
                TVODDataUtil.getInstance().createCacheData();
                if (TVODPresenter.this.mView != null) {
                    ((Activity) TVODPresenter.this.mContext).runOnUiThread(new Runnable() { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.3.5
                        public transient NBSRunnableInspect nbsHandler = new NBSRunnableInspect();

                        @Override // java.lang.Runnable
                        public void run() {
                            NBSRunnableInstrumentation.preRunMethod(this);
                            ((TVODContract.View) TVODPresenter.this.mView).onQueryChannelSubjectListSuccess();
                            NBSRunnableInstrumentation.sufRunMethod(this);
                        }
                    });
                }
            }
        });
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.TVODContract.Presenter
    public void playChannel(final Context context, final PlayChannelRequest playChannelRequest) {
        String edsURL;
        PbsUaService.setStartStayTime();
        if (OTTApplication.getContext().isUseHttps() && !OTTApplication.getContext().isPlayChannelFailed()) {
            edsURL = ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURLHttp();
        } else {
            edsURL = ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL();
        }
        HttpApi.getInstance().getService().playChannel(edsURL + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.PLAYCHANNEL, playChannelRequest).compose(onCompose(((TVODContract.View) this.mView).bindToLife())).subscribe(new RxCallBack<PlayChannelResponse>(HttpConstant.PLAYCHANNEL, context) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.4
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
                if (!retCode.equals(Result.RETCODE_OK)) {
                    SuperLog.error(TVODPresenter.TAG, "[playChannel] error code = " + retCode + " message = " + result.getRetMsg());
                    if (authorizeResult != null) {
                        ((TVODContract.View) TVODPresenter.this.mView).onPlayChannelUrlFailed(playChannelResponse);
                        return;
                    } else {
                        ((TVODContract.View) TVODPresenter.this.mView).onPlayChannelUrlError();
                        TVODPresenter.this.handleErrorIncludeTimeOut(retCode, HttpConstant.PLAYCHANNEL, context);
                        return;
                    }
                }
                String productID = authorizeResult != null ? authorizeResult.getProductID() : null;
                String playURL = playChannelResponse.getPlayURL();
                if (TextUtils.isEmpty(productID) && TextUtils.isEmpty(playURL)) {
                    return;
                }
                SuperLog.debug(TVODPresenter.TAG, "[playChannel] get playUrl success.");
                TVODContract.View view = (TVODContract.View) TVODPresenter.this.mView;
                String channelID = playChannelRequest.getChannelID();
                String strSplicingPlayUrl = StringUtils.splicingPlayUrl(playURL);
                String bookmark = playChannelResponse.getBookmark();
                String mediaID = playChannelRequest.getMediaID();
                if (TextUtils.isEmpty(productID)) {
                    productID = "";
                }
                view.onPlayChannelUrlSuccess(channelID, strSplicingPlayUrl, bookmark, mediaID, productID, playChannelRequest.getPlaybillID());
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                if (!OTTApplication.getContext().isUseHttps() || OTTApplication.getContext().isPlayChannelFailed()) {
                    if (TVODPresenter.this.mView != null) {
                        ((TVODContract.View) TVODPresenter.this.mView).onPlayChannelUrlError();
                    }
                    PbsUaService.reportPlayChannel(playChannelRequest, null, th.getMessage());
                } else {
                    SuperLog.infoSDCardOptimize(TVODPresenter.TAG, "https request playChannel fail retry request!");
                    OTTApplication.getContext().setPlayChannelFailed(true);
                    TVODPresenter.this.playChannel(context, playChannelRequest);
                }
            }
        });
    }

    @Override // com.pukka.ydepg.moudule.catchup.presenter.TVODContract.Presenter
    public void queryPlayBillList(Context context, String str, String str2, String str3) {
        QueryPlaybillListRequest queryPlaybillListRequest = new QueryPlaybillListRequest();
        QueryChannel queryChannel = new QueryChannel();
        ArrayList arrayList = new ArrayList();
        arrayList.add(str);
        queryChannel.setChannelIDs(arrayList);
        queryChannel.setContentType("CHANNEL");
        queryChannel.setIsReturnAllMedia("1");
        QueryPlaybill queryPlaybill = new QueryPlaybill();
        queryPlaybill.setType("2");
        queryPlaybill.setStartTime(String.valueOf(str2));
        queryPlaybill.setCount("100");
        queryPlaybill.setOffset("0");
        queryPlaybill.setIsFillProgram("1");
        queryPlaybill.setEndTime(str3);
        queryPlaybill.setSortType(QueryVODListBySubjectRequest.SortType.START_TIME_ASC);
        queryPlaybillListRequest.setNeedChannel("0");
        queryPlaybillListRequest.setQueryChannel(queryChannel);
        queryPlaybillListRequest.setQueryPlaybill(queryPlaybill);
        HttpApi.getInstance().getService().queryPlaybillList(ConfigUtil.getConfig(OTTApplication.getContext()).getEdsURL() + HttpConstant.HTTPS_VSP_IP_VSP_PORT_VSP_V3 + HttpConstant.QUERYPLAYBILLLISTSTCPROPS, queryPlaybillListRequest).compose(onCompose(((TVODContract.View) this.mView).bindToLife())).subscribe(new RxCallBack<QueryPlaybillListResponse>(context) { // from class: com.pukka.ydepg.moudule.catchup.presenter.TVODPresenter.5
            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onSuccess(QueryPlaybillListResponse queryPlaybillListResponse) {
                if (queryPlaybillListResponse.getResult() == null || !queryPlaybillListResponse.getResult().getRetCode().equals(Result.RETCODE_OK)) {
                    ((TVODContract.View) TVODPresenter.this.mView).onQueryPlayBillListFailed();
                } else {
                    ((TVODContract.View) TVODPresenter.this.mView).onQueryPlayBillListSuccess(queryPlaybillListResponse.getChannelPlaybills().get(0).getPlaybillLites().get(0));
                }
            }

            @Override // com.pukka.ydepg.launcher.util.RxCallBack
            public void onFail(Throwable th) {
                ((TVODContract.View) TVODPresenter.this.mView).onQueryPlayBillListFailed();
            }
        });
    }
}
