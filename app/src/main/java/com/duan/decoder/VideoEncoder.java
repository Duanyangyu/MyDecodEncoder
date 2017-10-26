package com.duan.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by duanyy on 2017/10/26.
 */

public class VideoEncoder {

    private static final String TAG = "VideoEncoder";
    private static final String ENCODER_NAME_MP4 = "OMX.google.mpeg4.encoder";

    private static final int TIME_OUT_MS = 10000;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int BIT_RATE = 1920*1080*10;            // 2Mbps
    public static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 30;

    private MediaCodec mVideoEncoder;

    public VideoEncoder() {
        listEncoderType();
        initEncoder();
    }

    private boolean initEncoder(){
        try {
            mVideoEncoder = MediaCodec.createByCodecName(ENCODER_NAME_MP4);
            mVideoEncoder.configure(getConfigureFormat(),null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void startEncode(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                drain();
            }
        }.start();
    }

    private MediaFormat getConfigureFormat(){
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_MPEG4,WIDTH,HEIGHT);
        //...configure
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE,BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,IFRAME_INTERVAL);

        return format;
    }

    private void drain(){
        boolean inputEOS = false;
        boolean outputEOS = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!outputEOS){
            if (!outputEOS){
                int outBufferIndex = mVideoEncoder.dequeueOutputBuffer(bufferInfo, TIME_OUT_MS);
                if (outBufferIndex >= 0){
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                        outputEOS = true;
                        continue;
                    }
                    ByteBuffer outputBuffer = mVideoEncoder.getOutputBuffer(outBufferIndex);
                    byte[] array = outputBuffer.array();
                    Log.e(TAG,"drain outBuffer.length="+array);

                }
            }
        }
    }

    private void listEncoderType(){
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()){
                Log.d(TAG,"Encoder name="+codecInfo.getName());
            }
        }
    }

}
