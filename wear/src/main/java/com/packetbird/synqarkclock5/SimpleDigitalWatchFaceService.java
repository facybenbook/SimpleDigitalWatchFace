/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.packetbird.synqarkclock5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 一般的にAmbientModeのときは分間隔で更新し、リアルタイム更新時秒(未満)間隔で更新する
 */
public class SimpleDigitalWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "AnalogWatchFaceService";

    public static int sBatteryLeftHandheld = 0;
    public static int sBatteryLeftWear = 0;



    /**
     * リアルタイム更新時の更新間隔（AmbientModeの時は使用されず、onTimeTickのみを使用する。
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 15;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        /**
         * アクティブな状態（shouldTimerBeRunning:表示状態で、アンビエントモードでないとき）
         * 再描画(invalidate)させ、INTERACTIVE_UPDATE_RATE_MS後に再実行する
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };


        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        java.text.DateFormat mMediumDateFormat;
        java.text.DateFormat mTimeFormat;

        Paint mTextDatePaint;
        Paint mTextTimePaint;
        Paint mTextLevelPaint;
        boolean mMute;
        Date mDate;

        StringBuffer mStringBuffer;
        StringBuffer mDateBuffer;
        StringBuffer mTimeBuffer;

        float mDrawingBatteryLeftHandheld;
        float mDrawingBatteryLeftWear;
        int mTimePassedSinceActive;

        float mHeightTimePaint;
        /**
         * Android標準のライフサイクルonCreateと同義
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SimpleDigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP)
                    .setShowUnreadCountIndicator(true)
                    .build());

            Date d = new Date();
            d.setTime(System.currentTimeMillis());


            Resources resources = SimpleDigitalWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            Typeface typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "tt0250m.ttf");

            mTextDatePaint = new Paint();
            mTextDatePaint.setARGB(255, 255, 255, 255);
            mTextDatePaint.setStrokeWidth(1.f);
            mTextDatePaint.setAntiAlias(true);
            mTextDatePaint.setTextSize(20f);
            mTextDatePaint.setStrokeCap(Paint.Cap.BUTT);
            mTextDatePaint.setTextAlign(Paint.Align.CENTER);
            mTextDatePaint.setTypeface(typeface);

            mTextTimePaint = new Paint();
            mTextTimePaint.setARGB(255, 255, 255, 255);
            mTextTimePaint.setStrokeWidth(1.f);
            mTextTimePaint.setAntiAlias(true);
            mTextTimePaint.setTextSize(50f);
            mTextTimePaint.setStrokeCap(Paint.Cap.BUTT);
            mTextTimePaint.setTextAlign(Paint.Align.CENTER);
            mTextTimePaint.setTypeface(typeface);

            mTextLevelPaint = new Paint();
            mTextLevelPaint.setARGB(255, 255, 255, 255);
            mTextLevelPaint.setStrokeWidth(1.f);
            mTextLevelPaint.setAntiAlias(true);
            mTextLevelPaint.setTextSize(20f);
            mTextLevelPaint.setStrokeCap(Paint.Cap.BUTT);
            mTextLevelPaint.setTextAlign(Paint.Align.CENTER);
            mTextLevelPaint.setTypeface(typeface);

            mMediumDateFormat = DateFormat.getDateFormat (getApplicationContext());
            mTimeFormat = new SimpleDateFormat("kk:mm");

            mDate = new Date();
            mDrawingBatteryLeftHandheld = 0;
            mDrawingBatteryLeftWear = 0;
            mTimePassedSinceActive = 0;

            mStringBuffer = new StringBuffer();
            mDateBuffer = new StringBuffer();
            mTimeBuffer = new StringBuffer();
            mHeightTimePaint = mTextTimePaint.getFontMetrics().bottom;

            updateTime();
        }


        FieldPosition fp = new FieldPosition(Field.DECLARED);
        /**
         * 至る箇所からコールされるinvalidateに応じて適切なタイミングで走る。
         * 頻度はAmbientModeで毎分（onTimeTick）, アクティブ時でINTERACTIVE_UPDATE_RATE_MSミリ秒毎
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d(TAG, "onDraw");

            int width = bounds.width();
            int height = bounds.height();

            int barWidth = (int)(bounds.width()*0.02f);
            int barOffset =  (int)(bounds.width()*0.0875f);

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true);//  filter
            }

            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            canvas.drawText(mDateBuffer, 0, mDateBuffer.length(), width / 2, height / 2 + 15 - mHeightTimePaint - 50, mTextDatePaint);
            canvas.drawText(mTimeBuffer, 0, mTimeBuffer.length(), width/2, height/2 + 15 - mHeightTimePaint, mTextTimePaint);

            if(shouldTimerBeRunning()) {
                mDrawingBatteryLeftHandheld += (sBatteryLeftHandheld - mDrawingBatteryLeftHandheld)*0.2f;
                if(Math.abs(mDrawingBatteryLeftHandheld - sBatteryLeftHandheld) < 0.1 ) mDrawingBatteryLeftHandheld = sBatteryLeftHandheld;
                mDrawingBatteryLeftWear += (sBatteryLeftWear - mDrawingBatteryLeftWear) * 0.2f;
                if(Math.abs(mDrawingBatteryLeftWear - sBatteryLeftWear) < 0.1 ) mDrawingBatteryLeftWear = sBatteryLeftWear;
                ++mTimePassedSinceActive;

                mTextLevelPaint.setAlpha(255);
                mTextLevelPaint.setTextAlign(Paint.Align.RIGHT);

                mStringBuffer.delete(0,mStringBuffer.length());
                mStringBuffer.append((int)mDrawingBatteryLeftWear);
                canvas.drawText(mStringBuffer, 0, mStringBuffer.length(), width, 22, mTextLevelPaint);

                mTextLevelPaint.setTextAlign(Paint.Align.LEFT);
                mStringBuffer.delete(0,mStringBuffer.length());
                mStringBuffer.append((int)mDrawingBatteryLeftHandheld);
                canvas.drawText(mStringBuffer, 0, mStringBuffer.length(), 0, 22, mTextLevelPaint);

                int barMaxHeight = barOffset + (height-barOffset);
                canvas.drawRect(0f, barOffset, barWidth, barMaxHeight * (float) (mDrawingBatteryLeftHandheld * 0.01), mTextLevelPaint);
                canvas.drawRect(width - barWidth, barOffset, width, barMaxHeight  * (float) (mDrawingBatteryLeftWear * 0.01), mTextLevelPaint);

            }else{
                mDrawingBatteryLeftHandheld = 0;
                mDrawingBatteryLeftWear = 0;
                mTimePassedSinceActive =0;
            }
        }

        /**
         * Android標準のライフサイクルonDestroyと同義
         */
        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        boolean mLowBitAmbient;
        /**
         * LowBitAmbientへの移動を確認
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        /**
         * AmbientModeだろうがそうでなかろうが「分針が変わるタイミング」でコールされる。
         * 結果的に大体一分毎に更新となる
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            updateTime();
            invalidate();
        }

        private void updateTime() {
            mDate.setTime(System.currentTimeMillis());
            mDateBuffer.delete(0,mDateBuffer.length());
            mMediumDateFormat.format(mDate,mDateBuffer,fp);
            mTimeBuffer.delete(0,mTimeBuffer.length());
            mTimeFormat.format(mDate,mTimeBuffer,fp);
        }

        /**
         * AmbientModeへの変更、あるいは復帰時にコール
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if(inAmbientMode){
                System.gc();
            }
            /*
            if (mLowBitAmbient) {
                //boolean antiAlias = !inAmbientMode;
            }*/
            invalidate();

            updateTimer();
        }

        /**
         * ミュートモードへ移行・復帰時にコール
         * ミュートモード時にはINTERRUPTION_FILTER_NONEと同じ値が帰る？
         */
        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                invalidate();
            }
        }

        /**
         * 可視性に変更があった場合にコールされる。
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        boolean mRegisteredReceiver = false;

        /**
         * タイムゾーン変更通知を登録
         */
        private void registerReceiver() {
            if (mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = true;
            SimpleDigitalWatchFaceService.this.registerReceiver(mBatteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        /**
         * タイムゾーン変更通知を解除
         */
        private void unregisterReceiver() {
            if (!mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = false;
            SimpleDigitalWatchFaceService.this.unregisterReceiver(mBatteryChangedReceiver);
        }

        /**
         * バッテリー容量(自分）が変更された場合に通知
         */
        final BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sBatteryLeftWear = intent.getIntExtra("level",0);
            }
        };

        /**
         * 更新タイミングの変更が必要なイベントが発生したときに、それぞれの関数から呼ばれる
         * 高頻度更新が必要かを判断しセット
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * @return 高頻度更新が必要なときにtrue
         */
        private boolean shouldTimerBeRunning() {return isVisible() && !isInAmbientMode();}
    }
}
