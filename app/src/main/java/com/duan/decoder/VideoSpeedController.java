package com.duan.decoder;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by duanyy on 2017/10/24.
 */

public class VideoSpeedController implements VideoDecoder.IVideoSpeedCallback{

    public static final String TAG = "VideoSpeedController";

    //每20ms计时一次
    public static final int TIME_INTERNAL_US = 20000;

    //计时器记录的当前时间
    private long mRealTimeUs;

    private float mVideoSpeedRate = 1.f;

    private Timer mTimer;
    private TimerTask mTimeTask;
    private boolean mTimeRunning;

    public void runingTimer(boolean run){
        this.mTimeRunning = run;
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimeTask == null) {
            mTimeTask = new TimerTask() {
                @Override
                public void run() {
                    while (mTimeRunning){
                        mRealTimeUs += TIME_INTERNAL_US;
                        try {
                            Thread.sleep(TIME_INTERNAL_US/1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            mTimer.schedule(mTimeTask,0,TIME_INTERNAL_US/1000);
        }
    }

    @Override
    public void onPreRender(long presentationTimeUs) {
        boolean needSleep = !(presentationTimeUs == 0);
        long frameDelta = presentationTimeUs - mRealTimeUs;
        frameDelta *= mVideoSpeedRate;
        Log.d(TAG,"presentationTimeUs="+presentationTimeUs+", mRealTimeUs="+mRealTimeUs+", frameDelta="+frameDelta);
        if (needSleep){
            if (frameDelta > 0){
                long sleep_ms = frameDelta / 1000;
                int sleep_ns = (int) (frameDelta % 1000 * 1000);
                Log.d(TAG,"sleep_ms="+sleep_ms+", sleep_ns="+sleep_ns);
                if (sleep_ms > 0){
                    try {
                        Thread.sleep(sleep_ms, sleep_ns);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    public void release(){
        mTimeRunning = false;

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (mTimeTask != null) {
            mTimeTask.cancel();
            mTimeTask = null;
        }
    }
}
