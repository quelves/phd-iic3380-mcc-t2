package edu.puc.iic3380.mcc.t2;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;
import jwtc.chess.GameControl;
import jwtc.chess.JNI;

/**
 * Encapsulates any execution to be run either locally or
 * in the cloud.
 */
public class OffloadingCodeChessRunnable extends CloudRunnable {
    private static final String TAG = "ChessRunnable";
    /**
     * Code to be executed.
     *
     * @param params A set of parameters supplied by
     *               the developer.
     * @param lastState The last recorded state managed
     *                  to be sent by the cloud service.
     *                  This is used in case the code
     *                  execution is resumed locally after
     *                  being offloaded. This may happen
     *                  if the connection is interrupted
     *                  before obtaining the final
     *                  result. May be null.
     * @return A result encapsulated in a Params object.
     */


    protected GameControl m_control;

    public static final String KEY_LEVEL = "LEVEL";
    public static final String KEY_MOVE = "MOVE";

    @Override
    public Params execute(Params params,
                          Params lastState) {
        // Do stuff...

        log(TAG, "entrei");
        Params result = new Params();
        // Populate result object
        // (e.g: result.putString("key", "value");



        try {
            int level = params.getInt(KEY_LEVEL);
            log(TAG, "playerlev: " + level);


            if (m_control == null) {
                log(TAG, "creando GameControl");
                m_control = new GameControl();
                log(TAG, "creando GameControl");

            }

            JNI _jni = m_control.getJNI();
            log(TAG, "creando getJNI");
            if(_jni.isEnded() == 0) {
                log(TAG, "level: " + level);
                _jni.searchDepth(level);
                int move = _jni.getMove();
                log(TAG, "move: " + move);

                result.putInt(KEY_MOVE, move);

            }


        }
        catch (Exception e) {
            e.printStackTrace();

        }


        return result;
    }

    private void log(String tag, String message) {
        System.out.println(TAG + message);

    }
}
