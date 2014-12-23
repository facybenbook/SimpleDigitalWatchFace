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
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by synqark on 2014/12/15.
 */
public class SimpleDigitalWatchDataTransferService extends Service {

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
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
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
        if(mReceiver != null) unregisterReceiver(mReceiver);

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

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
