package se.ifthenel.android.example.ffa;

import static se.ifthenel.android.example.ffa.Utilities.LOCAL;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * The main startup activity.
 *
 * Parses local or remote (through an AsyncTask) JSON to create a list of locations to display.
 * When interacting with the activity, specific locations will be passed to other activities for
 * further use.
 */
public class LocationsActivity extends AppCompatActivity implements LOG {
  private static final String KEY_LAYOUT_MANAGER = "layoutManager";
  private static final int SPAN_COUNT = 2;
  protected LayoutManagerType mCurrentLayoutManagerType;
  private ArrayList<Location> mLocations = new ArrayList<>();
  private ImageButton mClearFastFilter;
  private EditText mEditFastFilter;
  private RecyclerView mRecyclerView;
  private LocationsAdapter mListsAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private FetchLocations mAsyncTask;
  private SharedPreferences mSharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_locations);
    mSharedPreferences = getPreferences(Context.MODE_PRIVATE);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // TODO Replace this basic filtering technique, with e.g. a Filterable adapter
    mEditFastFilter = (EditText) findViewById(R.id.edit_fast_filter);

    mClearFastFilter = (ImageButton) findViewById(R.id.button_clear_fast_filter);
    mClearFastFilter.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mEditFastFilter.setText("");
      }
    });

    if (savedInstanceState != null) {
      // Restore saved layout manager type.
      if (savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER) != null) {
        mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState.getSerializable(KEY_LAYOUT_MANAGER);
      }
    }

    if(LOCAL) {
      // We get the asset stored locations
      mLocations = getAssetsLocations();
    } else {
      // We get the external storage stored locations
      mLocations = getExternalStorageLocations();
    }

    // Populate the RecyclerView with what we have so far
    refreshAdapter();

    if(!LOCAL) {
      // We can use an AsyncTask to collect information and resources e.g. from a service
      mAsyncTask = new FetchLocations();
      mAsyncTask.execute();
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // If we were to add this listener in onCreate, we would run into issues when the screen
    // rotates.
    mEditFastFilter.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        final String query = charSequence.toString().toLowerCase().trim();
        final ArrayList<Location> filteredList = new ArrayList<>();

        if(count > 0 && mClearFastFilter.getVisibility() == ImageView.INVISIBLE) {
          mClearFastFilter.setVisibility(ImageView.VISIBLE);
        } else if(count == 0) {
          mClearFastFilter.setVisibility(ImageView.INVISIBLE);
        }
        for (int i = 0; i < mLocations.size(); i++) {
          final String text = mLocations.get(i).getLocationName().toLowerCase();
          if (text.contains(query)) {
            filteredList.add(mLocations.get(i));
          }
        }

        // Update the recyclerview excluding the non-matching parts of the filter string
        mRecyclerView.setLayoutManager(new LinearLayoutManager(LocationsActivity.this));
        mListsAdapter = new LocationsAdapter(filteredList);
        mRecyclerView.setAdapter(mListsAdapter);
        mListsAdapter.notifyDataSetChanged();
      }

      @Override
      public void afterTextChanged(Editable editable) {}
    });
  }

  @Override
  protected void onPause() {
    super.onPause();

    if(mAsyncTask != null) {
      // TODO Replace this with e.g. a Fragment, for better thread handling
      mAsyncTask.cancel(true);
    }
  }

  private void refreshAdapter() {
    // Bind the adapter and recyclerview for displaying available locations
    mListsAdapter = new LocationsAdapter(mLocations);
    mRecyclerView = (RecyclerView) findViewById(R.id.locations_recyclerview);
    mRecyclerView.setAdapter(mListsAdapter);
    mLayoutManager = new LinearLayoutManager(this);
    mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

    setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
  }

  public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
    int scrollPosition = 0;

    // If a layout manager has already been set, get current scroll position.
    if (mRecyclerView.getLayoutManager() != null) {
      scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
          .findFirstCompletelyVisibleItemPosition();
    }

    switch (layoutManagerType) {
      case GRID_LAYOUT_MANAGER:
        mLayoutManager = new GridLayoutManager(this, SPAN_COUNT);
        mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
        break;
      case LINEAR_LAYOUT_MANAGER:
        mLayoutManager = new LinearLayoutManager(this);
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        break;
      default:
        mLayoutManager = new LinearLayoutManager(this);
        mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
    }

    mRecyclerView.setLayoutManager(mLayoutManager);
    mRecyclerView.scrollToPosition(scrollPosition);
  }

  /*
   * Get locations from string, process them and make them available through an ArrayList
   */
  private ArrayList<Location> parseLocations(String json) {
    Gson gson = new Gson();
    Type type = new TypeToken<ArrayList<Location>>() {}.getType();
    return gson.fromJson(json, type);
  }

  /* Checks if external storage is available for read and write */
  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      return true;
    }
    return false;
  }

  /* Checks if external storage is available to at least read */
  public boolean isExternalStorageReadable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state) ||
        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      return true;
    }
    return false;
  }

  private enum LayoutManagerType {
    GRID_LAYOUT_MANAGER,
    LINEAR_LAYOUT_MANAGER
  }

  /**
   * Parse a pre set file located in external storage containing JSON describing one or more
   * locations
   *
   * @return ArrayList<Location> A list of parsed locations
   */
  private ArrayList<Location> getExternalStorageLocations() {
    ArrayList<Location> localLocations = new ArrayList<>();
    String ret = "";
    File file = new File(getExternalFilesDir(null), "locations.json");
    try {
      InputStream inputStream = new FileInputStream(file);

      if ( inputStream != null ) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString = "";
        StringBuilder stringBuilder = new StringBuilder();

        while ( (receiveString = bufferedReader.readLine()) != null ) {
          stringBuilder.append(receiveString);
        }

        inputStream.close();
        ret = stringBuilder.toString();
        localLocations = parseLocations(ret);
      }
    } catch (FileNotFoundException e) {
      //Log.e("login activity", "File not found: " + e.toString());
    } catch (IOException e) {
      //Log.e("login activity", "Can not read file: " + e.toString());
    }
    return localLocations;
  }

  /**
   * Parse a pre set file located in assets storage containing JSON describing one or more locations
   *
   * @return ArrayList<Location> A list of parsed locations
   */
  private ArrayList<Location> getAssetsLocations() {
    ArrayList<Location> localLocations = new ArrayList<>();
    String ret;
    try {
      InputStream inputStream = getAssets().open("locations.json");

      if ( inputStream != null ) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString;
        StringBuilder stringBuilder = new StringBuilder();

        while ( (receiveString = bufferedReader.readLine()) != null ) {
          stringBuilder.append(receiveString);
        }

        inputStream.close();
        ret = stringBuilder.toString();
        localLocations = parseLocations(ret);
      }
    } catch (FileNotFoundException e) {
      //Log.e("login activity", "File not found: " + e.toString());
    } catch (IOException e) {
      //Log.e("login activity", "Can not read file: " + e.toString());
    }
    return localLocations;
  }

  /**
   * An AsyncTask for fetching and adding news articles of the user-defined news feeds. Note: Cancelling an AsyncTask
   * might not cancel the currently active process/thread immediately. doInBackground will run its course if no
   * defined procedure to stop the code from executing exists.
   */
  public class FetchLocations extends AsyncTask<String, Integer, Boolean> implements LOG {
    private ProgressDialog progressDialog = null;
    private ArrayList<Location> mStoredLocations;
    private ArrayList<Location> mFetchedLocations;
    private String mLocationsJson = "";
    private volatile boolean running = true;
    private boolean mIsConnected = true;
    private boolean mIsSuccessfullySynced = false;

    @Override
    protected void onPreExecute() {
      if(mSharedPreferences.getBoolean("first_run", true)) {
        // If it's the first run syncing, we display a progressDialog, because it can take a while
        // to sync. After a successful sync, we will not show the dialog again, but instead sync
        // silently.
        progressDialog = new ProgressDialog(LocationsActivity.this);
        progressDialog.setMessage(getResources().getString(R.string.fetching_location_data));
        progressDialog.setCancelable(true);

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            cancel(true);
            mLocations = mStoredLocations;
            refreshAdapter();
          }
        });
        progressDialog.show();
      }
      running = true;
      String ret = "";
      File file = new File(getExternalFilesDir(null), "locations.json");
      try {
        InputStream inputStream = new FileInputStream(file);

        if ( inputStream != null ) {
          InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
          BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
          String receiveString = "";
          StringBuilder stringBuilder = new StringBuilder();

          while ( (receiveString = bufferedReader.readLine()) != null ) {
            stringBuilder.append(receiveString);
          }

          inputStream.close();
          ret = stringBuilder.toString();
          mStoredLocations = parseLocations(ret);
        }
      } catch (FileNotFoundException e) {
        //Log.e("login activity", "File not found: " + e.toString());
      } catch (IOException e) {
        //Log.e("login activity", "Can not read file: " + e.toString());
      }

    }

    @Override
    protected Boolean doInBackground(String... status) {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      mIsConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

      if(!mIsConnected) {
        //Log.d(TAG, "We're not connected...");
        return false;
      }
      if(isCancelled()) {
        //Log.d(TAG, "We're cancelled...");
        return false;
      }
      if(!isExternalStorageWritable()) {
        //Log.d(TAG, "We're not allowed external storage access...");
        return false;
      }
      // Woohoo! We haven't been thrown out of Async yet!

      // TODO See if there's another way of stopping an AsyncTask more efficiently.
      // Killing it is not an option, we should really use the isCancelled() method
      // for this. But we would rather not have to wait until an entire feed is
      // processed before we can stop it.
      while (running) {
        //Log.d(TAG, "Downloading initiated...");
        // Set the maximum number of retries for fetching and parsing
        int maxTries = 5;
        int tries = 0;
        int count;
        try {
          String path = "[INSERT YOUR WEB HOST HERE]/locations.json";
          URL u;

          try {
            u = new URL(path);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.connect();
            InputStream in = c.getInputStream();
            mLocationsJson = convertStreamToString(in);
            mFetchedLocations = parseLocations(mLocationsJson);

            /*
              JSON-logging
              longStringLogging(mLocationsJson);
            */
            mIsSuccessfullySynced = true;
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }

          File file;
          /*
            Compare fetched locations with local ones, and determine whether a new floor map
            download is needed.
           */
          for (Location newLocation : mFetchedLocations) {
            // Loop arrayList1 items
            //boolean found = false;
            if (mStoredLocations != null && mStoredLocations.size() > 0) {
              for (Location oldLocation : mStoredLocations) {
                if (newLocation.getId() == oldLocation.getId()) {
                  if (newLocation.getLastUpdate().contains(oldLocation.getLastUpdate())) {
                    // No new updates on this location found...
                  } else {
                    file = new File(getExternalFilesDir(null), oldLocation.getPlanImage());
                    // We need to download a floor map...
                    long length = file.length();
                    if (length > 0) {
                      // A floor map already existed, so we delete it to prepare for another
                      // download
                      file.delete();
                    }
                  }
                }
              }
            }

            file = new File(getExternalFilesDir(null), newLocation.getPlanImage());
            long length = file.length();
            if (!newLocation.getPlanImage().isEmpty()) {
              // There is a floor map available for this location, so let's download it.
              URL url = new URL("[INSERT YOUR WEB HOST HERE]/" + newLocation.getPlanImage());
              URLConnection connection = url.openConnection();
              connection.connect();
              int sourceLenghtOfFile = connection.getContentLength();
              if (sourceLenghtOfFile != length) {
                // Input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                // Output stream to write file
                FileOutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];
                while ((count = input.read(data)) != -1) {
                  // Writing data to file
                  output.write(data, 0, count);
                }

                // Flushing output
                output.flush();

                // Closing streams
                output.close();
                input.close();
              } else {
                // Remote and local are the same. No-op.
              }
            }
            running = false;
          }
        } catch(Exception e) {
          tries++;
          if(tries == maxTries) {
            running = false;
          }
          e.printStackTrace();
        }
      }

      return null;
    }

    @Override
    protected void onPostExecute(Boolean asynchDownloadSuccess) {
      running = false;
      if(!mIsSuccessfullySynced) {
        // If we couldn't properly save and process data from the web service, we use the local data
        // we already have
        mLocations = mStoredLocations;
      } else {
        // If we managed to properly save and process data from the web service, we put it in
        // external storage
        File file = new File(getExternalFilesDir(null), "locations.json");
        try {
          FileOutputStream outputStream = new FileOutputStream(file);
          outputStream.write(mLocationsJson.getBytes());
          outputStream.close();
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
        mLocations = mFetchedLocations;
      }
      refreshAdapter();
      if(progressDialog != null) {
        progressDialog.dismiss();
        mSharedPreferences.edit().putBoolean("first_run", false).apply();
      }
    }

    @Override
    protected void onCancelled() {
      super.onCancelled();
      running = false;
    }

    /**
     * Creates a string from an InputStream
     *
     * @param is InputStream The stream to process
     * @return String The resulting string
     * @throws UnsupportedEncodingException
     */
    private String convertStreamToString(InputStream is) throws UnsupportedEncodingException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      StringBuilder sb = new StringBuilder();
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return sb.toString();
    }
  }
}
