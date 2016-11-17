package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

import android.app.Application;

import edu.puc.astral.CloudManager;

/**
 * Created by jose on 11/17/15.
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
