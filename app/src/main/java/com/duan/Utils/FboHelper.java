package com.duan.Utils;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.IntBuffer;

public class FboHelper {
	private static final String TAG = "FboHelper TAG";
	public IntBuffer frameBuffer = IntBuffer.allocate(1);
	public IntBuffer textureBuffer = IntBuffer.allocate(1);

	public int width;
	public int height;

	public FboHelper(int w, int h) {
		width = w;
		height = h;
	}

	public int frameId() {
		return frameBuffer.get(0);
	}

	public int textureId() {
		return textureBuffer.get(0);
	}
	
	public void createFbo() {
		frameBuffer.position(0);
		GLES20.glGenFramebuffers(1, frameBuffer);
//		GlUtil.checkGlError("glGenFramebuffers");
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer.get(0));
//		GlUtil.checkGlError("glBindFramebuffer");
		CreateFboTexture(width, height, textureBuffer);
//		GlUtil.checkGlError("CreateFboTexture");
		if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			Log.e(TAG, "Failure with framebuffer generation");
		}

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	public void CreateFboTexture(int w, int h, IntBuffer texture) {
		texture.position(0);
		GLES20.glGenTextures(1, texture);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.get(0));
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
				GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
				GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
				texture.get(0), 0);
	}

	public void close() {
		if (frameBuffer != null) {
			frameBuffer.position(0);
			GLES20.glDeleteFramebuffers(1, frameBuffer);
			frameBuffer.clear();
		}

		if (textureBuffer != null) {
			textureBuffer.position(0);
			GLES20.glDeleteTextures(1, textureBuffer);
			textureBuffer.clear();
		}
	}
	
	 private void checkGlError(String op) {
	        int error;
	        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
	            Log.e(TAG, op + ": glError " + error);
	            throw new RuntimeException(op + ": glError " + error);
	        }
	    }
}