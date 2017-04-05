package se.ifthenel.android.example.ffa;

import static com.google.android.gms.wearable.DataMap.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class for placing everything that should be shared within the app but doesn't fit in its
 * own category. Some things are used more than others.
 */
public class Utilities {
  public static boolean LOCAL = true;

  public static Bitmap getBitmap(Context context, String imageName) {
    Bitmap b = null;
    try {
      File f = new File(context.getFilesDir(), imageName);
      b = BitmapFactory.decodeStream(new FileInputStream(f));
      b.toString();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return b;
  }

  public static boolean putBitmap(Context context, Drawable drawable, String imageName) {
    boolean success = false;
    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
    File file = new File(context.getFilesDir(), imageName);

    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      try {
        if (out != null) {
          out.close();
          success = true;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return success;
  }

  public void longStringLogging(String stringToLog) {
    int maxLogStringSize = 1000;
    for (int i = 0; i <= stringToLog.length() / maxLogStringSize; i++) {
      int start = i * maxLogStringSize;
      int end = (i + 1) * maxLogStringSize;
      end = end > stringToLog.length() ? stringToLog.length() : end;
      Log.v(TAG, stringToLog.substring(start, end));
    }
  }
}
