package com.duan.drawer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.duan.R;
import com.duan.Utils.CoordinateUtils;
import com.duan.Utils.FboHelper;
import com.duan.Utils.GlUtil;
import com.duan.Utils.ShaderUtils;

import java.nio.FloatBuffer;

/**
 * Created by duanyy on 2017/10/25.
 */

/**
 * 绘制水印图片。
 */
public class WaterMarkerDrawer extends Drawer{

    private static final String TAG = "WaterMarkerDrawer";
    
    private Context mContext;

    private float[] mMvpMatrix = new float[16];
    private float[] mIdentyMatrix = new float[16];
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    
    public static final int WIDTH_WATER_MARKER = 200;
    public static final int HEIGHT_WATER_MARKER = 200;
    private int mProgramId;
    private int mTextureId;
    private FboHelper mFbo;

    public WaterMarkerDrawer(Context context) {
        super();
        this.mContext = context;
        init();
    }

    @Override
    public void init(){
        mVertexBuffer = GlUtil.createFloatBuffer(CoordinateUtils.VERTEX_COORDINATE_DEFAULT);
        mTextureBuffer = GlUtil.createFloatBuffer(CoordinateUtils.TEXTURE_COORDINATE_NO_ROTATION);

        Matrix.setIdentityM(mIdentyMatrix,0);
        Matrix.setIdentityM(mMvpMatrix,0);
        Matrix.scaleM(mMvpMatrix,0,0.2f,0.2f,0.2f);
    }

    @Override
    public void initProgram(){
        mProgramId = GlUtil.createProgram(ShaderUtils.VERTEX_SHADER_SIMPLE, ShaderUtils.FRAGMENT_SHADER_SIMPLE);
    }

    private void initTexture(){
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bitmap);
        mTextureId = GlUtil.createTexture(bitmap);
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
    public int getTargetTextureId(){
        if (mFbo != null) {
            return mFbo.textureId();
        }
        return -1;
    }

    @Override
    public void onSurfaceCreate() {
        super.onSurfaceCreate();
        initTexture();
    }

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    @Override
    public void onSurfaceChanged(int width,int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        releaseFbo();
        initFbo(mSurfaceWidth,mSurfaceHeight);
    }

    @Override
    public void onInputSizeChanged(float width, float height,int rotation) {

    }

    public void drawFrame(int textureBgId){

    }

    @Override
    public void drawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer, int rotation) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFbo.frameId());

        GLES20.glUseProgram(mProgramId);

        int a_position = GLES20.glGetAttribLocation(mProgramId, "a_position");
        int a_textCoord = GLES20.glGetAttribLocation(mProgramId, "a_textCoord");
        int u_mvpMatrix = GLES20.glGetUniformLocation(mProgramId, "u_MVPMatrix");
        int sampleTexture = GLES20.glGetUniformLocation(mProgramId, "u_sampleTexture");

        GLES20.glEnableVertexAttribArray(a_position);
        GLES20.glEnableVertexAttribArray(a_textCoord);

        GLES20.glVertexAttribPointer(a_position,2,GLES20.GL_FLOAT,false,0,mVertexBuffer);
        GLES20.glVertexAttribPointer(a_textCoord,2,GLES20.GL_FLOAT,false,0,mTextureBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);
        GLES20.glUniformMatrix4fv(u_mvpMatrix,1,false,mIdentyMatrix,0);
        GLES20.glUniform1i(sampleTexture,1);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTextureId);
        GLES20.glUniformMatrix4fv(u_mvpMatrix,1,false,mMvpMatrix,0);
        GLES20.glUniform1i(sampleTexture,2);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

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
