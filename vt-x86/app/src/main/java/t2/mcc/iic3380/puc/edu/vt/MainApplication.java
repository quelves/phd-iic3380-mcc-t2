package t2.mcc.iic3380.puc.edu.vt;

import android.content.ContentResolver;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import edu.puc.astral.CloudManager;

/**
 * Created by jose on 11/17/15.
 */
public class MainApplication extends MultiDexApplication {
    private static String HOST_IP = "http://35.163.222.118";
    private static String SENDER_ID = "948048471281";

    private static final String TAG = "MainApplication";

    private static MainApplication instance;

    public MainApplication() {
        super();
        instance = this;

    }

    @Override
    public void onCreate() {
        super.onCreate();


        CloudManager.initialize(this, SENDER_ID, HOST_IP);

        try {
            FFmpeg.getInstance(MainApplication.getMainApplicationContext()).loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    log(TAG, "Failed to load FFmpeg");
                }

                @Override
                public void onSuccess() {
                    log(TAG, "Successfully loaded FFmpeg");
                }

                @Override
                public void onStart() {
                    log(TAG, "FFmpeg started");
                }

                @Override
                public void onFinish() {
                    log(TAG, "FFmpeg finished");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
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

    private void log(String tag, String message) {
        System.out.println(TAG + message);

    }
}
