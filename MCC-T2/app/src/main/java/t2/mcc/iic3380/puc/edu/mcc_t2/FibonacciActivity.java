package t2.mcc.iic3380.puc.edu.mcc_t2;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import edu.puc.astral.CloudManager;
import edu.puc.astral.CloudOperation;
import edu.puc.astral.CloudResultReceiver;
import edu.puc.astral.Params;

public class FibonacciActivity extends AppCompatActivity {
    private static final String LOG_TAG = FibonacciActivity.class.getSimpleName();
    private EditText nEditText;
    private long startTime;
    private Handler handler = new Handler();
    private Map<String, ResultHandler> handlers;

    private CloudResultReceiver receiver = new CloudResultReceiver() {
        @Override
        public void onReceiveResult(final String operationId, final Params result) {
            if (handlers.containsKey(operationId)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handlers.get(operationId).handleResult(result);
                        handlers.remove(operationId);
                    }
                });
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fibonacci);
        nEditText = (EditText)findViewById(R.id.n_edit_text);
        handlers = new HashMap<>();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Fibonacci");

    }


    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, FibonacciActivity.class);
        return intent;
    }

    @Override
    protected void onStart() {
        super.onStart();
        CloudManager.registerReceiver(this, receiver);
        CloudManager.unregisterReceiver(this, receiver);
    }

    public void doSomething(View view) {
        Params bundle = new Params();
        int n = Integer.parseInt(nEditText.getText().toString());
        bundle.putInt(FibonacciRunnable.KEY_N, n);
        startTime = System.currentTimeMillis();

        CloudOperation cloudOperation = new CloudOperation(this, FibonacciRunnable.class);

        cloudOperation.setParams(bundle);
        cloudOperation.setExecutionContext(CloudOperation.CONTEXT_CLOUD);
        cloudOperation.setExecutionStrategy(CloudOperation.STRATEGY_DEFAULT);
        handlers.put(cloudOperation.getOperationId(), new ResultHandler() {
            @Override
            public void handleResult(Params result) {
                long duration = System.currentTimeMillis() - startTime;
                int execDuration = result.getInt(FibonacciRunnable.RESULT_DURATION);
                String log = "Total time: " + duration + " - Exec time: " + execDuration;
                Log.i(LOG_TAG, log);
                Toast.makeText(FibonacciActivity.this, log, Toast.LENGTH_LONG).show();
            }
        });
        CloudManager.executeCloudOperation(this, cloudOperation);
    }

    private interface ResultHandler {
        void handleResult(Params result);
    }
}
