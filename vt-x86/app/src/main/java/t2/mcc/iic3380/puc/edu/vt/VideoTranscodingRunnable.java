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

    private FileDescriptor mVideoFileDescriptor;
    private ImageView mVideoFrameHolder;

    private File outputFile;

    private boolean result;

    private boolean finalized = false;

    @Override
    public Params execute(Params params, Params lastState) {
        FileOutputStream fos = null;
        try {
            InputStream is = params.openFile(getContext(), KEY_VIDEO);

            File file = createOutputFile(is, FILE_NAME);
            ContentResolver resolver = MainApplication.getMainApplicationContentResolver();
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor = resolver.openFileDescriptor(Uri.fromFile(file), "r");

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mVideoFileDescriptor = parcelFileDescriptor.getFileDescriptor();


            transcode();

            while (!finalized) {
                System.out.println("waiting....");
                Thread.sleep(100);

            }

            Params result = new Params();
            result.putFile(KEY_VIDEO, outputFile);
            result.putString(KEY_RESULT, String.valueOf(result));


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

    private void transcode() {

        if (mVideoFileDescriptor != null) {
            final long startTime = SystemClock.uptimeMillis();
            final MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                @Override
                public void onTranscodeProgress(double progress) {
                    System.out.println(TAG + "Progress: " + progress);
                }

                @Override
                public void onTranscodeCompleted() {
                    System.out.println(TAG + "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                    onTranscodeFinished(true);
                }

                @Override
                public void onTranscodeCanceled() {
                    System.out.println(TAG + "trancoding canceled");
                    onTranscodeFinished(false);
                }

                @Override
                public void onTranscodeFailed(Exception exception) {
                    onTranscodeFinished(false);
                }
            };

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mVideoFileDescriptor);

            Bitmap firstFrame = retriever.getFrameAtTime(3000000, MediaMetadataRetriever.OPTION_CLOSEST);
            retriever.release();

            outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), getTranscodedVideoOutputFileName());
            File moviesDirectory = outputFile.getParentFile();
            if (!moviesDirectory.exists()) {
                moviesDirectory.mkdir();
            }
            try {
                outputFile.createNewFile();

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        MediaTranscoder.getInstance().transcodeVideo(mVideoFileDescriptor, outputFile.getAbsolutePath(),
                                MediaFormatStrategyPresets.createAndroid720pStrategy(), listener);
                    }
                };
                thread.start();




            } catch (IOException e) {
                e.printStackTrace();

            }
        } else {
            System.out.println(TAG + "No video file selected.");
        }

    }

    private void onTranscodeFinished(boolean success) {

        result = success;

        finalized = true;

        if (success) {
            System.out.println(TAG + "Successfully transcoded video file.");
        } else {
            System.out.println(TAG + "Failed to transcode video file.");
        }
    }

    private String getTranscodedVideoOutputFileName() {
        String title = "vt_run";
        String extension = ".mp4";
        int counter = 1;

        File moviesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
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
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), filenane);
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
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), filenane);
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


}
