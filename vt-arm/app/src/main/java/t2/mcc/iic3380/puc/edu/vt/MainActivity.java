package t2.mcc.iic3380.puc.edu.vt;


import android.Manifest.permission;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.firebase.storage.FileDownloadTask;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.puc.astral.CloudManager;
import edu.puc.astral.CloudOperation;
import edu.puc.astral.CloudResultReceiver;
import edu.puc.astral.Params;

import static android.R.id.input;
import static android.R.id.message;

public class MainActivity extends Activity {
    private static final String TAG = "TranscoderActivity";
    private static final int REQUEST_CODE_READ_PERMISSION = 1;
    private static final int REQUEST_CODE_WRITE_PERMISSION = 2;

    private static final int REQUEST_CODE_SELECT_VIDEO = 3;

    private FileDescriptor mVideoFileDescriptor;
    private ImageView mVideoFrameHolder;

    private ProgressDialog mProgressDialog;

    private TextView tvResult;

    private long mStartTime;

    private Handler mHandler = new Handler();
    private File mVideoFileIn;

    private Map<String, ResultHandler> mResultHandlers;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultHandlers = new HashMap<>();

        mVideoFrameHolder = (ImageView) findViewById(R.id.img_video_frame);
        tvResult = (TextView)findViewById(R.id.tvResult);

