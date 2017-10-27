package com.duan.drawer;

import java.nio.FloatBuffer;

/**
 * Created by duanyy on 2017/10/25.
 */

public abstract class Drawer {

    protected abstract void init();
    protected abstract void initProgram();
    public abstract int getTargetTextureId();
    public void onSurfaceCreate(){
        initProgram();
    }
    public abstract void onSurfaceChanged(int width,int height);
    public abstract void onInputSizeChanged(float width,float height,int rotation);
    public abstract void drawFrame(int textureId);
    public abstract void drawFrame(int textureId, FloatBuffer vertexBuffer,FloatBuffer textureBuffer,int rotation);
    public abstract void release();

}
