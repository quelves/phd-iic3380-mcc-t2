package t2.mcc.iic3380.puc.edu.mcc_t2;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;

/**
 * Created by Pablo on 11/13/2016.
 */

public class FibonacciRunnable extends CloudRunnable {
    public static final String KEY_N = "KEY_N";
    public static final String RESULT_STATUS = "RESULT_STATUS";
    public static final String RESULT_DURATION = "RESULT_DURATION";
    @Override
    public Params execute(Params params, Params lastState) {
        int n = params.getInt(KEY_N);

        long startTime = System.currentTimeMillis();
        fibonnacci(n);
        long endTime = System.currentTimeMillis();

        int durationMs = (int)(endTime - startTime);

        Params result = new Params();

        result.putString(RESULT_STATUS, "SUCCESS");
        result.putInt(RESULT_DURATION, durationMs);
        return null;
    }

    private long fibonnacci(int n) {
        if (n == 0 || n == 1)
            return 1;
        int n0 = 0;
        int n1 = 1;
        int n2 = n0 + n1;
        for (int i = 3; i <= n; i++) {
            n0 = n1;
            n1 = n2;
            n2 = n0 + n1;
        }

        return n2;
    }
}
