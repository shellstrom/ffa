package se.ifthenel.android.example.ffa;

import com.google.gson.annotations.SerializedName;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Keeps track of all the details of a location. Can also be passed as an extras between activities.
 */
public class Location implements Parcelable {
  /**
   * Static field used to regenerate object, individually or as arrays
   */
  public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
    public Location createFromParcel(Parcel parcel) {
      return new Location(parcel);
    }

    public Location[] newArray(int size) {
      return new Location[size];
    }
  };
  @SerializedName("id")
  private int mId;
  @SerializedName("name")
  private String mLocationName;
  @SerializedName("description")
  private String mLocationDescription;
  @SerializedName("plan-image")
  private String mPlanImage;
  @SerializedName("latitude")
  private double mLatitude;
  @SerializedName("longitude")
  private double mLongitude;
  @SerializedName("bearing")
  private float mBearing;
  @SerializedName("zoom")
  private float mZoom;
  @SerializedName("bitmap-x")
  private int mLayoutX;
  @SerializedName("bitmap-y")
  private int mLayoutY;
  @SerializedName("pre-scale")
  private float mPreScale;
  @SerializedName("last-update")
  private String mLastUpdate;

  public Location(Parcel parcel) {
    mId = parcel.readInt();
    mLocationName = parcel.readString();
    mLocationDescription = parcel.readString();
    mPlanImage = parcel.readString();
    mLatitude = parcel.readDouble();
    mLongitude = parcel.readDouble();
    mBearing = parcel.readFloat();
    mZoom = parcel.readFloat();
    mLayoutX = parcel.readInt();
    mLayoutY = parcel.readInt();
    mPreScale = parcel.readFloat();
    mLastUpdate = parcel.readString();
  }

  public int getId() {
    return mId;
  }

  public void setId(int id) {
    mId = id;
  }

  public String getLocationName() {
    return mLocationName;
  }

  public void setLocationName(String locationName) {
    mLocationName = locationName;
  }

  public String getLocationDescription() {
    return mLocationDescription;
  }

  public void setLocationDescription(String location) {
    mLocationDescription = location;
  }

  public String getPlanImage() {
    return mPlanImage;
  }

  public void setPlanImage(String planImage) {
    mPlanImage = planImage;
  }

  public double getLatitude() {
    return mLatitude;
  }

  public void setLatitude(long latitude) {
    mLatitude = latitude;
  }

  public double getLongitude() {
    return mLongitude;
  }

  public void setLongitude(long longitude) {
    mLongitude = longitude;
  }

  public float getBearing() {
    return mBearing;
  }

  public void setBearing(long bearing) {
    mBearing = bearing;
  }

  public float getZoom() {
    return mZoom;
  }

  public void setZoom(long zoom) { mZoom = zoom; }

  public int getLayoutX() {
    return mLayoutX;
  }

  public void setLayoutX(int layoutX) {
    mLayoutX = layoutX;
  }

  public int getLayoutY() {
    return mLayoutY;
  }

  public void setLayoutY(int layoutY) {
    mLayoutY = layoutY;
  }

  public float getPreScale() {
    return mPreScale;
  }

  public void setPreScale(long scale) {
    mPreScale = scale;
  }

  public String getLastUpdate() {
    return mLastUpdate;
  }

  public void setLastUpdate(String lastUpdate) {
    mLastUpdate = lastUpdate;
  }

  public Uri getLocationUri() {
    return Uri.parse("geo:" + mLatitude + "," + mLongitude + "?q=(" + mLocationDescription + ")@" + mLatitude + "," + mLongitude);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(mId);
    parcel.writeString(mLocationName);
    parcel.writeString(mLocationDescription);
    parcel.writeString(mPlanImage);
    parcel.writeDouble(mLatitude);
    parcel.writeDouble(mLongitude);
    parcel.writeFloat(mBearing);
    parcel.writeFloat(mZoom);
    parcel.writeInt(mLayoutX);
    parcel.writeInt(mLayoutY);
    parcel.writeFloat(mPreScale);
    parcel.writeString(mLastUpdate);
  }
}