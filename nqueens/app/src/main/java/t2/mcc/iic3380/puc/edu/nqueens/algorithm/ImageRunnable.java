package t2.mcc.iic3380.puc.edu.nqueens.algorithm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.puc.astral.CloudRunnable;
import edu.puc.astral.Params;

/**
 * Created by jose on 11/25/15.
 */
public class ImageRunnable extends CloudRunnable {
    public static final String KEY_IMAGE = "image";
    public static final String FILE_NAME = "blur.jpg";

    @Override
    public Params execute(Params params, Params lastState) {
        try {
            InputStream is = params.openFile(getContext(), KEY_IMAGE);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);

            Matrix matrix = new Matrix();
            matrix.postRotate(180, bitmap.getWidth() / 2, bitmap.getHeight() / 2);

            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            Context context = getContext();
            File file = new File(context.getFilesDir(), FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);

            newBitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.close();

            Params result = new Params();
            result.putFile(KEY_IMAGE, file);
            return result;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
