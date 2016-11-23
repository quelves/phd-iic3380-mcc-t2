package jwtc.android.chess;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import edu.puc.astral.CloudManager;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app, such as
 * the {@link Tracker}.
 */
public class MyApplication extends MultiDexApplication {
    private Tracker mTracker;

    private static String HOST_IP = "http://35.163.228.128";
    private static String SENDER_ID = "413368182491";

    private static MyApplication instance;

    public MyApplication() {
        super();
        instance = this;

    }

    @Override
    public void onCreate() {
        super.onCreate();

       CloudManager.initialize(this, SENDER_ID, HOST_IP);
    }

    public static void setHostIP(String host) {
        HOST_IP = host;
    }

    public static String getHostIP() {
        return HOST_IP;
    }

    public static void setSenderId(String senderId) {
        SENDER_ID = senderId;
    }

    public static String getSenderId() {
        return SENDER_ID;
    }

    public static Context getMainApplicationContext() {
        return instance;
    }

    public static ContentResolver getMainApplicationContentResolver() {
        return instance.getContentResolver();
    }


    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.tracker_config);

            //GoogeAnalytics.getInstance(this).setDryRun(true);
        }
        return mTracker;
    }
}