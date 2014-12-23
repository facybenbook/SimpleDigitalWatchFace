package com.packetbird.synqarkclock5;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by synqark on 2014/12/16.
 */

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerListenerService";
    final String BATT_PATH = "/BATT_PATH";
    final String BATT_LEFT = "left";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        for(DataEvent event : dataEvents){
            DataItem dataItem = event.getDataItem();
            if(BATT_PATH.equals(dataItem.getUri().getPath())){
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                SimpleDigitalWatchFaceService.sBatteryLeftHandheld = dataMap.getInt(BATT_LEFT);
            }
        }
    }
}