package com.packetbird.synqarkclock5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * Created by synqark on 2014/12/16.
 */
public class TestActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();
        Context context = this.getApplicationContext();
        context.startService(new Intent(context, SimpleDigitalWatchDataTransferService.class));
        this.finish();
    }
}
