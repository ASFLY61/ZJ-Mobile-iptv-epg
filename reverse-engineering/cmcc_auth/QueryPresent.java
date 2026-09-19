package com.chinamobile.middleware.auth.http;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.chinamobile.middleware.auth.Encode.AESUtils;
import com.chinamobile.middleware.auth.tools.PropManager;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/* JADX INFO: loaded from: classes.dex */
public class QueryPresent {
    private static final String TAG = "QueryPresent";
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;

    public interface QueryStbAccountCallback {
        void queryStbAccountFail(String str, String str2);

        void queryStbAccountSucc(String str, String str2);
    }

    public void query(final QueryStbAccountCallback queryStbAccountCallback) {
        this.client.newCall(new Request.Builder().url("http://zzjdhgl.zj.chinamobile.com:37020/public/common/stb/query/stbaccount").post(RequestBody.create(MediaType.parse("application/json"), "{\"STBID\":\"" + PropManager.getSystemProperties("ro.serialno") + "\"}")).build()).enqueue(new Callback() { // from class: com.chinamobile.middleware.auth.http.QueryPresent.1
            @Override // okhttp3.Callback
            public void onFailure(Call call, IOException iOException) {
                Log.i(QueryPresent.TAG, "onFailure: ");
                iOException.printStackTrace();
                queryStbAccountCallback.queryStbAccountFail("", "");
            }

            @Override // okhttp3.Callback
            public void onResponse(Call call, Response response) throws IOException {
                QueryStbAccountResponse queryStbAccountResponse;
                if (queryStbAccountCallback == null) {
                    return;
                }
                String strString = response.body().string();
                if (!TextUtils.isEmpty(strString) && (queryStbAccountResponse = (QueryStbAccountResponse) JsonParse.json2Object(strString, QueryStbAccountResponse.class)) != null) {
                    String stbAccount = queryStbAccountResponse.getStbAccount();
                    String stbPassword = queryStbAccountResponse.getStbPassword();
                    if (!TextUtils.isEmpty(stbAccount) && !TextUtils.isEmpty(stbPassword)) {
                        try {
                            String strDecryption = AESUtils.decryption(stbAccount, "YOUR_STBACCOUNT_AES_KEY");
                            String strDecryption2 = AESUtils.decryption(stbPassword, "YOUR_STBACCOUNT_AES_KEY");
                            Log.i(QueryPresent.TAG, "onResponse: account " + strDecryption);
                            Log.i(QueryPresent.TAG, "onResponse: password " + strDecryption2);
                            if (!TextUtils.isEmpty(strDecryption) && !TextUtils.isEmpty(strDecryption2)) {
                                queryStbAccountCallback.queryStbAccountSucc(strDecryption, strDecryption2);
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!TextUtils.isEmpty(queryStbAccountResponse.getErrorParams()) || !TextUtils.isEmpty(queryStbAccountResponse.getResult())) {
                        queryStbAccountCallback.queryStbAccountFail(queryStbAccountResponse.getResult(), queryStbAccountResponse.getErrorParams());
                        return;
                    }
                }
                queryStbAccountCallback.queryStbAccountFail("", "");
            }
        });
    }
}
