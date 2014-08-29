package org.fedorahosted.freeotp;

import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.*;
import java.lang.StringBuilder;
import java.util.concurrent.TimeUnit;

public class WearListenerService extends WearableListenerService {
    private static final String TAG = "FreeOTPWear";

    /**
     * Here we process messages received from the watch. In the future, we might
     * consider doing this more cleanly by using e.g. DataMap and friends, but
     * for an initial attempt at getting this working, this seemed to be a
     * reasonable starting place.
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        GoogleApiClient googleApiClient =
            new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .build();

        ConnectionResult connectionResult =
            googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        TokenAdapter ta = new TokenAdapter(this);
        String path = messageEvent.getPath();
        if (path.equals("/freeotp/start")) {
            StringBuilder msg = new StringBuilder();
            // Someone opened the freeotp smartwatch app. Send them the list
            // of tokens.
            for (int i = 0; i < ta.getCount(); i++) {
                Token t = ta.getItem(i);
                msg.append(t.getLabel());
                if (t.getIssuer() != null && t.getIssuer() != "") {
                    msg.append("(" + t.getIssuer() + ")");
                }
                msg.append("\n");
            }
            Wearable.MessageApi.sendMessage(
                googleApiClient,
                messageEvent.getSourceNodeId(),
                "/freeotp/list",
                msg.toString().getBytes());
        } else if (path.equals("/freeotp/token/request")) {
            String idx = new String(messageEvent.getData());

            TokenPersistence tp = new TokenPersistence(this);
            Token t = ta.getItem(Integer.parseInt(idx));
            TokenCode codes = t.generateCodes();
            tp.save(t);

            String code = codes.getCurrentCode();

            Wearable.MessageApi.sendMessage(
                googleApiClient,
                messageEvent.getSourceNodeId(),
                "/freeotp/token/code",
                code.getBytes());
        }
    }
}
