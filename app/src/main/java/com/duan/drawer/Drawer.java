package com.duan.drawer;

/**
 * Created by duanyy on 2017/10/25.
 */

public abstract class Drawer {

    protected abstract void init();
    public abstract int getTargetTextureId();
    public abstract void onSurfaceCreate();
    public abstract void onSurfaceChanged(int width,int height);
    public abstract void drawFrame(int textureId);
    public abstract void release();

}
