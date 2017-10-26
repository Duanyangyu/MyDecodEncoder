package com.duan.decoder;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.duan.R;
import com.duan.Utils.PermissionHelper;

import java.io.IOException;

public class VideoPlayActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "VideoPlayActivity";

    private TextView mTvFilePath;
    private VideoPlayerView mVideoPlayView;
    private boolean mPauseVideo;
    private Button mBtnPause;
    private String[] mAssetFileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_play);
        mTvFilePath = findViewById(R.id.mTvFilePath);
        mVideoPlayView = findViewById(R.id.mVideoPlayerView);
        findViewById(R.id.mBtnPlay).setOnClickListener(this);
        findViewById(R.id.mBtnEncoder).setOnClickListener(this);
        mBtnPause = findViewById(R.id.mBtnPause);
        mBtnPause.setOnClickListener(this);

        checkPermission();
        initFile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoPlayView.pauseVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoPlayView.resumeVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoPlayView != null) {
            mVideoPlayView.destroy();
        }
    }

    private void initFile(){
        try {
            mAssetFileList = getAssets().list("video");
            for (String path : mAssetFileList){
                Log.e(TAG,"initFile path="+path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.mBtnPlay:
                String path = getFilePath();
                mTvFilePath.setText(path);
                mVideoPlayView.setDataSource(path);
                mVideoPlayView.playVideo();
                break;
            case R.id.mBtnPause:
                pauseVideo();
                break;
            case R.id.mBtnEncoder:
                mVideoPlayView.reEncode();
                break;
            default:
                break;
        }
    }

    private int mFileIndex;
    private String getFilePath(){
        if (mAssetFileList != null && mAssetFileList.length > 0){
            String path = mAssetFileList[mFileIndex%mAssetFileList.length];
            mFileIndex++;
            return path;
        }else {
            return null;
        }
    }

    private void pauseVideo(){
        if (mPauseVideo){
            mVideoPlayView.resumeVideo();
            mBtnPause.setText("Pause");
        }else {
            mVideoPlayView.pauseVideo();
            mBtnPause.setText("Resume");
        }
        mPauseVideo = !mPauseVideo;
    }

    private void checkPermission(){
        String[] permiss = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        PermissionHelper helper = new PermissionHelper(this);
        boolean result = helper.checkPermissions(permiss);
        if (!result){
            ActivityCompat.requestPermissions(this,permiss,100);
        }
    }
}
