package com.duan.Utils;

import android.util.Log;

import java.io.File;

/**
 * Created by duanyy on 2017/10/23.
 */

public class FileUtils {

    public static final String TAG = "FileUtils";

    public static boolean checkFile(String path){
        File file = new File(path);
        if (!file.exists()){
            return false;
        }
        if (file.length() == 0){
            return false;
        }
        return true;
    }

}
