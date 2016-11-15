package t2.mcc.iic3380.puc.edu.mcc_t2;

import android.app.Application;

import edu.puc.astral.CloudManager;

/**
 * Created by quelves on 11/15/16.
 */

public class MainApplication extends Application {
    private static final String HOST_IP = "http://35.163.228.128";
    private static final String SENDER_ID = "460750150714";

    @Override
    public void onCreate() {
        super.onCreate();

        CloudManager.initialize(this, SENDER_ID, HOST_IP);
    }
}



