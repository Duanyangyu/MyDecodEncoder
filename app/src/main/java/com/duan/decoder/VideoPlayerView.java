package com.duan.decoder;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.duan.Utils.CoordinateUtils;
import com.duan.Utils.FboHelper;
import com.duan.Utils.GlUtil;
import com.duan.Utils.ShaderUtils;
import com.duan.Utils.TextureRotationUtil;
import com.duan.drawer.VideoDrawer;
import com.duan.drawer.WaterMarkerDrawer;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by duanyy on 2017/10/23.
 */

public class VideoPlayerView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "VideoPlayerView";

    private Context mContext;

    private VideoDecoder mDecoder;
    private int mProgramIdOES;
    private int mProgramId;
    private int mTextureId = -1;

    private float mViewWidth;
    private float mViewHeight;
    private VideoDrawer mVideoDrawer;
    private WaterMarkerDrawer mWatermarkerDrawer;
    private VideoEncoder mEncoder;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    public VideoPlayerView(Context context) {
        this(context,null);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    private void init(){
        setEGLContextClientVersion(2);

        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        mVertexBuffer = GlUtil.createFloatBuffer(CoordinateUtils.VERTEX_COORDINATE_DEFAULT);
        mTextureBuffer = GlUtil.createFloatBuffer(CoordinateUtils.TEXTURE_COORDINATE_NO_ROTATION);
    }

    private void initVideoDecoder(String path){

        if (mDecoder != null) {
            mDecoder.destroy();
        }
        mDecoder = new VideoDecoder();
        mDecoder.setDecodeCallback(mDecodeCallback);
        mDecoder.setDataSource(mContext,path);
        mTextureId = mDecoder.getTargetTextureId();
        initTextureBufferByRotation(mDecoder.getVideoRotation());
        initVertexBufferByVideoSize(mDecoder.getVideoWidth(),mDecoder.getVideoHeight(),mDecoder.getVideoRotation());



        mVideoDrawer.onInputSizeChanged(mDecoder.getVideoWidth(),mDecoder.getVideoHeight(),mDecoder.getVideoRotation());
        mWatermarkerDrawer.onInputSizeChanged(mDecoder.getVideoWidth(),mDecoder.getVideoHeight(),mDecoder.getVideoRotation());
    }

    private void initVideoEncoder(){
        mEncoder = new VideoEncoder();
        VideoDecoder decoder = new VideoDecoder(true);
        mEncoder.setVideoDecoder(decoder);
    }

    public void setDataSource(final String path){
        this.queueEvent(
                new Runnable() {
                    @Override
                    public void run() {
                        initVideoDecoder(path);
                        initVideoEncoder();
                    }
                }
        );
    }

    public void reEncode(){
        if (mEncoder != null) {
            mEncoder.startEncode();
        }
    }

    public void playVideo(){
        this.queueEvent(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mDecoder != null) {
                            mDecoder.play();
                        }
                    }
                }
        );
    }

    public void resumeVideo(){
        this.queueEvent(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mDecoder != null) {
                            mDecoder.resumeVideo();
                        }
                    }
                }
        );
    }

    public void pauseVideo(){
        if (mDecoder != null) {
            mDecoder.pauseVideo();
        }
    }

    public void destroy(){
        if (mDecoder != null) {
            mDecoder.destroy();
        }
        if (mVideoDrawer != null) {
            mVideoDrawer.release();
        }
        if (mWatermarkerDrawer != null) {
            mWatermarkerDrawer.release();
        }
    }

    private VideoDecoder.IDecodeCallback mDecodeCallback = new VideoDecoder.IDecodeCallback() {
        @Override
        public void onFrameDecoded() {
            requestRender();
        }
    };



    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mProgramIdOES = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_OES);
        mProgramId = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_SIMPLE);

        Matrix.setIdentityM(mMvpMatrix,0);

        mVideoDrawer = new VideoDrawer();
        mWatermarkerDrawer = new WaterMarkerDrawer(mContext);

        mVideoDrawer.onSurfaceCreate();
        mWatermarkerDrawer.onSurfaceCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        Log.e(TAG,"onSurfaceChanged width="+width+", height="+height);
        this.mViewWidth = width;
        this.mViewHeight = height;

        mVideoDrawer.onSurfaceChanged(width,height);
        mWatermarkerDrawer.onSurfaceChanged(width,height);
    }

    private float[] mMvpMatrix = new float[16];
    private FloatBuffer mVertexCoordsBuffer;
    private FloatBuffer mFragmentCoordsBuffer;

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG,"onDrawFrame mTextureId="+mTextureId);
        if (mTextureId < 0){
            return;
        }

        if (mDecoder != null) {
            mDecoder.drawFrame();
        }

        int targetTextureId = -1;
        if (mVideoDrawer != null) {
            mVideoDrawer.drawFrame(mTextureId,mVertexCoordsBuffer,mFragmentCoordsBuffer,0);
            targetTextureId = mVideoDrawer.getTargetTextureId();
        }

        if (mWatermarkerDrawer != null && targetTextureId != -1) {
            mWatermarkerDrawer.drawFrame(targetTextureId,mVertexCoordsBuffer,mFragmentCoordsBuffer,0);
            targetTextureId = mWatermarkerDrawer.getTargetTextureId();
        }

        if (targetTextureId != -1){
            GLES20.glUseProgram(mProgramId);

            GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_position"));
            GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_textCoord"));

            GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramId, "a_position"),2,GLES20.GL_FLOAT,false,0,mVertexBuffer);
            GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramId, "a_textCoord"),2,GLES20.GL_FLOAT,false,0,mFragmentCoordsBuffer);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgramId, "u_MVPMatrix"),1,false,mMvpMatrix,0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,targetTextureId);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramId, "u_sampleTexture"),3);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

            GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_position"));
            GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_textCoord"));
            GLES20.glUseProgram(0);
        }
    }

    private void initVertexBufferByVideoSize(float inputWidth,float inputHeight,int rotation){
        float width = 1.f;
        float height = 1.f;

        float realVideoWidth = inputWidth;
        float realVideoHeight = inputHeight;
        if (inputWidth != 0 && inputHeight != 0){
            if (rotation == 90 || rotation == 270){
                realVideoWidth = inputHeight;
                realVideoHeight = inputWidth;
            }
            float containerRatio = mViewWidth/mViewHeight;
            float textureRatio = realVideoWidth/realVideoHeight;
            if (textureRatio > containerRatio){
                height = containerRatio/textureRatio;
            }else {
                width = textureRatio/containerRatio;
            }
        }

        float[] vertexCoords = {
                -width,-height,
                width,-height,
                -width,height,
                width,height
        };

        Log.e(TAG,"initVertexBufferByVideoSize realVideoWidth="+realVideoWidth+", realVideoHeight="+realVideoHeight);
        String log = "initVertexBufferByVideoSize array: ";
        int length = vertexCoords.length;
        for (int i = 0; i < length; i++) {
            log += vertexCoords[i]+", ";
        }
        Log.e(TAG,log);

        mVertexCoordsBuffer = GlUtil.createFloatBuffer(vertexCoords);
    }

    private void initTextureBufferByRotation(int rotation){
        float[] array = CoordinateUtils.TEXTURE_COORDINATE_NO_ROTATION;
        switch (rotation){
            case 0:
                array = CoordinateUtils.TEXTURE_COORDINATE_NO_ROTATION;
                break;
            case 90:
                array = TextureRotationUtil.TEXTURE_ROTATED_90;
                break;
            case 180:
                array = TextureRotationUtil.TEXTURE_ROTATED_180;
                break;
            case 270:
                array = TextureRotationUtil.TEXTURE_ROTATED_270;
                break;
        }
        String log = "initTextureBufferByRotation rotation="+rotation+", array:";
        int length = array.length;
        for (int i = 0; i < length; i++) {
            log += array[i]+", ";
        }
        Log.e(TAG,log);
        mFragmentCoordsBuffer = GlUtil.createFloatBuffer(array);
    }


}
