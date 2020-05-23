package com.example.hw2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener,
    SensorEventListener{

    List<Marker> markerList;
    List<String> positionList;

    private GoogleMap mMap;
    private SensorManager sensorManager;
    Sensor accelerometer;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Create an instance of FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>(); //Initialize markerList
        positionList = new ArrayList<>(); //Initialize positionList

        TextView text = (TextView) findViewById(R.id.textView);
        FloatingActionButton dot = findViewById(R.id.floatDot);
        FloatingActionButton stop = findViewById(R.id.floatStop);

        text.setVisibility(View.INVISIBLE);
        dot.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MapsActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        restoreFromJson();
    }

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    //private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    private final String MARKERS_JSON_FILE = "markers.json";



    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request the missing permissions
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //Add a marker on the last known location
                if(location != null && mMap !=null) {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        //Add a custom marker at the position of the long click
        Marker marker = mMap.addMarker(new MarkerOptions()
            .position(new LatLng(latLng.latitude,latLng.longitude))
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
            .alpha(0.8f)
            //.title(String.format("Position:(%.2f, %.2f) Distance:%.2f",latLng.latitude,latLng.longitude,distance)));
            .title(String.format("Position:(%.2f, %.2f)",latLng.latitude,latLng.longitude)));
        //Add the marker to the list
        markerList.add(marker);
        positionList.add(Double.toString(latLng.latitude) + "," + Double.toString(latLng.longitude));
        saveMarkersToJson();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        bounceAnimation();
        return false;
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        //Request location updates with mLocationRequest and locationCallback
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback() {
        //create the locationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //Code executed when user's location changes
                if(locationResult != null) {
                    //Remove the last reported location
                    if (gpsMarker != null)
                        gpsMarker.remove();
                    //Add a custom marker to the map for location from the last locationResult
                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current Location"));
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View view) {
        //Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        //Zoom out the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private void saveMarkersToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(positionList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKERS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<String>>() {
            }.getType();
            List<String> o = gson.fromJson(readJson, collectionType);
            if (o != null) {
                markerList.clear();
                positionList.clear();

                for (String position : o) {
                    positionList.add(position);
                    double x = Double.parseDouble(position.substring(0, position.indexOf(",")));
                    double y = Double.parseDouble(position.substring(position.indexOf(",") + 1));

                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(x, y))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                            .alpha(0.8f)
                            .title(String.format("Position:(%.2f, %.2f)", x, y)));
                    markerList.add(marker);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void bounceTextAnimation() {
        TextView text = (TextView) findViewById(R.id.textView);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.bounce);

        text.startAnimation(animation);

        text.setVisibility(View.VISIBLE);
    }

    public void slideDownTextAnimation() {
        TextView text = (TextView) findViewById(R.id.textView);
        Animation animation = AnimationUtils.loadAnimation(this,R.anim.slide_down_text);

        text.startAnimation(animation);
        text.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        TextView text = (TextView) findViewById(R.id.textView);
        text.setText("Acceleration \n x: "+ sensorEvent.values[0] + ", y: " + sensorEvent.values[1]);
    }

    public void startAccelerometer(View view) {
        if (counter%2 != 0)
            bounceTextAnimation();
        if (counter%2 == 0)
            slideDownTextAnimation();
        counter += 1;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void clearMemory(View view) {
        markerList.clear();
        positionList.clear();
        mMap.clear();
        saveMarkersToJson();
    }

    public void bounceAnimation() {
        FloatingActionButton dot = findViewById(R.id.floatDot);
        FloatingActionButton stop = findViewById(R.id.floatStop);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.bounce);

        dot.startAnimation(animation);
        stop.startAnimation(animation);

        dot.setVisibility(View.VISIBLE);
        stop.setVisibility(View.VISIBLE);
    }

    public void slideDownAnimation(View view) {
        FloatingActionButton dot = findViewById(R.id.floatDot);
        FloatingActionButton stop = findViewById(R.id.floatStop);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        dot.startAnimation(animation);
        stop.startAnimation(animation);

        dot.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
    }
}
