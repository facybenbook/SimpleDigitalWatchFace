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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Date;
import java.util.TimeZone;

/**
 * Sample analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p/>
 * 一般的にAmbientModeのときは分間隔で更新し、リアルタイム更新時秒(未満)間隔で更新する
 */
public class AnalogWatchFaceServiceBackup extends CanvasWatchFaceService {
    private static final String TAG = "AnalogWatchFaceService";

    /**
     * リアルタイム更新時の更新間隔（AmbientModeの時は使用されず、onTimeTickのみを使用する。
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 50;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        /**
         * タイムゾーンが変更された場合に通知
         */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.set(System.currentTimeMillis());
            }
        };
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mTickPaint;
        Paint mArcBackgroundPaint;
        Paint mTextDatePaint;
        java.text.DateFormat mMediumDateFormat;
        Paint mTextTimePaint;
        Paint mTextTimeSecondsPaint;
        boolean mMute;
        Time mTime;

        /**
         * バッテリーの確認で使用
         */
        BatteryManager mBm;
        int mBatteryRemainWear;
        int mBatteryRemainHand;




        /**
         * アクティブな状態（shouldTimerBeRunning:表示状態で、アンビエントモードでないとき）
         * 再描画(invalidate)させ、INTERACTIVE_UPDATE_RATE_MS後に再実行する
         */
        /**
         * AmbientMode時は0、アクティブモード時は100になっていれば平常稼動
         * なっていない場合、数字に応じた描画を行う
         */
        int mAnimateStep = 0;
        /**
         * Handler to update the time once a second in interactive mode.
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
                        if(isVisible() && !isInAmbientMode()){
                            mAnimateStep = Math.min(100,mAnimateStep+10);
                        }else{
                            mAnimateStep = Math.max(0,mAnimateStep-10);
                        }
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
        boolean mRegisteredTimeZoneReceiver = false;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mTickBitmap;

        /**
         * Android標準のライフサイクルonCreateと同義
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceServiceBackup.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = AnalogWatchFaceServiceBackup.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            mTickBitmap =BitmapFactory.decodeResource(resources, R.drawable.clock_tick);
            mTickBitmap = resize(mTickBitmap,240,240);

            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 255, 255, 255);
            mHourPaint.setStrokeWidth(10.0f);
            mHourPaint.setStyle(Paint.Style.STROKE);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.BUTT);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 255, 255, 255);
            mMinutePaint.setStrokeWidth(10.0f);
            mMinutePaint.setStyle(Paint.Style.STROKE);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.BUTT);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 255, 255);
            mSecondPaint.setStrokeWidth(10.0f);
            mSecondPaint.setStyle(Paint.Style.STROKE);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.BUTT);

            mArcBackgroundPaint = new Paint();
            mArcBackgroundPaint.setARGB(128, 128, 0, 0);
            mArcBackgroundPaint.setStrokeWidth(mHourPaint.getStrokeWidth()+mMinutePaint.getStrokeWidth()+mSecondPaint.getStrokeWidth());
            mArcBackgroundPaint.setStyle(Paint.Style.STROKE);
            mArcBackgroundPaint.setAntiAlias(true);
            mArcBackgroundPaint.setStrokeCap(Paint.Cap.BUTT);

            mTickPaint = new Paint();
            mTickPaint.setARGB(100, 255, 255, 255);
            mTickPaint.setStrokeWidth(2.f);
            mTickPaint.setAntiAlias(true);

            Typeface typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "tt0250m.ttf");

            mTextDatePaint = new Paint();
            mTextDatePaint.setARGB(255, 255, 255, 255);
            mTextDatePaint.setStrokeWidth(1.f);
            mTextDatePaint.setAntiAlias(true);
            mTextDatePaint.setTextSize(20f);
            mTextDatePaint.setStrokeCap(Paint.Cap.BUTT);
            mTextDatePaint.setTypeface(typeface);

            mTextTimePaint = new Paint();
            mTextTimePaint.setARGB(255, 255, 255, 255);
            mTextTimePaint.setStrokeWidth(1.f);
            mTextTimePaint.setAntiAlias(true);
            mTextTimePaint.setTextSize(50f);
            mTextTimePaint.setStrokeCap(Paint.Cap.BUTT);
            mTextTimePaint.setTypeface(typeface);

            mMediumDateFormat = DateFormat.getMediumDateFormat(getApplicationContext());

            mBm = new BatteryManager();
            mBm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);


            mTime = new Time();
        }

        /**
         * Android標準のライフサイクルonDestroyと同義
         */
        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        /**
         * プロパティチェンジ時に呼び出される（らしい）
         * TODO 明確に処理を理解し記述する
         *
         * @param properties
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
            invalidate();
        }

        /**
         * AmbientModeへの変更、あるいは復帰時にコールのはず
         * TODO ↑の確証を得る
         *
         * @param inAmbientMode
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mTickPaint.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * ミュートモードへ移行・復帰時にコール
         * ミュートモード時にはINTERRUPTION_FILTER_NONEと同じ値が帰る？
         * TODO ほかに役目ある？
         *
         * @param interruptionFilter
         */
        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        /**
         * 至る箇所からコールされるinvalidateに応じて適切なタイミングで走る。
         * 頻度はAmbientModeで毎分（onTimeTick）, アクティブ時でINTERACTIVE_UPDATE_RATE_MSミリ秒毎
         *
         * @param canvas
         * @param bounds
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Long milliseconds = System.currentTimeMillis();
            mTime.set(milliseconds);

            int width = bounds.width();
            int height = bounds.height();
            float animStep = mAnimateStep / 100f;
            float animLength =
                    -1 * animStep*(animStep-2.0f);

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            RectF rectf = new RectF(
                    mArcBackgroundPaint.getStrokeWidth() / 2,
                    mArcBackgroundPaint.getStrokeWidth() / 2,
                    width - mArcBackgroundPaint.getStrokeWidth() / 2,
                    height - mArcBackgroundPaint.getStrokeWidth() / 2
            );
            canvas.drawArc(rectf, 0 - 90, 270f * animLength , false, mArcBackgroundPaint);

            rectf = new RectF(
                    mSecondPaint.getStrokeWidth() / 2,
                    mSecondPaint.getStrokeWidth() / 2,
                    width - mSecondPaint.getStrokeWidth() / 2,
                    height - mSecondPaint.getStrokeWidth() / 2
            );
            canvas.drawArc(rectf, 0 - 90, (mTime.second + (milliseconds - mTime.toMillis(true)) / 1000f) * 4.5f * Math.max(0, animLength * 5f - 4f), false, mSecondPaint);

            rectf = new RectF(
                    mMinutePaint.getStrokeWidth()/2 + mSecondPaint.getStrokeWidth(),
                    mMinutePaint.getStrokeWidth()/2 + mSecondPaint.getStrokeWidth(),
                    width - (mMinutePaint.getStrokeWidth()/2 + mSecondPaint.getStrokeWidth()),
                    height - (mMinutePaint.getStrokeWidth()/2 + mSecondPaint.getStrokeWidth())
            );
            canvas.drawArc(rectf, 0 - 90, (mTime.minute*60f + mTime.second) * 0.075f * Math.max(0,animLength*2f-1f), false, mMinutePaint);

            rectf = new RectF(
                    mHourPaint.getStrokeWidth()/2 + mMinutePaint.getStrokeWidth() + mSecondPaint.getStrokeWidth(),
                    mHourPaint.getStrokeWidth()/2 + mMinutePaint.getStrokeWidth() + mSecondPaint.getStrokeWidth(),
                    width - (mHourPaint.getStrokeWidth()/2 + mMinutePaint.getStrokeWidth() + mSecondPaint.getStrokeWidth()),
                    height - (mHourPaint.getStrokeWidth()/2 + mMinutePaint.getStrokeWidth() + mSecondPaint.getStrokeWidth())
            );
            canvas.drawArc(rectf, 0 - 90, (mTime.hour*60f+mTime.minute) * 0.1875f * animLength, false, mHourPaint);

            float cx = rectf.centerX() - mTickBitmap.getWidth()/2;
            float cy = rectf.centerY() - mTickBitmap.getHeight()/2;

            /*
            canvas.save();

            canvas.rotate((milliseconds - mTime.toMillis(true)) * 0.36f,cx + (mTickBitmap.getWidth() / 2), cy + (mTickBitmap.getHeight() / 2));
            Paint p = new Paint();
            p.setAlpha((int)(animLength * 255));
            canvas.drawBitmap(mTickBitmap, cx, cy, p);
            canvas.restore();
 */

            canvas.drawText(mMediumDateFormat.format(new Date(milliseconds)), 0, rectf.centerY() - mTextTimePaint.getFontMetrics().bottom - 50, mTextDatePaint);
            canvas.drawText(DateFormat.format("kk:mm",milliseconds).toString(), 0, rectf.centerY() - mTextTimePaint.getFontMetrics().bottom, mTextTimePaint);

        }

        /**
         * 可視性に変更があった場合にコールされるらしい。
         * TODO 正確に挙動を理解する
         *
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.set(System.currentTimeMillis());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * タイムゾーン変更通知を登録
         */
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFaceServiceBackup.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        /**
         * タイムゾーン変更通知を解除
         */
        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AnalogWatchFaceServiceBackup.this.unregisterReceiver(mTimeZoneReceiver);
        }

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
        private boolean shouldTimerBeRunning() {
            if(isVisible() && !isInAmbientMode()) {
                return true;
            }else if (mAnimateStep > 0){
               return true;
            }
            return false;
        }


        /**
         * 画像リサイズ
         * @param bitmap 変換対象ビットマップ
         * @param newWidth 変換サイズ横
         * @param newHeight 変換サイズ縦
         * @return 変換後Bitmap
         */
        public Bitmap resize(Bitmap bitmap, int newWidth, int newHeight) {

            if (bitmap == null) {
                return null;
            }

            int oldWidth = bitmap.getWidth();
            int oldHeight = bitmap.getHeight();

            if (oldWidth < newWidth && oldHeight < newHeight) {
                // 縦も横も指定サイズより小さい場合は何もしない
                return bitmap;
            }

            float scaleWidth = ((float) newWidth) / oldWidth;
            float scaleHeight = ((float) newHeight) / oldHeight;
            float scaleFactor = Math.min(scaleWidth, scaleHeight);

            Matrix scale = new Matrix();
            scale.postScale(scaleFactor, scaleFactor);

            Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, oldWidth, oldHeight, scale, false);
            bitmap.recycle();

            return resizeBitmap;

        }

    }
}
