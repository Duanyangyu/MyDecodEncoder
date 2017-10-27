package com.duan.decoder;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.duan.Utils.FileUtils;
import com.duan.Utils.GlUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by duanyy on 2017/10/23.
 */

public class VideoDecoder {

    private static final String TAG = "VideoDecoder";

    private MediaCodec mDecoder;
    private int mTextureId = -1;
    private Surface mOutputSurface;
    private MediaExtractor mExtractor;
    private IDecodeCallback mDecodeCallback;
    private SurfaceTexture mSurfaceTexture;

    private IVideoSpeedCallback mVideoSpeedController;
    private int mVideoRotation;
    private int mVideoHeight;
    private int mVideoWidth;
    private MediaMetadataRetriever mRetriever;

    //是否为重新编码模式
    private boolean mExport;

    public VideoDecoder() {

    }

    public VideoDecoder(boolean mExport) {
        this.mExport = mExport;
    }

    public interface IDecodeCallback{
        void onFrameDecoded();
    }

    public interface IVideoSpeedCallback{
        /**
         * call this before codec.releaseOutputBuffer method.
         */
        void onPreRender(long presentationTimeUs);
    }

    public void setDataSource(Context context, String source){
        Log.e(TAG,"setDataSource source="+source);
        if (TextUtils.isEmpty(source)){
            throw new RuntimeException("非法的本地文件！");
        }

        mRetriever = new MediaMetadataRetriever();
        mExtractor = new MediaExtractor();
        mVideoSpeedController = new VideoSpeedController();
        try {
            if (source.contains("asset")){
                AssetFileDescriptor fileDescriptor = context.getAssets().openFd("video/"+source);
                mRetriever.setDataSource(fileDescriptor.getFileDescriptor(),fileDescriptor.getStartOffset(),fileDescriptor.getLength());
                mExtractor.setDataSource(fileDescriptor.getFileDescriptor(),fileDescriptor.getStartOffset(),fileDescriptor.getLength());
            }else {
                if (!FileUtils.checkFile(source)){
                    throw new RuntimeException("非法的本地文件！");
                }
                mRetriever.setDataSource(source);
                mExtractor.setDataSource(source);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        initVideoInfo();
        boolean result = initDecoder();
        Log.e(TAG,"setDataSource result="+result);
    }

    public void setDecodeCallback(IDecodeCallback mDecodeCallback) {
        this.mDecodeCallback = mDecodeCallback;
    }

    public int getTargetTextureId(){
        return mTextureId;
    }

    public void play(){
        Log.e(TAG,"play()~~");
        new Thread(){
            @Override
            public void run() {
                super.run();
                runingTimer(true);
                decodeLoop();
            }
        }.start();
    }

    public void resumeVideo(){
        Log.e(TAG,"resumeVideo()~~");
        synchronized (mSync){
            this.mRequestPause = false;
            runingTimer(true);
            mSync.notifyAll();
        }
    }

    public void pauseVideo(){
        synchronized (mSync) {
            Log.e(TAG,"pauseVideo()~~");
            runingTimer(false);
            this.mRequestPause = true;
        }
    }

    public void destroy(){
        synchronized (mSync) {
            Log.e(TAG,"destroy()~~");
            this.mRequestRelease = true;
        }
    }

    private void runingTimer(boolean start){
        if (mVideoSpeedController != null) {
            if (mVideoSpeedController instanceof VideoSpeedController){
                ((VideoSpeedController)mVideoSpeedController).runingTimer(start);
            }
        }
    }

    private boolean initDecoder(){
        MediaFormat format = selectMediaTrack("video/");
        if (format == null) {
            throw new RuntimeException("非法的本地文件！");
        }

        try {
            mDecoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (mDecoder != null) {
            initSurfaceTexture();
            mDecoder.configure(format,mOutputSurface,null,0);
            mDecoder.start();
        }else {
            return false;
        }
        return true;
    }

    private void initVideoInfo(){
        if (mRetriever == null) {
            return;
        }
        mVideoRotation = Integer.parseInt(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Log.e(TAG,"initVideoInfo rotation="+ mVideoRotation);
//        mVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
//        mVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
//        Log.e(TAG,"mVideoWidth="+mVideoWidth+", mVideoHeight="+mVideoHeight);
    }

    public int getVideoRotation(){
        return mVideoRotation;
    }

    public int getVideoWidth(){
        return mVideoWidth;
    }

    public int getVideoHeight(){
        return mVideoHeight;
    }

    private void initSurfaceTexture(){
        Log.e(TAG,"initSurfaceTexture mTextureId="+mTextureId);
        mTextureId = GlUtil.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(mSurfaceFrameAvailableListener);
        mOutputSurface = new Surface(mSurfaceTexture);
    }

    private MediaFormat selectMediaTrack(String type){
        if (mExtractor == null) {
            return null;
        }
        int count = mExtractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(type)){
                mExtractor.selectTrack(i);
                return format;
            }
        }
        return null;
    }

    private void release(){
        if (mDecoder != null) {
            mDecoder.release();
        }
        if (mExtractor != null) {
            mExtractor.release();
        }
        if (mRetriever != null) {
            mRetriever.release();
        }
        if (mOutputSurface != null) {
            mOutputSurface.release();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        GlUtil.deleteGlTexture(mTextureId);

        if (mVideoSpeedController != null) {
            if (mVideoSpeedController instanceof VideoSpeedController){
                ((VideoSpeedController)mVideoSpeedController).release();
            }
        }
    }

    private static final int TIME_OUT = 10000;
    private Object mSync = new Object();
    private boolean mRequestPause;
    private boolean mRequestRelease;

    private void decodeLoop(){
        boolean inputEOS = false;
        boolean outputEOS = false;
        boolean requestPause;
        boolean requestRelease;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (!outputEOS){
            Log.d(TAG,"decodeLoop~~");
            synchronized (mSync){
                requestPause = mRequestPause;
                requestRelease = mRequestRelease;
            }

            if (requestPause){
                synchronized (mSync){
                    try {
                        mSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (requestRelease){
                release();
                break;
            }

            if (!inputEOS){
                int inputBufferIndex = mDecoder.dequeueInputBuffer(TIME_OUT);
                Log.d(TAG,"decodeLoop inputBufferIndex="+inputBufferIndex);
                if (inputBufferIndex >= 0){
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferIndex);
                    int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
                    long sampleTime = mExtractor.getSampleTime();
                    Log.d(TAG,"decodeLoop sampleSize="+sampleSize+",  sampleTime="+sampleTime);
                    if (sampleSize > 0){
                        mDecoder.queueInputBuffer(inputBufferIndex,0,sampleSize,sampleTime,0);
                    }else {
                        inputEOS = true;
                        mDecoder.queueInputBuffer(inputBufferIndex,0,0,sampleTime,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                    mExtractor.advance();
                }
            }

            int outputBufferIndex = mDecoder.dequeueOutputBuffer(bufferInfo, TIME_OUT);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
                Log.d(TAG,"decodeLoop outputBufferIndex="+outputBufferIndex+" ---> INFO_TRY_AGAIN_LATER");
            }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.d(TAG,"decodeLoop outputBufferIndex="+outputBufferIndex+" ---> INFO_OUTPUT_FORMAT_CHANGED");
            }else if (outputBufferIndex < 0){
                Log.d(TAG,"decodeLoop outputBufferIndex="+outputBufferIndex);
            }else {
                Log.d(TAG,"decodeLoop outputBufferIndex="+outputBufferIndex);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    outputEOS = true;
                    runingTimer(false);
                    Log.e(TAG,"decodeLoop ---------outputEOS="+outputEOS);
                }
                if (bufferInfo.size > 0){
                    if (mDecodeCallback != null) {
                        mDecodeCallback.onFrameDecoded();
                    }
                }

                long presentationTimeUs = bufferInfo.presentationTimeUs;
                //导出模式时，用最大速度解码
                if (!mExport){
                    if (mVideoSpeedController != null) {
                        mVideoSpeedController.onPreRender(presentationTimeUs);
                    }
                }
                boolean needRender = !mExport;
                mDecoder.releaseOutputBuffer(outputBufferIndex,needRender);
            }
        }
    }

    private int	_updateTexImageCounter = 0;
    private int	_updateTexImageCompare = 0;
    private SurfaceTexture.OnFrameAvailableListener mSurfaceFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (this){
                _updateTexImageCounter++;
            }
        }
    };

    public void drawFrame(){
        if (mSurfaceTexture != null) {
            if (_updateTexImageCompare != _updateTexImageCounter){
                while (_updateTexImageCompare != _updateTexImageCounter){
                    mSurfaceTexture.updateTexImage();
                    _updateTexImageCompare++;
                }
            }
        }
    }

}
