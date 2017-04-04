package se.ifthenel.android.example.ffa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/*
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 *
 * Created in 2017-04 by Jonas Hellström (SHELLSTROM)
 */

/**
 * http://stackoverflow.com/a/38205219/975641
 *
 * Layout that provides pinch-zooming of content. This view should have exactly one child
 * view containing the content.
 */
public class PlanLayout extends ViewGroup {
  // States.
  private static final byte NONE = 0;
  private static final byte DRAG = 1;
  private static final byte ZOOM = 2;
  private byte mode = NONE;

  int MIN_ZOOM = 1;
  int MAX_ZOOM = 4;

  private int mImageActualWidth = 0;
  private int mImageActualHeight = 0;

  private PointF mAbsoluteViewCoordinates = new PointF(0,0);
  private PointF mAbsoluteImageBottomRightCoordinates = new PointF(0,0);

  // Matrices used to move and zoom image.
  private Matrix matrix = new Matrix();
  private Matrix matrixInverse = new Matrix();
  private Matrix savedMatrix = new Matrix();

  // Matrices for marker
  private Matrix mMatrixMarkerSource = new Matrix();
  private Matrix mMatrixMarker = new Matrix();
  private Matrix mMatrixInverseMarker = new Matrix();
  private Matrix mSavedMatrixMarker = new Matrix();

  private ImageView mMapMarker;

  // Parameters for zooming.
  private PointF start = new PointF();
  private PointF mid = new PointF();
  private float oldDist = 1f;
  private float[] lastEvent = null;
  private long lastDownTime = 0l;

