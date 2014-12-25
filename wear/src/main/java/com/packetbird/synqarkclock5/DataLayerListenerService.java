package com.packetbird.synqarkclock5;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerListenerService";
    final String BATT_PATH = "/BATT_PATH";
    final String BATT_LEFT = "left";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        for (DataEvent event : dataEvents) {
            DataItem dataItem = event.getDataItem();
            if (BATT_PATH.equals(dataItem.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                SimpleDigitalWatchFaceService.sBatteryLeftHandheld = dataMap.getInt(BATT_LEFT);
            }
        }
    }

    int count = 0;

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        checkGoogleApiClient();
        PutDataMapRequest dataMap = PutDataMapRequest.create("/message");
        dataMap.getDataMap().putInt("greetings", ++count);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
    }

    private void checkGoogleApiClient() {
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks(){
                        @Override
                        public void onConnected(Bundle bundle) {
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                        }
                    }).build();
            mGoogleApiClient.connect();
        }
    }
}