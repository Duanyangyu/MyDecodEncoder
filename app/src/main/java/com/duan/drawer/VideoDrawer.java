package com.duan.drawer;

/**
 * Created by duanyy on 2017/10/27.
 */

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import com.duan.Utils.FboHelper;
import com.duan.Utils.GlUtil;
import com.duan.Utils.ShaderUtils;

import java.nio.FloatBuffer;

/**
 * 绘制视频内容。
 */
public class VideoDrawer extends Drawer {

    public static final String TAG = "VideoDrawer";

    private int mProgramIdOES;
    private FboHelper mFbo;

    private float[] mMvpMatrix = new float[16];

    public VideoDrawer() {
        super();
        init();
    }

    @Override
    protected void init() {
        Matrix.setIdentityM(mMvpMatrix,0);
    }

    protected void initProgram(){
        mProgramIdOES = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_OES);
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

    @Override
    public int getTargetTextureId() {
        if (mFbo != null) {
            return mFbo.textureId();
        }
        return 0;
    }

    @Override
    public void onSurfaceCreate() {
        super.onSurfaceCreate();
    }

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    @Override
    public void onSurfaceChanged(int width,int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        releaseFbo();
        initFbo((int)width,(int)height);
    }

    @Override
    public void onInputSizeChanged(float width, float height,int rotation) {

    }

    @Override
    public void drawFrame(int textureId) {

    }

    @Override
    public void drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer, int rotation) {
        GLES20.glClearColor(1.0f,1.0f,1.0f,1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT|GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFbo.frameId());

        GLES20.glUseProgram(mProgramIdOES);
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"));
        GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"));

        GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"),2,GLES20.GL_FLOAT,false,0,vertexBuffer);
        GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"),2,GLES20.GL_FLOAT,false,0,textureBuffer);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgramIdOES, "u_MVPMatrix"),1,false,mMvpMatrix,0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramIdOES, "u_sampleTexture"),0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_position"));
        GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(mProgramIdOES, "a_textCoord"));

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
        GLES20.glUseProgram(0);
    }

    @Override
    public void release() {
        if (mFbo != null) {
            mFbo.close();
            mFbo = null;
        }
    }

}