  private float[] mDispatchTouchEventWorkingArray = new float[2];
  private float[] mOnTouchEventWorkingArray = new float[2];

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    mDispatchTouchEventWorkingArray[0] = ev.getX();
    mDispatchTouchEventWorkingArray[1] = ev.getY();
    mDispatchTouchEventWorkingArray = screenPointsToScaledPoints(mDispatchTouchEventWorkingArray);
    ev.setLocation(mDispatchTouchEventWorkingArray[0], mDispatchTouchEventWorkingArray[1]);
    return super.dispatchTouchEvent(ev);
  }

  public PlanLayout(Context context) {
    super(context);
    init(context);
  }

  public PlanLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public PlanLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }


  private void init(Context context) {}

  public void init(Context context, int preScaledX, int preScaledY, float preScale, int centerViewX, int centerViewY) {
    if(preScaledX != 0 && preScaledY != 0 && preScale != 0f) {
      // Zooms and centers viewport if specified in the location data
      matrix.set(savedMatrix);
      float dx = preScaledX+(centerViewX/preScale);
      float dy = preScaledY+(centerViewY/preScale);
      matrix.postTranslate(dx, dy);
      matrix.postScale(preScale, preScale, mid.x, mid.y);
      matrix.invert(matrixInverse);
      invalidate();
    }
  }

  public void setSourceImageDimensions(int width, int height) {
    mImageActualWidth = width;
    mImageActualHeight = height;
  }

  public void placeMarker(float x, float y) {
    RelativeLayout parent = (RelativeLayout) this.getParent();
    mMapMarker = (ImageView) parent.findViewById(R.id.map_marker);
    mMapMarker.setScaleType(ImageView.ScaleType.MATRIX);

    mMatrixMarker.postTranslate(x, y);
    mMatrixMarker.postScale(1f,1f,mid.x,mid.y);
    mMatrixMarker.invert(mMatrixInverseMarker);

    mMatrixMarkerSource.postTranslate(x,y);
    mMatrixMarkerSource.postScale(1f,1f,mid.x,mid.y);
    mMatrixMarkerSource.invert(mMatrixInverseMarker);
    float[] valuesMatrixSource = new float[9];
    mMatrixMarkerSource.getValues(valuesMatrixSource);
  }

  /**
   * Determine the space between the first two fingers
   */
  private float spacing(MotionEvent event) {
    float x = event.getX(0) - event.getX(1);
    float y = event.getY(0) - event.getY(1);
    return (float) Math.sqrt(x * x + y * y);
  }

  /**
   * Calculate the mid point of the first two fingers
   */
  private void midPoint(PointF point, MotionEvent event) {
    float x = event.getX(0) + event.getX(1);
    float y = event.getY(0) + event.getY(1);
    point.set(x / 2, y / 2);
  }

  /**
   * Conversion method to transform scaled coordinates to screen coordinate equivalents
   *
   * @param a Scaled coordinates array
   * @return Screen coordinate array
   */
  private float[] scaledPointsToScreenPoints(float[] a) {
    matrix.mapPoints(a);
    return a;
  }

  /**
   * Conversion method to transform screen coordinates to scaled coordinate equivalents
   *
   * @param a Screen coordinates array
   * @return Scaled coordinates array
   */
  private float[] screenPointsToScaledPoints(float[] a) {
    matrixInverse.mapPoints(a);
    return a;
  }

  /*
  The marker inherits from this class. When the marker is added we determine its location.
   */
  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
      }
    }
  }

  /*
  The marker inherits from this class. When the marker is added we determine its size.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
      }
    }
  }

  /*
  Draw the requested changes taking pan and zoom into account
   */
  @Override
  protected void dispatchDraw(Canvas canvas) {
    // Keep a reference to the marker's initial location and scale for comparison
    float[] valuesMarkerSource = new float[9];
    mMatrixMarkerSource.getValues(valuesMarkerSource);
    float sx = valuesMarkerSource[Matrix.MTRANS_X];
    float sy = valuesMarkerSource[Matrix.MTRANS_Y];

    // Keep a reference to the current location and scale of out ViewGroup
    float[] values = new float[9];
    matrix.getValues(values);

    Drawable d = getResources().getDrawable(R.drawable.ic_map_marker_150);
    // Adjust marker image position based on its measured size
    float vX = ((sx * values[Matrix.MSCALE_X]) + values[Matrix.MTRANS_X]) - (d.getIntrinsicWidth()/2);
    float vY = ((sy * values[Matrix.MSCALE_Y]) + values[Matrix.MTRANS_Y]) - (d.getIntrinsicHeight());
    mMatrixMarker.reset();
    mMatrixMarker.postTranslate(vX, vY);


    float[] valuesMarker = new float[9];
    mMatrixMarker.getValues(valuesMarker);
    if(mMapMarker != null) {
      // We want to use a marker, so save the translated matrix to it
      mMapMarker.setImageMatrix(mMatrixMarker);
    } else {
      /*
      TODO This is suboptimal. We really should just hide the marker once and not deal with it any more

      We don't want to use a marker, so hide it
       */
      RelativeLayout parent = (RelativeLayout) this.getParent();
      mMapMarker = (ImageView) parent.findViewById(R.id.map_marker);
      mMapMarker.setVisibility(ImageView.INVISIBLE);
    }

    canvas.save();
    canvas.translate(values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
    canvas.scale(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y]);
    super.dispatchDraw(canvas);
    canvas.restore();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mOnTouchEventWorkingArray[0] = event.getX();
    mOnTouchEventWorkingArray[1] = event.getY();

    mOnTouchEventWorkingArray = scaledPointsToScreenPoints(mOnTouchEventWorkingArray);

    event.setLocation(mOnTouchEventWorkingArray[0], mOnTouchEventWorkingArray[1]);

    // Here is a lot of transformation matrix magix
    switch (event.getAction() & MotionEvent.ACTION_MASK) {

      case MotionEvent.ACTION_DOWN:
        savedMatrix.set(matrix);
        mode = DRAG;
        lastEvent = null;
        long downTime = event.getDownTime();
        // One finger zoom
        if (downTime - lastDownTime < 300l) {
          float density = getResources().getDisplayMetrics().density;
          if (Math.max(Math.abs(start.x - event.getX()), Math.abs(start.y - event.getY())) < 40.f * density) {
            savedMatrix.set(matrix);
            mid.set(event.getX(), event.getY());
            mode = ZOOM;
            lastEvent = new float[4];
            lastEvent[0] = lastEvent[1] = event.getX();
            lastEvent[2] = lastEvent[3] = event.getY();
          }
          lastDownTime = 0l;
        } else {
          lastDownTime = downTime;
        }
        start.set(event.getX(), event.getY());

        mSavedMatrixMarker.set(mMatrixMarker);
        break;

      case MotionEvent.ACTION_POINTER_DOWN:
        oldDist = spacing(event);
        if (oldDist > 10f) {
          savedMatrix.set(matrix);
          midPoint(mid, event);
          mode = ZOOM;
        }
        lastEvent = new float[4];
        lastEvent[0] = event.getX(0);
        lastEvent[1] = event.getX(1);
        lastEvent[2] = event.getY(0);
        lastEvent[3] = event.getY(1);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        mode = NONE;
        lastEvent = null;
        break;

      case MotionEvent.ACTION_MOVE:
        final float density = getResources().getDisplayMetrics().density;
        if (mode == DRAG) {
          matrix.set(savedMatrix);
          float[] values = new float[9];
          matrix.getValues(values);

          dragPanConstraint(event, values, density);
        } else if (mode == ZOOM) {
          if (event.getPointerCount() > 1) {
            float newDist = spacing(event);
            if (newDist > 10f * density) {
              // Two finger zoom
              matrix.set(savedMatrix);
              float scale = (newDist / oldDist);
              float[] values = new float[9];
              matrix.getValues(values);

              scale = getBoundedScale(scale, values);
              matrix.postScale(scale, scale, mid.x, mid.y);
              matrix.getValues(values);

              dragPanConstraint(event, values, density);
            }
          } else {
            // Double tap zoom
            matrix.set(savedMatrix);
            float[] values = new float[9];
            matrix.getValues(values);
            float scale = event.getY() / start.y;

            scale = getBoundedScale(scale, values);
            matrix.postScale(scale, scale, mid.x, mid.y);
            matrix.getValues(values);

            dragPanConstraint(event, values, density);
          }
        }
        break;
    }

    invalidate();
    return true;
  }

  /**
   * Limits scaling according to a pre set min/max value
   *
   * @param scale New calculated scale value
   * @param values Current matrix scale
   * @return A scale value that never exceeds the pre set min/max value
   */
  private float getBoundedScale(float scale, float[] values) {
    if(scale*values[Matrix.MSCALE_X] >= MAX_ZOOM) {
      scale = MAX_ZOOM/values[Matrix.MSCALE_X];
    }
    if(scale*values[Matrix.MSCALE_X] <= MIN_ZOOM) {
      scale = MIN_ZOOM/values[Matrix.MSCALE_X];
    }

    return scale;
  }

  /**
   * Limits panning/dragging of this ViewGroup taking ZOOM vs DRAG into account. Assumes ZOOM
   * panning limit per default
   *
   * @param event The current touch event
   * @param values This ViewGroup's current matrix values
   * @param density DisplayMetrics density
   */
  private void dragPanConstraint(MotionEvent event, float[] values, float density) {
    float dx = 0;
    float dy = 0;
    if(mode == DRAG) {
      dx = event.getX() - start.x;
      dy = event.getY() - start.y;
    }


    /*
    DRAG/PAN CONSTRAINTS
    */

    // Calculate this ViewGroup's and marker absolute position on screen
    if (mAbsoluteViewCoordinates.x == 0 && mAbsoluteViewCoordinates.y == 0) {
      mAbsoluteViewCoordinates.x -= values[Matrix.MTRANS_X];
      mAbsoluteViewCoordinates.y -= values[Matrix.MTRANS_Y];
      mAbsoluteImageBottomRightCoordinates.x = mImageActualWidth;
      mAbsoluteImageBottomRightCoordinates.y = mImageActualHeight;
    }
    if(mode == DRAG) {
      mAbsoluteViewCoordinates.x = dx + values[Matrix.MTRANS_X];
      mAbsoluteViewCoordinates.y = dy + values[Matrix.MTRANS_Y];
    } else {
      mAbsoluteViewCoordinates.x = values[Matrix.MTRANS_X];
      mAbsoluteViewCoordinates.y = values[Matrix.MTRANS_Y];
    }
    mAbsoluteImageBottomRightCoordinates.x = -((getWidth() * values[Matrix.MSCALE_X]) - getWidth());
    mAbsoluteImageBottomRightCoordinates.y = -((getHeight() * values[Matrix.MSCALE_Y]) - getHeight());

    // Determine if any edge is hitting its bound
    boolean xHitBound = false;
    boolean yHitBound = false;
    if (mAbsoluteViewCoordinates.x >= 0) {
      values[Matrix.MTRANS_X] = 0;
      values[Matrix.MTRANS_Y] = values[Matrix.MTRANS_Y] + dy;
      xHitBound = true;
    }
    if (mAbsoluteViewCoordinates.x <= mAbsoluteImageBottomRightCoordinates.x) {
      values[Matrix.MTRANS_X] = mAbsoluteImageBottomRightCoordinates.x;
      values[Matrix.MTRANS_Y] = values[Matrix.MTRANS_Y] + dy;
      xHitBound = true;
    }
    if (mAbsoluteViewCoordinates.y >= 0) {
      if (xHitBound) {
        values[Matrix.MTRANS_Y] = 0;
      } else {
        values[Matrix.MTRANS_X] = values[Matrix.MTRANS_X] + dx;
        values[Matrix.MTRANS_Y] = 0;
      }
      yHitBound = true;
    }
    if (mAbsoluteViewCoordinates.y <= mAbsoluteImageBottomRightCoordinates.y) {
      if (xHitBound) {
        values[Matrix.MTRANS_Y] = mAbsoluteImageBottomRightCoordinates.y;
      } else {
        values[Matrix.MTRANS_X] = values[Matrix.MTRANS_X] + dx;
        values[Matrix.MTRANS_Y] = mAbsoluteImageBottomRightCoordinates.y;
      }
      yHitBound = true;
    }
    if(mode == DRAG) {
      if (xHitBound || yHitBound) {
        matrix.setValues(values);
      } else {
        matrix.postTranslate(dx, dy);
      }
    } else {
      if (xHitBound || yHitBound) {
        matrix.setValues(values);
      }
    }

    matrix.invert(matrixInverse);
    if(mode == DRAG) {
    } else {
      // Determine the state of double tap zoom
      if (Math.max(Math.abs(start.x - event.getX()), Math.abs(start.y - event.getY())) > 20.f * density) {
        lastDownTime = 0l;
      }
    }
    mMatrixMarker.set(mSavedMatrixMarker);
    mMatrixMarker.postTranslate(dx, dy);
    mMatrixMarker.invert(mMatrixInverseMarker);
  }
}