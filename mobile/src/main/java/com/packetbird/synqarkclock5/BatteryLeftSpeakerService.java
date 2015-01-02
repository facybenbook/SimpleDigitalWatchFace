package com.packetbird.synqarkclock5;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class BatteryLeftSpeakerService extends Service {

    private static String TAG = "BATTERY_LEFT_SPEAKER";

    private GoogleApiClient mGoogleApiClient;
    private IntentFilter filter;
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
        Log.d(TAG,"onBind");
        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d(TAG, "onConnected: " + connectionHint);
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    })
                    .addApi(Wearable.API)
                    .build();
        }
        if(!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();

        if(filter == null) {
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(myReceiver, filter);
        }
        return START_STICKY;
    }

    //受信機
    public BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                // 電池残量
                Log.d(TAG, "BATTERY_CHANGED_RECEIVED");
                mBatteryLeft = intent.getIntExtra("level", 0);
                shareBatteryLeft();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        if (myReceiver != null) {
            unregisterReceiver(myReceiver);
            myReceiver = null;
        }
    }



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
    }
}
