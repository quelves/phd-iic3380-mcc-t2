package t2.mcc.iic3380.puc.edu.vt.algorithm;

import android.util.Log;

/**
 * NQueens algorithm implementation as found in: http://www.java.achchuthan.org/2012/02/n-queens-problem-in-java.html
 */
public class NQueens {
    private int mSize;
    private int[] mQueens;
    private boolean mLogOutput;

    public NQueens(int size, boolean logOutput) {
        mSize = size;
        mQueens = new int[size];
        mLogOutput = logOutput;
    }

    public void solve() {
        placeNqueens(0, mQueens.length);
    }

    private boolean canPlaceQueen(int row, int column) {
        /**
         * Returns TRUE if a queen can be placed in row r and column c.
         * Otherwise it returns FALSE. x[] is a global array whose first (r-1)
         * values have been set.
         */
        for (int i = 0; i < row; i++) {
            if (mQueens[i] == column || (i - row) == (mQueens[i] - column) ||(i - row) == (column - mQueens[i]))
            {
                return false;
            }
        }
        return true;
    }

    private void placeNqueens(int r, int n) {
        /**
         * Using backtracking this method prints all possible placements of n
         * queens on an n x n chessboard so that they are non-attacking.
         */
        for (int c = 0; c < n; c++) {
            if (canPlaceQueen(r, c)) {
                mQueens[r] = c;
                if (r == n - 1 && mLogOutput) {
                    printQueens();
                } else {
                    placeNqueens(r + 1, n);
                }
            }
        }
    }

    private void printQueens() {
        String solution = "";
        for (int column : mQueens) solution = solution + column + " ";
        Log.i("NQueens", solution);
    }

}
