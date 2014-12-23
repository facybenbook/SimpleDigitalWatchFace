package com.packetbird.synqarkclock5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by synqark on 2014/12/15.
 */
public class SimpleWatchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         *  <action android:name="android.intent.action.BOOT_COMPLETED" />
         *  <action android:name="com.android.vending.INSTALL_REFERRER"/>
         */
            context.startService(new Intent(context, SimpleDigitalWatchDataTransferService.class));
    }
}