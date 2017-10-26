package com.duan.decoder;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.duan.Utils.FboHelper;
import com.duan.Utils.GlUtil;
import com.duan.Utils.ShaderUtils;
import com.duan.Utils.TextureRotationUtil;
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
    private float mVideoWidth;
    private float mVideoHeight;
    private WaterMarkerDrawer mWatermarkerDrawer;
    private FboHelper mFbo;
    private VideoEncoder mEncoder;

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

        mWatermarkerDrawer = new WaterMarkerDrawer(mContext);
    }

    private void initVideoDecoder(String path){
        mDecoder = new VideoDecoder();
        mDecoder.setDecodeCallback(mDecodeCallback);
        mDecoder.setDataSource(mContext,path);
        initVertexBufferByVideoSize();
        initTextureBufferByRotation();
        mTextureId = mDecoder.getTargetTextureId();
    }

    private void initVideoEncoder(){
        mEncoder = new VideoEncoder();
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
        if (mFbo != null) {
            mFbo.close();
            mFbo = null;
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

    private void initVertexBufferByVideoSize(){
        float width = 1.f;
        float height = 1.f;

        releaseFbo();
        initFbo((int)mViewWidth,(int)mViewHeight);
        mWatermarkerDrawer.onSurfaceChanged((int)mViewWidth,(int)mViewHeight);

        if (mDecoder != null) {
            mVideoWidth = mDecoder.getVideoWidth();
            mVideoHeight = mDecoder.getVideoHeight();
        }

        if (mVideoWidth != 0 && mVideoHeight != 0){
            float realVideoWidth = mVideoWidth;
            float realVideoHeight = mVideoHeight;
            int rotation = mDecoder.getVideoRotation();
            if (rotation == 90 || rotation == 270){
                realVideoWidth = mVideoHeight;
                realVideoHeight = mVideoWidth;
            }
            float containerRatio = mViewWidth/mViewHeight;
            float textureRatio = realVideoWidth/realVideoHeight;
            if (textureRatio > containerRatio){
                height = containerRatio/textureRatio;
            }else {
                width = textureRatio/containerRatio;
            }
        }

        Log.e(TAG,"initVertexBufferByVideoSize width="+width+", height="+height);
        float[] vertexCoords = {
                -width,-height,
                width,-height,
                -width,height,
                width,height
        };
        mVertexCoordsBuffer = GlUtil.createFloatBuffer(vertexCoords);
    }

    private void initTextureBufferByRotation(){
        if (mDecoder != null) {
            int rotation = mDecoder.getVideoRotation();
            float[] vertex = FRAGMENT_COORDS;
            switch (rotation){
                case 0:
                    vertex = TextureRotationUtil.TEXTURE_NO_ROTATION;
                    break;
                case 90:
                    vertex = TextureRotationUtil.TEXTURE_ROTATED_90;
                    break;
                case 180:
                    vertex = TextureRotationUtil.TEXTURE_ROTATED_180;
                    break;
                case 270:
                    vertex = TextureRotationUtil.TEXTURE_ROTATED_270;
                    break;
            }
            mFragmentCoordsBuffer = GlUtil.createFloatBuffer(vertex);
        }
    }

    private void initFbo(int width,int height){
        mFbo = new FboHelper(width,height);
        mFbo.createFbo();
    }

    private void releaseFbo(){
        if (mFbo != null) {
            mFbo.close();
            mFbo = null;
        }
    }

    private float[] mMvpMatrix = new float[16];
    private FloatBuffer mVertexCoordsBuffer;
    private FloatBuffer mFragmentCoordsBuffer;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mProgramIdOES = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_OES);
        mProgramId = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_SIMPLE);
        Matrix.setIdentityM(mMvpMatrix,0);
        mWatermarkerDrawer.onSurfaceCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        Log.e(TAG,"onSurfaceChanged width="+width+", height="+height);
        this.mViewWidth = width;
        this.mViewHeight = height;
        releaseFbo();
        initFbo((int)mViewWidth,(int)mViewHeight);
        mWatermarkerDrawer.onSurfaceChanged(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(TAG,"onDrawFrame mTextureId="+mTextureId);
        if (mTextureId < 0){
            return;
        }

        if (mDecoder != null) {
            mDecoder.drawFrame();
        }

        GLES20.glClearColor(1.0f,1.0f,1.0f,1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT|GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFbo.frameId());

        GLES20.glUseProgram(mProgramIdOES);
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"));
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"));

        GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"),2,GLES20.GL_FLOAT,false,0,mVertexCoordsBuffer);
        GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"),2,GLES20.GL_FLOAT,false,0,mFragmentCoordsBuffer);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgramIdOES, "u_MVPMatrix"),1,false,mMvpMatrix,0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mTextureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramIdOES, "u_sampleTexture"),0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"));
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"));

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
        GLES20.glUseProgram(0);

        int targetTextureId = -1;
        if (mWatermarkerDrawer != null) {
            mWatermarkerDrawer.drawFrame(mFbo.textureId());
            targetTextureId = mWatermarkerDrawer.getTargetTextureId();
        }

        if (targetTextureId != -1){
            GLES20.glUseProgram(mProgramId);

            GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_position"));
            GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramId, "a_textCoord"));

            GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramId, "a_position"),2,GLES20.GL_FLOAT,false,0,mVertexCoordsBuffer);
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

    private static final float[] VERTEX_COORDS = {
            -1f,-1f,  1f,-1f,  -1f,1f,  1f,1f
    };

    private static final float[] FRAGMENT_COORDS = {
            0f,0f,  1f,0f,  0f,1f,  1f,1f
    };


}
