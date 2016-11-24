package t2.mcc.iic3380.puc.edu.vt;

import android.*;
import android.Manifest;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;

/**
 * Created by jose on 11/25/15.
 */
public class VideoTranscodingRunnable extends CloudRunnable {

    private static final String TAG = "VideoTranscodingRunnable";

    public static final String KEY_VIDEO = "MOVIE";
    public static final String FILE_NAME = "TRASCODED.MP4";

    public static final String KEY_RESULT = "RESULT";


    private static final int REQUEST_CODE_READ_PERMISSION = 1;
    private static final int REQUEST_CODE_WRITE_PERMISSION = 2;

    private static final int REQUEST_CODE_SELECT_VIDEO = 3;

    private File mVideoFileIn;
    private ImageView mVideoFrameHolder;

    private File outputFile;

    private boolean resultProcess;

    private boolean finalized = false;

    @Override
    public Params execute(Params params, Params lastState) {
        FileOutputStream fos = null;
        try {
            InputStream is = params.openFile(getContext(), KEY_VIDEO);
            mVideoFileIn = copyVideoToTempFile(is);

            transcode();
            while (!finalized) {
                System.out.println("waiting....");
                Thread.sleep(1000);
            }

            Params result = new Params();
            if (outputFile != null) {
                log(TAG, "OutputFile name : " + outputFile.getAbsolutePath());
                result.putFile(KEY_VIDEO, outputFile);
            }
            else {
                resultProcess = false;
            }
            result.putString(KEY_RESULT, String.valueOf(resultProcess));

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Transcodes a video into a .mp4 video file with 720p resolution.
     */
    private void transcode() {

        if (mVideoFileIn != null) {
            final long startTime = SystemClock.elapsedRealtime();
            outputFile = new File(MainApplication.getMainApplicationContext().getFilesDir(), getTranscodedVideoOutputFileName());
            File moviesDirectory = outputFile.getParentFile();
            if (!moviesDirectory.exists()) {
                moviesDirectory.mkdir();
            }
            log(TAG, "Work dir: " + MainApplication.getMainApplicationContext().getFilesDir().getAbsolutePath());
            log(TAG, "fileoutput: " + outputFile.getPath());

            String[] command = {"-y", "-i", "", "-s", "1280x720", ""};
            command[2] = mVideoFileIn.getAbsolutePath();
            command[5] = outputFile.getAbsolutePath();

            //Log.i(TAG, "Executing FFmpeg command: " + command);
            try {
                FFmpeg.getInstance(MainApplication.getMainApplicationContext()).execute(command, new FFmpegExecuteResponseHandler() {

                    @Override
                    public void onStart() {
                        log(TAG, "Transcoding started");

                    }

                    @Override
                    public void onFinish() {
                        long time = SystemClock.elapsedRealtime() - startTime;
                        log(TAG, "Transcoding finished. Operation took " + time + " ms.");
                        finalized = true;

                    }

                    @Override
                    public void onSuccess(String message) {
                        log(TAG, "Transcoding success: " + message);
                        onTranscodeFinished(true);

                    }

                    @Override
                    public void onProgress(String message) {
                        log(TAG, "Transcoding progress: " + message);
                    }

                    @Override
                    public void onFailure(String message) {
                        log(TAG, "Transcoding failure: " + message);
                        onTranscodeFinished(false);

                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {
                e.printStackTrace();
                resultProcess = false;
                finalized = true;
            }


        } else {
            log(TAG, "No video file selected.");
        }

    }

    private void onTranscodeFinished(boolean success) {

        resultProcess = success;

        finalized = true;

        if (success) {
            log(TAG, "Successfully transcoded video file.");
        } else {
            log(TAG, "Failed to transcode video file.");
        }
    }

    private String getTranscodedVideoOutputFileName() {
        String title = "vt_run";
        String extension = ".mp4";
        int counter = 1;

        File moviesDirectory = MainApplication.getMainApplicationContext().getFilesDir();
        File[] files = moviesDirectory.listFiles();
        if (files == null || !isNameContained(title + extension, files)) {
            return title + extension;
        } else {
            String newTitle;
            do {
                newTitle = title + " (" + counter + ")" + extension;
                counter++;
            } while (isNameContained(newTitle, files));

            return newTitle;
        }
    }

    private boolean isNameContained(String name, File[] files) {
        for (File file : files) {
            if (file.getName().equals(name)) return true;
        }
        return false;
    }

    public static File createOutputFile(InputStream in, String filenane) {
        final File file = new File(MainApplication.getMainApplicationContext().getFilesDir(), filenane);
        File moviesDirectory = file.getParentFile();
        if (!moviesDirectory.exists()) {
            moviesDirectory.mkdir();
        }
        try {
            OutputStream output = new FileOutputStream(file);
            try {
                try {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = in.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        System.out.print(".");
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace(); // handle exception, define IOException and others
            }

        } catch (Exception e) {

        } finally {
        }

        return file;


    }

    public FileDescriptor createFileAndGetFD(InputStream in, String filenane) {
        FileDescriptor result = null;
        final File file = new File(MainApplication.getMainApplicationContext().getFilesDir(), filenane);
        File moviesDirectory = file.getParentFile();
        if (!moviesDirectory.exists()) {
            moviesDirectory.mkdir();
        }
        try {
            FileOutputStream output = new FileOutputStream(file);
            try {
                try {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = in.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                        System.out.print(".");
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace(); // handle exception, define IOException and others
            }
            result = output.getFD();
        } catch (Exception e) {

        } finally {
        }

        return result;


    }

    private void log(String tag, String message) {
        System.out.println(TAG + message);

    }

    private File copyVideoToTempFile(InputStream is) {
        File tempFile = new File(MainApplication.getMainApplicationContext().getFilesDir(), "temp_run.webm");
        try {
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.close();

            mVideoFileIn = tempFile;
            return tempFile;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
