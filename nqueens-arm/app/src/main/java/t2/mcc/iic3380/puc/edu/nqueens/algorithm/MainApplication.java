package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import edu.puc.astral.CloudManager;

/**
 * Created by jose on 11/17/15.
 */
public class MainApplication extends MultiDexApplication {
    private static String HOST_IP = "http://35.163.228.128";
    private static String SENDER_ID = "413368182491";

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


}
