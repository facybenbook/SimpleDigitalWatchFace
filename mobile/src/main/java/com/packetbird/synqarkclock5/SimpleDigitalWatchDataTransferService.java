package com.packetbird.synqarkclock5;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class SimpleDigitalWatchDataTransferService extends WearableListenerService {

    private static String TAG = "BATTERY_LEFT_SPEAKER";

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);
        Log.v(TAG,"onPeerDisconnected!!!!");
        stopService(new Intent(getBaseContext(), BatteryLeftSpeakerService.class));
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.v(TAG,"onPeerConnected!!!!");
        startService(new Intent(getBaseContext(),BatteryLeftSpeakerService.class));
    }
}