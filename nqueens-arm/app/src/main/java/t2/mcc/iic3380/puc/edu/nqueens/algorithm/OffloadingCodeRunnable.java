package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;

/**
 * Encapsulates any execution to be run either locally or
 * in the cloud.
 */
public class OffloadingCodeRunnable
        extends CloudRunnable {

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
    @Override
    public Params execute(Params params,
                          Params lastState) {
        // Do stuff...

        Params result = new Params();
        // Populate result object
        // (e.g: result.putString("key", "value");
        return result;
    }
}
