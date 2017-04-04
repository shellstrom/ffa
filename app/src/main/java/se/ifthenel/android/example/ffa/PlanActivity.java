package se.ifthenel.android.example.ffa;

import static se.ifthenel.android.example.ffa.Utilities.LOCAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/*
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 *
 * Created in 2017-04 by Jonas Hellström (SHELLSTROM)
 */

public class PlanActivity extends AppCompatActivity {
  private Location mLocation;

  private PlanLayout mPlanLayout;
  private ImageView imageView;
  private TextView mTextView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_plan);

    mPlanLayout = (PlanLayout) findViewById(R.id.zoom_view);
    imageView = (ImageView) findViewById(R.id.zoomview_image_location);
    mTextView = (TextView) findViewById(R.id.text_location_info);

    if(getIntent().hasExtra("location")) {
      mLocation = getIntent().getParcelableExtra("location");
      mTextView.setText(mLocation.getLocationDescription());

      File file;
      Bitmap myBitmap = null;
      if(LOCAL) {
        try {
          InputStream istr = getAssets().open(mLocation.getPlanImage());
          myBitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        file = new File(getExternalFilesDir(null), mLocation.getPlanImage());
        myBitmap = BitmapFactory.decodeFile(file.getPath());
      }
      imageView.setImageBitmap(myBitmap);
    }

    ViewTreeObserver vto = imageView.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

      public void onGlobalLayout() {
        // Remove the listener to prevent being called again by future layout events
        imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        int[] values = getBitmapPositionInsideImageView(imageView);
        int x = values[0];
        int y = values[1];
        int actualWidth = values[2];
        int actualHeight = values[3];
        int width = values[4];
        int height = values[5];

        float[] scaleValues = getBitmapScaleInsideImageView(imageView);
        float scaleX = scaleValues[0];
        float scaleY = scaleValues[1];

        mPlanLayout.setSourceImageDimensions(width, height);
        if(mLocation.getLayoutX() != 0 && mLocation.getLayoutY() != 0) {
          int theX = x + (Math.round(mLocation.getLayoutX() * scaleX));
          int theY = y + (Math.round(mLocation.getLayoutY() * scaleY));
          mPlanLayout.placeMarker(theX, theY);
          zoomAndCenterViewPort(x, y, mLocation.getLayoutX(), mLocation.getLayoutY(), scaleX, scaleY);
        }
      }
    });

  }

  private void zoomAndCenterViewPort(int paddingX, int paddingY, float x, float y, float scaleX, float scaleY) {
    int centerViewX = (mPlanLayout.getRight() / 2);
    int centerViewY = (mPlanLayout.getBottom() / 2);

    float newScaledLocationX = x*scaleX;
    float newScaledLocationY = y*scaleY;
    float newScale = mLocation.getPreScale();

    mPlanLayout.init(this, -(paddingX+Math.round(newScaledLocationX)), -(paddingY+Math.round(newScaledLocationY)), newScale, centerViewX, centerViewY);
  }

  /**
   * http://stackoverflow.com/questions/6536418/why-are-the-width-height-of-the-drawable-in-imageview-wrong
   * http://stackoverflow.com/a/26930852/975641
   *
   * Returns the bitmap position inside an imageView.
   *
   * @param imageView source ImageView
   * @return 0: left, 1: top, 2: width, 3: height, 4: original width, 5: original height
   */
  public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
    int[] ret = new int[6];

    if (imageView == null || imageView.getDrawable() == null)
      return ret;

    // Get image dimensions
    // Get image matrix values and place them in an array
    float[] f = new float[9];
    imageView.getImageMatrix().getValues(f);

    // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
    final float scaleX = f[Matrix.MSCALE_X];
    final float scaleY = f[Matrix.MSCALE_Y];

    // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
    final Drawable d = imageView.getDrawable();
    final int origW = d.getIntrinsicWidth();
    final int origH = d.getIntrinsicHeight();
    ret[4] = origW;
    ret[5] = origH;

    // Calculate the actual dimensions
    final int actW = Math.round(origW * scaleX);
    final int actH = Math.round(origH * scaleY);

    ret[2] = actW;
    ret[3] = actH;

    // Get image position
    // We assume that the image is centered into ImageView
    int imgViewW = imageView.getWidth();
    int imgViewH = imageView.getHeight();

    int top = (int) (imgViewH - actH)/2;
    int left = (int) (imgViewW - actW)/2;

    ret[0] = left;
    ret[1] = top;

    return ret;
  }

  /**
   * http://stackoverflow.com/questions/6536418/why-are-the-width-height-of-the-drawable-in-imageview-wrong
   * http://stackoverflow.com/a/26930852/975641
   * http://www.peachpit.com/articles/article.aspx?p=1846580&seqNum=2
   *
   * Returns the scale inside an imageView.
   *
   * @param imageView source ImageView
   * @return 0: scale X, 1: scale Y
   */
  public static float[] getBitmapScaleInsideImageView(ImageView imageView) {
    float[] ret = new float[4];

    if (imageView == null || imageView.getDrawable() == null)
      return ret;

    // Get image dimensions
    // Get image matrix values and place them in an array
    float[] f = new float[9];
    imageView.getImageMatrix().getValues(f);

    // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
    final float scaleX = f[Matrix.MSCALE_X];
    final float scaleY = f[Matrix.MSCALE_Y];
    ret[0] = scaleX;
    ret[1] = scaleY;

    return ret;
  }
}
