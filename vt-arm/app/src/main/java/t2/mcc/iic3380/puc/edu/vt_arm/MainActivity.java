package t2.mcc.iic3380.puc.edu.vt_arm;

import android.Manifest.permission;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "TranscoderActivity";
    private static final int REQUEST_CODE_READ_PERMISSION = 1;
    private static final int REQUEST_CODE_WRITE_PERMISSION = 2;

    private static final int REQUEST_CODE_SELECT_VIDEO = 3;

    private FileDescriptor mVideoFile;
    private ImageView mVideoFrameHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVideoFrameHolder = (ImageView) findViewById(R.id.img_video_frame);
        findViewById(R.id.btn_select_video).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectVideo();
            }
        });
        findViewById(R.id.btn_transcode).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                transcode();
            }
        });
    }

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
            if (mVideoFile != null) {
                final long startTime = SystemClock.uptimeMillis();
                final MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                    @Override
                    public void onTranscodeProgress(double progress) {
                        Log.i(TAG, "Progress: " + progress);
                    }

                    @Override
                    public void onTranscodeCompleted() {
                        Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                        onTranscodeFinished(true);
                    }

                    @Override
                    public void onTranscodeCanceled() {
                        onTranscodeFinished(false);
                    }

                    @Override
                    public void onTranscodeFailed(Exception exception) {
                        onTranscodeFinished(false);
                    }
                };

                final File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), getTranscodedVideoOutputFileName());
                File moviesDirectory = outputFile.getParentFile();
                if (!moviesDirectory.exists()) {
                    moviesDirectory.mkdir();
                }
                try {
                    outputFile.createNewFile();
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            MediaTranscoder.getInstance().transcodeVideo(mVideoFile, outputFile.getAbsolutePath(),
                                    MediaFormatStrategyPresets.createAndroid720pStrategy(), listener);
                        }
                    };
                    thread.start();

                } catch (IOException e) {
                    e.printStackTrace();

                }
            } else {
                Toast.makeText(this, "No video file selected.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SELECT_VIDEO: {
                if (resultCode == RESULT_OK) {
                    ContentResolver resolver = getContentResolver();
                    final ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
                    } catch (FileNotFoundException e) {
                        Log.w("Could not open '" + data.getDataString() + "'", e);
                        Toast.makeText(MainActivity.this, "File not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    mVideoFile = parcelFileDescriptor.getFileDescriptor();

                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mVideoFile);

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

    private void onTranscodeFinished(boolean success) {
        if (success) {
            Toast.makeText(MainActivity.this, "Successfully transcoded video file.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed to transcode video file.", Toast.LENGTH_LONG).show();
        }
    }

    private String getTranscodedVideoOutputFileName() {
        String title = "transcoded_file";
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
            } while (!isNameContained(newTitle, files));

            return newTitle;
        }
    }

    private boolean isNameContained(String name, File[] files) {
        for (File file : files) {
            if (file.getName().equals(name)) return true;
        }
        return false;
    }
}

