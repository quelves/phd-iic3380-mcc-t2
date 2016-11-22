package t2.mcc.iic3380.puc.edu.vt.algorithm;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;

/**
 * Created by jose on 11/5/15.
 */
public class NQueensRunnable extends CloudRunnable {
    public static final String KEY_QUEENS = "queens";
    public static final String RESULT_STATUS = "status";
    public static final String RESULT_DURATION = "duration";

    @Override
    public Params execute(Params params, Params lastState) {
        int queens = params.getInt(KEY_QUEENS);

        long startTime = System.currentTimeMillis();
        NQueens nQueens = new NQueens(queens, false);
        nQueens.solve();
        long endTime = System.currentTimeMillis();

        int durationMs = (int) (endTime - startTime);

        Params result = new Params();
        result.putString(RESULT_STATUS, "SUCCESS");
        result.putInt(RESULT_DURATION, durationMs);
        return result;
    }
}
