package com.ott.hunterse;

import android.app.Application;

import com.ott.hunterse.contentprovider.SPHelper;

/**
 * Application类
 * Created at 2022-06-13 0015 13:27:17
 *
 * @author LiYuliang
 * @version 1.0
 */
public class HunterSeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化SPHelper
        SPHelper.init(this);
    }

}
