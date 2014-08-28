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

        String path = messageEvent.getPath();
        if (path.equals("/freeotp/start")) {
            StringBuilder msg = new StringBuilder();
            // Someone opened the freeotp smartwatch app. Send them the list
            // of token names.
            TokenAdapter ta = new TokenAdapter(this);
            for (int i = 0; i < ta.getCount(); i++) {
                Token t = ta.getItem(i);
                msg.append(t.getID() + " " + t.getLabel() + "(" + t.getIssuer() + ")\n");
            }
            Wearable.MessageApi.sendMessage(
                googleApiClient,
                messageEvent.getSourceNodeId(),
                "/freeotp/list",
                msg.toString().getBytes());
        }
    }
}