        findViewById(R.id.btn_select_video).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectVideo();
            }
        });
        findViewById(R.id.btn_transcode_local_local).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                transcode();
            }
        });
        findViewById(R.id.btn_transcode_local).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                transcode(CloudOperation.CONTEXT_LOCAL);
            }
        });
        findViewById(R.id.btn_transcode_cloud).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                transcode(CloudOperation.CONTEXT_CLOUD);
            }
        });
        findViewById(R.id.btn_default_video).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDefaultVideo();
            }
        });

        try {
            FFmpeg.getInstance(this).loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.i(TAG, "Failed to load FFmpeg");
                }

                @Override
                public void onSuccess() {
                    Log.i(TAG, "Successfully loaded FFmpeg");
                }

                @Override
                public void onStart() {
                    Log.i(TAG, "FFmpeg started");
                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "FFmpeg finished");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        CloudManager.registerReceiver(this, mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        CloudManager.unregisterReceiver(this, mReceiver);
    }

    private void loadDefaultVideo() {
        try {
            InputStream is = getAssets().open("video.webm");
            File video = copyVideoToTempFile(is);
            is.close();

            if (video != null) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(video.getAbsolutePath());

                Bitmap firstFrame = retriever.getFrameAtTime(3000000, MediaMetadataRetriever.OPTION_CLOSEST);
                retriever.release();

                mVideoFrameHolder.setImageBitmap(firstFrame);

                Log.i(TAG, "Default video loaded");
            } else {
                Log.i(TAG, "Failed to load default video");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File copyVideoToTempFile(InputStream is) {
        File tempFile = new File(MainApplication.getMainApplicationContext().getFilesDir(), "temp.webm");
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


    /**
     * Transcodes a video into a .mp4 video file with 720p resolution.
     *
     */
    private void transcode() {
        if (ContextCompat.checkSelfPermission(
                this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_PERMISSION);
        } else {
            if (mVideoFileIn != null) {
                final long startTime = SystemClock.elapsedRealtime();
                final File outputFile = new File(MainApplication.getMainApplicationContext().getFilesDir(), getTranscodedVideoOutputFileName());
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
                    FFmpeg.getInstance(MainActivity.this).execute(command, new FFmpegExecuteResponseHandler() {

                        @Override
                        public void onStart() {
                            log(TAG, "Transcoding started");

                        }

                        @Override
                        public void onFinish() {
                            long time = SystemClock.elapsedRealtime() - startTime;
                            log(TAG, "Transcoding finished. Operation took " + time + " ms.");

                        }

                        @Override
                        public void onSuccess(String message) {
                            log(TAG, "Transcoding success: " + message);
                            mProgressDialog.dismiss();
                        }

                        @Override
                        public void onProgress(String message) {
                            log(TAG, "Transcoding progress: " + message);
                        }

                        @Override
                        public void onFailure(String message) {
                            log(TAG, "Transcoding failure: " + message);
                            mProgressDialog.dismiss();
                        }
                    });
                } catch (FFmpegCommandAlreadyRunningException e) {
                    e.printStackTrace();
                }

                mProgressDialog = ProgressDialog.show(this, "Please wait", "Transcoding in progress...", true);


            } else {
                Toast.makeText(this, "No video file selected.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Transcodes a video into a .mp4 video file with 720p resolution.
     *
     */
    private void transcode(@CloudOperation.ExecutionContext int executionContext) {
        if (ContextCompat.checkSelfPermission(
                this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_PERMISSION);
        }
        else {
            if (mVideoFileIn != null) {
                try {
                    Params params = new Params();
                    params.putFile(VideoTranscodingRunnable.KEY_VIDEO, mVideoFileIn);

                    CloudOperation operation = new CloudOperation(this, VideoTranscodingRunnable.class);
                    operation.setParams(params);
                    operation.setExecutionContext(executionContext);
                    mResultHandlers.put(operation.getOperationId(), new ResultHandler() {
                        @Override
                        public void handleResult(Params result) {
                            try {
                                log(TAG, "Callback of runner!");
                                if (result != null) {
                                    boolean resultProcess = Boolean.valueOf(result.getString(VideoTranscodingRunnable.KEY_RESULT));
                                    if (resultProcess) {
                                        File file = VideoTranscodingRunnable.createOutputFile(result.openFile(MainActivity.this, VideoTranscodingRunnable.KEY_VIDEO), getTranscodedVideoOutputFileName());
                                    }
                                    onTranscodeFinished(resultProcess);
                                }
                                else {
                                    log(TAG, "Error en processamiento");
                                    onTranscodeFinished(false);
                                }
                                if (mProgressDialog != null) {
                                    mProgressDialog.dismiss();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                onTranscodeFinished(false);
                            }
                        }
                    });
                    CloudManager.executeCloudOperation(this, operation);
                    mProgressDialog = ProgressDialog.show(this, "Please wait", "Transcoding in progress...", true);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                }

            } else {
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private CloudResultReceiver mReceiver = new CloudResultReceiver() {
        @Override
        public void onReceiveResult(final String operationId, final Params result) {
            if (mResultHandlers.containsKey(operationId)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mResultHandlers.get(operationId).handleResult(result);
                        mResultHandlers.remove(operationId);
                    }
                });

            }
        }
    };

    private void selectVideo() {
        if (ContextCompat.checkSelfPermission(
                this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_PERMISSION);
        } else {
            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("video/mp4");

            startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SELECT_VIDEO: {
                if (resultCode == RESULT_OK) {
                    mVideoFileIn = uriToFile(data.getData());
                    Log.d(TAG, "File: " + mVideoFileIn.getPath());


                    //Get first image and set video attribute

                    ContentResolver resolver = getContentResolver();
                    final ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = resolver.openFileDescriptor(Uri.fromFile(mVideoFileIn), "r");

                    } catch (FileNotFoundException e) {
                        Log.w("Could not open '" + data.getDataString() + "'", e);
                        Toast.makeText(MainActivity.this, "File not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mVideoFileDescriptor = parcelFileDescriptor.getFileDescriptor();


                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mVideoFileDescriptor);

                    Bitmap firstFrame = retriever.getFrameAtTime(3000000, MediaMetadataRetriever.OPTION_CLOSEST);
                    retriever.release();
                    mVideoFrameHolder.setImageBitmap(firstFrame);
                }
                break;
            }
            case REQUEST_CODE_READ_PERMISSION: {
                if (resultCode == RESULT_OK) {
                    selectVideo();
                }
                break;
            }
            case REQUEST_CODE_WRITE_PERMISSION: {
                if (resultCode == RESULT_OK) {
                    transcode();
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void createOutputFile(InputStream in) {
        final File file = new File(MainApplication.getMainApplicationContext().getFilesDir(), getTranscodedVideoOutputFileName());
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
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace(); // handle exception, define IOException and others
            }
        }
        catch (Exception e) {

        } finally {
        }


    }

    private File uriToFile(Uri uri) {
        final File file = new File(MainApplication.getMainApplicationContext().getFilesDir(), getPreTranscodedVideoOutputFileName());
        File moviesDirectory = file.getParentFile();
        if (!moviesDirectory.exists()) {
            moviesDirectory.mkdir();
        }
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            OutputStream output = new FileOutputStream(file);
            try {
                try {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = in.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e1) {
                e1.printStackTrace(); // handle exception, define IOException and others
            }

        } catch (Exception e2) {

            e2.printStackTrace();
        }
        return file;
    }


    private interface ResultHandler {
        void handleResult(Params result);
    }

    private void onTranscodeFinished(boolean success) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (success) {
            Toast.makeText(MainActivity.this, "Successfully transcoded video file.", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(MainActivity.this, "Failed to transcode video file.", Toast.LENGTH_LONG).show();
            tvResult.setText("Failed to transcode video file.");
        }
    }

    private String getTranscodedVideoOutputFileName() {
        String title = "transcoded_file";
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

    private String getPreTranscodedVideoOutputFileName() {
        String title = "pre_transcoded_file";
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


    //remove
    private void storeVideoTranscoded(File localFile)  {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(localFile);
        scanIntent.setData(contentUri);
        sendBroadcast(scanIntent);
    }


    private void log(String tag, String message) {
        Log.i(TAG, message);
        tvResult.setText(TAG + ":" + message);

    }



}
