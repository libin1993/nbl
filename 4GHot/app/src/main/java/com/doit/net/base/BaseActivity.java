package com.doit.net.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Author：Libin on 2020/5/11 11:36
 * Email：1993911441@qq.com
 * Describe：Activity基类
 */
public class BaseActivity extends AppCompatActivity {
    private static List<Activity> activityList = new ArrayList<>();

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        activityList.add(this);
    }


    /**
     * @return 字体大小固定
     */
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }

    @Override
    protected void onDestroy() {
        activityList.remove(this);
        super.onDestroy();
    }


    public static void exitApp(){
        for (Activity activity : activityList) {
            if (activity !=null && activity.isFinishing()){
                activity.finish();
            }
        }
        System.exit(0);
    }
}
