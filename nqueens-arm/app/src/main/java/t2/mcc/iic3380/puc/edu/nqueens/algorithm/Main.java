package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

/**
 * Created by jose on 3/1/16.
 */
public class Main {
    public static void main(String[] args) {
        int queens = Integer.parseInt(args[0]);

        for (int i = 0; i < 20; i++) {
            long startTime = System.currentTimeMillis();
            NQueens nQueens = new NQueens(queens, false);
            nQueens.solve();
            long endTime = System.currentTimeMillis();

            int durationMs = (int) (endTime - startTime);

            System.out.println("Exec time: " + durationMs);
        }
    }
}
