package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import edu.puc.astral.CloudManager;
import edu.puc.astral.CloudOperation;
import edu.puc.astral.CloudOperation.ExecutionContext;
import edu.puc.astral.CloudOperation.ExecutionStrategy;
import edu.puc.astral.CloudResultReceiver;
import edu.puc.astral.Params;
import t2.mcc.iic3380.puc.edu.nqueens.R;

public class MainActivity extends Activity {
    private static final int CODE_IMAGE_PICKER = 1000;
    private static final String TAG = "NQueens";
    private long mStartTime;

    private Handler mHandler = new Handler();
    private File mImageFile;

    private Map<String, ResultHandler> mResultHandlers;
    private ImageView mImageView;

    private int mCounter = 0;
    private volatile int mBarrier = 0;
    private TextView tvTime;

    /*
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    long duration = System.currentTimeMillis() - mStartTime;
                    Params result = intent.getParcelableExtra(CloudService.EXTRA_RESULT);
                    int execDuration = result.getInt(NQueensRunnable.RESULT_DURATION);
                    Toast.makeText(MainActivity.this, "Total time: " + duration + " - Exec time: " + execDuration, Toast.LENGTH_LONG).show();
                }
            });
        }
    };
    */

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mResultHandlers = new HashMap<>();

        final EditText editQueens = (EditText) findViewById(R.id.edit_queens);

        tvTime = (TextView) findViewById(R.id.tvTime);

