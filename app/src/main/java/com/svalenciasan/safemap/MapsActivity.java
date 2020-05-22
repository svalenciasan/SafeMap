package com.svalenciasan.safemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity
        implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;
    /**
     * google map
     */
    private GoogleMap mMap;
    /**
     * mapFragment
     */
    SupportMapFragment mapFragment;
    /**
     * Clustering Manager
     */
    private ClusterManager<MyItem> mClusterManager;

    String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle bundle = getIntent().getExtras();
        type = bundle.getString("type");

         mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
         mapFragment.getMapAsync(this);
    }

    /**
     * Creates the map and markers.
     * @param map
     */
    @Override
    public void onMapReady(final GoogleMap map) {
        mMap = map;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.881832,-87.623177), 10.0f));
        if (type.equals("cluster")) {
            setupCluster();
            mMap.setOnCameraIdleListener(mClusterManager);
            mMap.setOnMarkerClickListener(mClusterManager);
        } else {
            setupHeat();
        }
    }

    private void setupCluster() {
        mClusterManager = new ClusterManager<>(this, mMap);
        final List<MyItem> list = new ArrayList<>();
        //Calendar Stuff to account for changes in the month
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currDate = dateFormat.format(calendar.getTime());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
        String currTime = hourFormat.format(calendar.getTime());
        String prevWeekDate = getCalculatedDate("yyyy-MM-dd", -30);
        String url = "https://data.cityofchicago.org/resource/ijzp-q8t2.json?$where=date between";
        url += " " + "\'" + prevWeekDate + "T00:00:00\' and \'" + currDate + "T" + currTime + "\'";

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject crime = null;
                            try {
                                crime = (JSONObject) response.get(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double latitude = 0;
                            try {
                                latitude = crime.getDouble("latitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double longitude = 0;
                            try {
                                longitude = crime.getDouble("longitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String description = null;
                            try {
                                description = crime.getString("description");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String primaryType = null;
                            try {
                                primaryType = crime.getString("primary_type");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            list.add(new MyItem(latitude, longitude, primaryType, description));
                        }
                        for (MyItem item: list) {
                            mClusterManager.addItem(item);
                        }
                        mClusterManager.setRenderer(new IconRenderer(MapsActivity.this, mMap, mClusterManager));
                        mClusterManager.cluster();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });
        request.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }
            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }
            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });

        // Add the request to the RequestQueue.
        queue.add(request);
    }

    /**
     * Heat Map Code
     */
    public void setupHeat() {
        final List<WeightedLatLng> list = new ArrayList<>();
        // Get the data: latitude/longitude positions of crimes.
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currDate = dateFormat.format(calendar.getTime());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
        String currTime = hourFormat.format(calendar.getTime());
        String prevWeekDate = getCalculatedDate("yyyy-MM-dd", -30);
        String url = "https://data.cityofchicago.org/resource/ijzp-q8t2.json?$where=date between";
        url += " " + "\'" + prevWeekDate + "T00:00:00\' and \'" + currDate + "T" + currTime + "\'";

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject crime = null;
                            try {
                                crime = (JSONObject) response.get(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double latitude = 0;
                            try {
                                latitude = crime.getDouble("latitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double longitude = 0;
                            try {
                                longitude = crime.getDouble("longitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            LatLng location = new LatLng(latitude, longitude);
                            WeightedLatLng weight = new WeightedLatLng(location, 20);
                            list.add(weight);
                        }
                        // Create the gradient.
                        int[] colors = {
                                Color.rgb(100, 0, 255),
                                Color.rgb(255, 0, 0)
                        };

                        float[] startPoints = {
                                0.2f, 1f
                        };

                        Gradient gradient = new Gradient(colors, startPoints);
                        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                                .weightedData(list)
                                .radius(25)
                                .opacity(1)
                                .gradient(gradient)
                                .build();
                        // Add a tile overlay to the map, using the heat map tile provider.
                        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });
        request.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }
            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }
            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        queue.add(request);
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Retrieve the data from the marker.
        String description = (String) marker.getTag();

        // Check if a click count was set, then display the click count.
        if (description != null) {
            AlertDialog.Builder popup = new AlertDialog.Builder(this);
            popup.setMessage(description);
            popup.setNegativeButton("Ok", null);
            popup.create().show();
        }

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
        // [END maps_check_location_permission]
    }

    /**
     * The button that centers the map on the location of the user.
     * @return
     */
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Current Location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    /**
     * Button that centers the map on the location of the user.
     * @param location
     */
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    /**
     * Handles request for permission to use location
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    // [START maps_check_location_permission_result]
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
            // [END_EXCLUDE]
        }
    }

    /**
     * Permission not granted to use location
     */
    // [END maps_check_location_permission_result]
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
    /**
     * Pass your date format and no of days for minus from current
     * If you want to get previous date then pass days with minus sign
     * else you can pass as it is for next date
     * @param dateFormat
     * @param days
     * @return Calculated Date
     */
    public static String getCalculatedDate(String dateFormat, int days) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat s = new SimpleDateFormat(dateFormat);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return s.format(new Date(cal.getTimeInMillis()));
    }

    /**
     * Creates Markers on the map, default
     * @param map
     */
    /**
    private void createMarkers(final GoogleMap map){
        //Calendar Stuff
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currDate = dateFormat.format(calendar.getTime());
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm:ss");
        String currTime = hourFormat.format(calendar.getTime());
        String prevWeekDate = getCalculatedDate("yyyy-MM-dd", -30);
        String url = "https://data.cityofchicago.org/resource/ijzp-q8t2.json?$where=date between";
        url += " " + "\'" + prevWeekDate + "T00:00:00\' and \'" + currDate + "T" + currTime + "\'";

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject crime = null;
                            try {
                                crime = (JSONObject) response.get(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double latitude = 0;
                            try {
                                latitude = crime.getDouble("latitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            double longitude = 0;
                            try {
                                longitude = crime.getDouble("longitude");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String description = null;
                            try {
                                description = crime.getString("description");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String primaryType = null;
                            try {
                                primaryType = crime.getString("primary_type");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Marker crimeMarker = map.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude,longitude))
                                    .title(primaryType));
                            crimeMarker.setTag(description);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });
        request.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }
            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }
            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });

        // Add the request to the RequestQueue.
        queue.add(request);
    }
     */
}