package se.ifthenel.android.example.ffa;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/*
 * This work is licensed under the Creative Commons Attribution 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 *
 * Created in 2017-04 by Jonas Hellström (SHELLSTROM)
 */


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

  private static final String[] LOCATION_PERMS={
      android.Manifest.permission.ACCESS_FINE_LOCATION
  };

  LatLng myPosition;
  private Location mLocation;
  private GoogleMap mMap;
  private TextView mBottomSheetTitle;
  private TextView mBottomSheetDescription;
  private ImageButton mOpenPlanButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    if(getIntent().hasExtra("location")) {
      mLocation = getIntent().getParcelableExtra("location");
    }

    mBottomSheetTitle = (TextView) findViewById(R.id.text_bottom_sheet_title);
    mBottomSheetDescription = (TextView) findViewById(R.id.text_bottom_sheet_description);
    View bottomSheet = findViewById(R.id.bottom_sheet);
    if(!mLocation.getPlanImage().isEmpty()) {
      bottomSheet.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = new Intent(MapsActivity.this, PlanActivity.class);
          intent.putExtra("location", mLocation);
          MapsActivity.this.startActivity(intent);
        }
      });
    }

    mOpenPlanButton = (ImageButton) findViewById(R.id.button_open_plan);
    if(!mLocation.getPlanImage().isEmpty()) {
      mOpenPlanButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = new Intent(MapsActivity.this, PlanActivity.class);
          intent.putExtra("location", mLocation);
          MapsActivity.this.startActivity(intent);
        }
      });
    } else {
      mOpenPlanButton.setVisibility(ImageView.INVISIBLE);
    }

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    if(!canAccessLocation()) {
      // TODO Replace this 1111 with something more appropriate
      ActivityCompat.requestPermissions(this, LOCATION_PERMS, 1111);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case 1111: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Permission granted, yay! Let's draw that MyLocation overlay!
          showMyLocationOverlay();
        } else {
          // Permission denied, boo! Do nothing...
        }
        return;
      }
    }
  }

  /**
   * Manipulates the map once available. This callback is triggered when the map is ready to be
   * used. This is where we can add markers or lines, add listeners or move the camera.
   */
  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;

    if(canAccessLocation()) {
      showMyLocationOverlay();
    }

    if(mLocation != null) {
      mMap.getUiSettings().setZoomControlsEnabled(true);
      LatLng markerLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
      // Place location marker
      mMap.addMarker(
          new MarkerOptions()
              .position(markerLocation)
              .title(mLocation.getLocationDescription())
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_150)));

      // Move camera taking location provided bearing and zoom in account
      CameraPosition currentPlace = new CameraPosition.Builder()
          .target(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))
          .bearing(mLocation.getBearing())
          .zoom(mLocation.getZoom()).build();
      googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

      mBottomSheetTitle.setText(mLocation.getLocationName());
      mBottomSheetDescription.setText(mLocation.getLocationDescription());

      if(!mLocation.getPlanImage().isEmpty()) {
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
          @Override
          public void onInfoWindowClick(Marker marker) {
            Intent intent = new Intent(MapsActivity.this, PlanActivity.class);
            intent.putExtra("location", mLocation);
            MapsActivity.this.startActivity(intent);
          }
        });
      }
    }
  }

  private boolean canAccessLocation() {
    return(hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION));
  }

  private boolean hasPermission(String perm) {
    return(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));
  }

  private void showMyLocationOverlay() {
    try {

        // Enabling MyLocation Layer of Google Map
        mMap.setMyLocationEnabled(true);

        // Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location
        android.location.Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
          // Getting latitude of the current location
          double latitude = location.getLatitude();

          // Getting longitude of the current location
          double longitude = location.getLongitude();

          myPosition = new LatLng(latitude, longitude);
        }
    } catch(SecurityException error) {
      error.printStackTrace();
    }
  }
}
