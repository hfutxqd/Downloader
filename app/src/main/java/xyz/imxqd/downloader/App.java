package xyz.imxqd.downloader;

import android.app.Application;

import com.orm.SugarContext;

/**
 * Created by imxqd on 2016/7/4.
 * Downloader的Application类
 */
public class App extends Application{
    private static App app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        SugarContext.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }

    public static App getApp() {
        return app;
    }
}
