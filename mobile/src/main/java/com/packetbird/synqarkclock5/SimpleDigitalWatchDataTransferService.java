package com.packetbird.synqarkclock5;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class SimpleDigitalWatchDataTransferService extends WearableListenerService {

    final static String TAG = "SimpleDigitalWatchDataTransferService";
    private BroadcastReceiver mReceiver = null;
    private GoogleApiClient mGoogleApiClient;
    private int mBatteryLeft = 0;
    final String BATT_PATH = "/BATT_PATH";
    final String BATT_LEFT = "left";

    private void shareBatteryLeft(){
        PutDataMapRequest dataMap = PutDataMapRequest.create(BATT_PATH);
        dataMap.getDataMap().putInt(BATT_LEFT, mBatteryLeft);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient,request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Battery Left Updated:" + mBatteryLeft);
            }
        });
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        checkReceiverAndGoogleApiCLient();
    }

    public SimpleDigitalWatchDataTransferService() {
        super();
        checkReceiverAndGoogleApiCLient();
    }

    private void checkReceiverAndGoogleApiCLient(){

        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks(){
                        @Override
                        public void onConnected(Bundle bundle) {
                            /**
                             * 接続時：送る
                             */
                            Log.d(TAG,"google api client connected");
                            shareBatteryLeft();
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            /**
                             * 切断時：何もしない
                             */
                        }
                    }).build();
            mGoogleApiClient.connect();
        }
        if(mReceiver == null)
        {
            mReceiver = new BroadcastReceiver(){
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)){
                        mBatteryLeft = intent.getIntExtra("level",0);
                        shareBatteryLeft();
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        if(mReceiver != null) unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        checkReceiverAndGoogleApiCLient();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if(mReceiver != null) unregisterReceiver(mReceiver);
        mReceiver = null;
        super.onDestroy();
    }
}
