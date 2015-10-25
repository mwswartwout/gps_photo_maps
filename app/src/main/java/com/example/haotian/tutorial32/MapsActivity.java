package com.example.haotian.tutorial32;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
//import android.location.LocationListener;
import com.google.android.gms.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.telecom.ConnectionRequest;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    public static final String TAG = "MapsActivity";
    public static final int THUMBNAIL = 1;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button picButton; //takes user to camera

    static final int REQUEST_IMAGE_CAPTURE_CODE = 1;

    private GoogleApiClient mGoogleApiClient;
    public Location mLastLocation;
    public double mLatitude;
    public double mLongitude;
    public LocationRequest mLocationRequest;
    public Location mCurrentLocation;
    public boolean mRequestingLocationUpdates = true;
    private File HW2dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/camera");
    private String HW2name = "Homework2.csv";
    public File Homework2 = new File(HW2dir,HW2name); // The Homework2.csv file is stored in DCIM/camera folder

    private TextView displaylatitude;
    private TextView displaylongitude;
    private ImageView mImageView;

    public EditText mEditTitle;
    public EditText mEditSnippet;
    public View MarkerInfoEditView;
    public String mTitle = "Edit title";
    public String mSnippet = "Edit Snippet";
    public Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        picButton = (Button) findViewById(R.id.photobutton);
        displaylatitude = (TextView) findViewById(R.id.displaylatitude);
        displaylongitude = (TextView) findViewById(R.id.displaylongitude);


        picButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        if (!Homework2.exists()){
            try {
                Homework2.createNewFile();
                BufferedWriter bfw = new BufferedWriter(new FileWriter(Homework2,true));
                bfw.write("TimeStamp,Latitude,Longitude\n");
                bfw.close();
            } catch (IOException e){
                Log.d("MapsActivity","Homework2.csv file failed to create");
                e.printStackTrace();
            }
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                Dialog MarkerInfo = MarkerInfoDialog(marker);

                MarkerInfo.show();

                marker.hideInfoWindow();

                return false;
            }
        });

        mMap.setMyLocationEnabled(true);
        buildGoogleApiClient();
        createLocationRequest();

    }

    private Dialog MarkerInfoDialog(Marker marker) {
        mMarker = marker;

        mTitle = mMarker.getTitle();
        mSnippet = mMarker.getSnippet();

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //MarkerInfoEditView = inflater.inflate(R.layout.edit_markerinfo, null);
        //mEditTitle.setText(mTitle, TextView.BufferType.NORMAL);
        //mEditSnippet.setText(mSnippet,TextView.BufferType.NORMAL);

        builder.setTitle("Marker Info editor")
                .setCancelable(true)
                .setView(inflater.inflate(R.layout.edit_markerinfo,null))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog f = (Dialog) dialog;

                        mEditTitle = (EditText) f.findViewById(R.id.title);
                        mEditSnippet = (EditText) f.findViewById(R.id.snippet);

                        mTitle = mEditTitle.getText().toString();
                        mSnippet = mEditSnippet.getText().toString();

                        mMarker.setTitle(mTitle);
                        mMarker.setSnippet(mSnippet);

                        mMarker.showInfoWindow();
                        dialog.dismiss();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        return builder.create();
    }


    //create the desired image name and directory to save for the next taken photo
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera");
        if (storageDir == null) {
            storageDir.mkdir();
        }
        File image = File.createTempFile(imageFileName,".jpg",storageDir);
        return image;
    }

    //Start camera activity to take a photo
    private  void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File tempdir = null;
        if (takePictureIntent.resolveActivity(getPackageManager())!=null){
            /*
            try {
                tempdir = createImageFile();
            } catch (IOException e){
                e.printStackTrace();
            }
            if ( tempdir != null  ){
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempdir));
            }
            */
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_CODE);
        }
    }

    @Override
    //do Writing CSV file for homework 2
    protected void onActivityResult (int requestCode, int resultCode, Intent data)  {

        //super.onActivityResult(requestCode,resultCode,data);

        if (requestCode == REQUEST_IMAGE_CAPTURE_CODE){
            if (resultCode == RESULT_OK ){
                // photo is captured, save timestamp and the last available coordinate data to the CSV file
                if (mCurrentLocation != null) {
                    double latitude = mCurrentLocation.getLatitude();
                    double longitude = mCurrentLocation.getLongitude();

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String mRowInput = timeStamp + "," + latitude + "," + longitude + "\n";

                    try {
                        BufferedWriter bfw1 = new BufferedWriter(new FileWriter(Homework2, true));
                        bfw1.write(mRowInput);
                        bfw1.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Bundle extras = data.getExtras();
                    if (extras.keySet().contains("data")) {

                        try {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");

                            //save imageBitmap to storage
                            String imageFileName = "JPEG_" + timeStamp + "_.jpg";
                            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera");

                            File image = new File(storageDir,imageFileName);
                            FileOutputStream out = new FileOutputStream(image);
                            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            out.flush();
                            out.close();

                            mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
                                            .icon(BitmapDescriptorFactory.fromBitmap(imageBitmap))
                            );
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                        //mImageView.setImageBitmap(imageBitmap);
                    }
                }

            }else if  (resultCode == RESULT_CANCELED){
                // User cancelled the image capture
            }else {
                // Image capture failed, make some advices for the user
            }
        }
    }


/*
    @Override
    public boolean onMarkerClick (Marker marker){

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

        LayoutInflater inflater = getLayoutInflater();

        builder.setTitle("Marker Info editor")
                .setView(inflater.inflate(R.layout.edit_markerinfo,null))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        builder.create().show();

        mTitle = mEditTitle.getText().toString();
        mSnippet = mEditSnippet.getText().toString();

        marker.setTitle(mTitle);
        marker.setSnippet(mSnippet);

        return false;
    }
    */





    //Connect with the GoogleApiClient
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();
    }

    //Create locationrequest
    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //Get last known location & start location updates
    @Override
    public void onConnected(Bundle bundle){
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();
        }

        if (mRequestingLocationUpdates){
            startLocationUpdates();
        }

    }

    //startLocationUpdates in onResume() and onConnected()
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mRequestingLocationUpdates = false;
    }

    //stopLocationUpdates in onPause()
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        mRequestingLocationUpdates = true;
    }

    @Override
    public void onLocationChanged (Location location){

        mCurrentLocation = location;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        displaylatitude.setText(String.format("%.5f",latitude));
        displaylongitude.setText(String.format("%.5f",longitude));

    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();

        //if(mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
          //  startLocationUpdates();
       // }

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(20, 20)).title("EECS397/600"));
    }

    //must implement abstract method onConnectionFailed()
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){

    }

    //must implement abstract method onConnectionSuspended()
    @Override
    public void onConnectionSuspended(int i){

    }

        /*
    @Override
    public void onProviderEnabled(String string){

    }

    @Override
    public void onProviderDisabled(String string){

    }

    @Override
    public void onStatusChanged(String string,int i, Bundle bundle){
    }
    */

}
