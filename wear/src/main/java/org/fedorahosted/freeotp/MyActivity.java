package org.fedorahosted.freeotp;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wearable.MessageApi.MessageListener;
import com.google.android.gms.wearable.*;

import java.util.ArrayList;

public class MyActivity extends Activity implements
                                             WearableListView.ClickListener,
                                             MessageListener,
                                             GoogleApiClient.ConnectionCallbacks {

    private WearableListView mListView;
    private MyListAdapter mAdapter;

    private float mDefaultCircleRadius;
    private float mSelectedCircleRadius;

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private Node peerNode;
    private ArrayList<String> lines = new ArrayList<String>();

    @Override
    public void onConnectionSuspended(int i) {
    }


    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final String path = messageEvent.getPath();
                    if (path.equals("/freeotp/list")) {
                        final String msg = new String(messageEvent.getData());
                        for (String s : msg.split("\n")) {
                            lines.add(s);
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d("MyActivity", "onConnectionFailed: " + result);
                    }
                })
            .addApi(Wearable.API)
            .build();
        mGoogleApiClient.connect();

        mDefaultCircleRadius = getResources().getDimension(R.dimen.default_settings_circle_radius);
        mSelectedCircleRadius = getResources().getDimension(R.dimen.selected_settings_circle_radius);
        mAdapter = new MyListAdapter();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mListView = (WearableListView) stub.findViewById(R.id.sample_list_view);
                mListView.setAdapter(mAdapter);
                mListView.setClickListener(MyActivity.this);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("MyActivity", "connected to Google Play Services on Wear!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
    }

    private ResultCallback<Status> resultCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            Log.v("MyActivity", "Status: " + status.getStatus().isSuccess());
            new AsyncTask<Void, Void, Void>(){
                @Override
                    protected Void doInBackground(Void... params) {
                    sendStartMessage();
                    return null;
                }
            }.execute();
        }
    };

    private void sendStartMessage() {
        NodeApi.GetConnectedNodesResult rawNodes =
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (final Node node : rawNodes.getNodes()) {
            Log.v("MyActivity", "Node: " + node.getId());
            PendingResult<MessageApi.SendMessageResult> result =
                Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    "/freeotp/start",
                    null);

            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.v("MyActivity", "Callback complete.");
                        peerNode = node;
                    }
                });
        }
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        Toast.makeText(this, String.format("You selected item #%s", viewHolder.getPosition()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTopEmptyRegionClick() {
        Toast.makeText(this, "You tapped into the empty area above the list", Toast.LENGTH_SHORT).show();
    }

    public class MyListAdapter extends WearableListView.Adapter {

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new WearableListView.ViewHolder(new MyItemView(MyActivity.this));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i) {
            MyItemView myItemView = (MyItemView) viewHolder.itemView;

            TextView textView = (TextView) myItemView.findViewById(R.id.text);
            String line = lines.get(i);
            textView.setText(line.substring(line.indexOf(" ")+1));
            //CircledImageView imageView = (CircledImageView) myItemView.findViewById(R.id.image);
            //imageView.setImageResource(resourceId);
        }

        @Override
        public int getItemCount() {
            return lines.size();
        }
    }

    private final class MyItemView extends FrameLayout implements WearableListView.Item {

        final CircledImageView image;
        final TextView text;
        private float mScale;

        public MyItemView(Context context) {
            super(context);
            View.inflate(context, R.layout.wearablelistview_item, this);
            image = (CircledImageView) findViewById(R.id.image);
            text = (TextView) findViewById(R.id.text);
        }

        @Override
        public float getProximityMinValue() {
            return mDefaultCircleRadius;
        }

        @Override
        public float getProximityMaxValue() {
            return mSelectedCircleRadius;
        }

        @Override
        public float getCurrentProximityValue() {
            return mScale;
        }

        @Override
        public void setScalingAnimatorValue(float value) {
            mScale = value;
            image.setCircleRadius(mScale);
            image.setCircleRadiusPressed(mScale);
        }

        @Override
        public void onScaleUpStart() {
            image.setAlpha(1f);
            text.setAlpha(1f);
        }

        @Override
        public void onScaleDownStart() {
            image.setAlpha(0.5f);
            text.setAlpha(0.5f);
        }
    }
}