        Button btnSolveLocally = (Button) findViewById(R.id.btn_solve_local);
        btnSolveLocally.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tvTime.setText("Processing...");
                    int queens = Integer.parseInt(editQueens.getText().toString());
                    computeNQueens(queens, CloudOperation.CONTEXT_LOCAL, CloudOperation.STRATEGY_DEFAULT);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_LONG).show();
                    tvTime.setText("Invalid input");
                }
            }
        });

        Button btnSolveCloud = (Button) findViewById(R.id.btn_solve_cloud);
        btnSolveCloud.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tvTime.setText("Processing...");
                    int queens = Integer.parseInt(editQueens.getText().toString());
                    computeNQueens(queens, CloudOperation.CONTEXT_CLOUD, CloudOperation.STRATEGY_DEFAULT);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_LONG).show();
                    tvTime.setText("Invalid input");
                }
            }
        });

        Button btnSolveOptimistic = (Button) findViewById(R.id.btn_solve_optimistic);
        btnSolveOptimistic.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tvTime.setText("Processing...");
                    int queens = Integer.parseInt(editQueens.getText().toString());
                    computeNQueens(queens, CloudOperation.CONTEXT_DEFAULT, CloudOperation.STRATEGY_DEFAULT);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_LONG).show();
                    tvTime.setText("Invalid input");
                }
            }
        });

        Button btnSolveConcurrently = (Button) findViewById(R.id.btn_solve_concurrent);
        btnSolveConcurrently.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    tvTime.setText("Processing...");
                    int queens = Integer.parseInt(editQueens.getText().toString());
                    computeNQueens(queens, CloudOperation.CONTEXT_DEFAULT, CloudOperation.STRATEGY_CONCURRENT);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid input", Toast.LENGTH_LONG).show();
                    tvTime.setText("Invalid input");
                }
            }
        });

        Button btnImagePicker = (Button) findViewById(R.id.btn_image_picker);
        btnImagePicker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, CODE_IMAGE_PICKER);
            }
        });

        Button btnSolveImageLocally = (Button) findViewById(R.id.btn_image_solve_local);
        btnSolveImageLocally.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                computeImage(CloudOperation.CONTEXT_LOCAL);
            }
        });


        Button btnSolveImageCloud = (Button) findViewById(R.id.btn_image_solve_cloud);
        btnSolveImageCloud.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                computeImage(CloudOperation.CONTEXT_CLOUD);
            }
        });

        mImageView = (ImageView) findViewById(R.id.img_result);

        writeTestFile();

    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.rbARM:
                if (checked) {
                    MainApplication.setHostIP("http://35.163.228.128");
                    MainApplication.setSenderId("413368182491");
                    break;
                }
            case R.id.rbx86:
                if (checked) {
                    MainApplication.setHostIP("http://35.163.222.118");
                    MainApplication.setSenderId("460750150714");
                    break;
                }
        }

    }


    private void writeTestFile() {
        File testFile = new File(getFilesDir(), "archivo.txt");
        if (!testFile.exists()) {
            try {
                FileWriter fw = new FileWriter(testFile);
                fw.write("hola");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_IMAGE_PICKER && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap image = BitmapFactory.decodeStream(imageStream);

                File tempFile = new File(getFilesDir(), "temp.jpg");
                if (!tempFile.exists()) {
                    tempFile.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                image.compress(CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();

                mImageFile = tempFile;
            } catch (Exception e) {
                Toast.makeText(this, "Error retrieving image.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void computeNQueens(final int queens,
                                @ExecutionContext final int executionContext,
                                @ExecutionStrategy final int executionStrategy) {
        mStartTime = System.currentTimeMillis();

        Params bundle = new Params();
        bundle.putInt(NQueensRunnable.KEY_QUEENS, queens);

        CloudOperation operation = new CloudOperation(this, NQueensRunnable.class);
        operation.setParams(bundle);
        operation.setExecutionContext(executionContext);
        operation.setExecutionStrategy(executionStrategy);
        mResultHandlers.put(operation.getOperationId(), new ResultHandler() {
            @Override
            public void handleResult(Params result) {
                long duration = System.currentTimeMillis() - mStartTime;
                int execDuration = result.getInt(NQueensRunnable.RESULT_DURATION);
                String log = "Host: " + MainApplication.getHostIP() + " Type: " + executionStrategy + " Total time: " + duration + " - Exec time: " + execDuration;
                Log.i(TAG, log);
                Toast.makeText(MainActivity.this, log, Toast.LENGTH_LONG).show();
                tvTime.setText(log);

            }
        });
        CloudManager.executeCloudOperation(this, operation);

        /*
        final int n = queens;
        for (int i = 0; i < n; i++) {
            Params bundle = new Params();
            //bundle.putInt(NQueensRunnable.KEY_QUEENS, queens);
            bundle.putInt(NQueensRunnable.KEY_QUEENS, 14);

            CloudOperation operation = new CloudOperation(this, NQueensRunnable.class);
            operation.setParams(bundle);
            operation.setExecutionContext(executionContext);
            operation.setExecutionStrategy(executionStrategy);
            mResultHandlers.put(operation.getOperationId(), new ResultHandler() {
                @Override
                public void handleResult(Params result) {
                    mBarrier++;
                    if (mBarrier == n) {
                        long duration = System.currentTimeMillis() - mStartTime;
                        int execDuration = result.getInt(NQueensRunnable.RESULT_DURATION);
                        String log = "Total time: " + duration + " - Exec time: " + execDuration;
                        Log.i(TAG, log);
                        Toast.makeText(MainActivity.this, log, Toast.LENGTH_LONG).show();
                        mBarrier = 0;


                        mCounter++;
                        if (mCounter < 5) {
                            computeNQueens(queens, executionContext, executionStrategy);
                        } else {
                            mCounter = 0;
                        }
                    }
                }
            });
            CloudManager.executeCloudOperation(this, operation);
        }
        */


    }

    private void computeImage(@ExecutionContext int executionContext) {
        if (mImageFile != null) {
            try {
                Params params = new Params();
                params.putFile(ImageRunnable.KEY_IMAGE, mImageFile);

                CloudOperation operation = new CloudOperation(this, ImageRunnable.class);
                operation.setParams(params);
                operation.setExecutionContext(executionContext);
                mResultHandlers.put(operation.getOperationId(), new ResultHandler() {
                    @Override
                    public void handleResult(Params result) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(result.openFile(MainActivity.this, ImageRunnable.KEY_IMAGE));
                            mImageView.setImageBitmap(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                CloudManager.executeCloudOperation(this, operation);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, "Please select an image first.", Toast.LENGTH_LONG).show();
        }
    }

    private interface ResultHandler {
        void handleResult(Params result);
    }

    private void exportDB() {
        File dbFile =
                new File(Environment.getDataDirectory() + "/data/edu.puc.offloadingtest.app/databases/ancome.db");

        File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, dbFile.getName());
        try {
            file.createNewFile();
            this.copyFile(dbFile, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }
}
