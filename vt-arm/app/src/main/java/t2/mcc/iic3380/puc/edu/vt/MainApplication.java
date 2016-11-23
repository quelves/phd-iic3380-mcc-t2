package t2.mcc.iic3380.puc.edu.vt;

import android.content.ContentResolver;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import edu.puc.astral.CloudManager;

/**
 * Created by jose on 11/17/15.
 */
public class MainApplication extends MultiDexApplication {
    private static String HOST_IP = "http://35.163.228.128";
    private static String SENDER_ID = "413368182491";

    private static MainApplication instance;

    public MainApplication() {
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


}
