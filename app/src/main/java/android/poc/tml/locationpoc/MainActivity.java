package android.poc.tml.locationpoc;
//APP IMPORTS
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;

//PLAYS SERVICES IMPORTS
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

//Reverse GEOCODING
import java.util.List;
import java.util.Locale;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    //DEBUG OPTIONS
    private static final String TAG = MainActivity.class.getSimpleName();

    //SET TIMINGS FOR UPDATES
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private static int UPDATE_INTERVAL = 10000;
    private static int FASTEST_INTERVAL = 5000;
    private static int displacement = 10;

    //VARIABLES FOR APIS
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

   //APP VARIABLES
    private String locationAddress;
    private boolean mRequestLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private TextView lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lblLocation = (TextView) findViewById(R.id.lblLocation);
       // btnShowLocation = (Button) findViewById(R.id.showlocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.showlocationupdates);

//CHECK IF APP HAS PERMISSIONS - ANDROID 6
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        //CHECK FOR AVAILABILITY OF PLAYSERVICES
        if (checkPlayServices()) {
            buildGoogleApiClient();
            createLocationRequest();
        }

/*        btnShowLocation.setOnClickListener(new View.OnClickListener() {

                                               public void onClick(View v) {
                                                   displayLocation();
                                               }

                                           }
        );*/

        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLocationUpdate();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setType("text/plain");

                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"pranav.j@tatamotors.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Location Update");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Location Update: Lat: " + mLastLocation.getLongitude() + "Long : " +mLastLocation.getLatitude()+  locationAddress);

                Snackbar.make(view, "Sharing Location", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startActivity(emailIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    protected void onResume() {
        super.onResume();

       CheckEnableGPS();
       CheckEnableInternet();

        //turnGPSOn();
        checkPlayServices();
        if (mGoogleApiClient.isConnected() && mRequestLocationUpdates) {
            startLocationUpdates();

        }
    }

    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }


    public String getMyLocationAddress(double Latitude, Double Longitude) {
        String myAddress = null;
        Geocoder geocoder= new Geocoder(this, Locale.ENGLISH);

        try {

            //Place your latitude and longitude
            List <Address> addresses = geocoder.getFromLocation(Latitude,Longitude, 1);

            if(addresses != null) {

                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                }

               myAddress = "I am at: " +strAddress.toString();
            }
            else
                myAddress = "No location found..!";
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Could not get address..!", Toast.LENGTH_LONG).show();
        }
        return myAddress;
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation!= null)
        {
            double latitude =mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();


          locationAddress = getMyLocationAddress(latitude,longitude);
            lblLocation.setText("Loc Coordinates: "+ latitude + "," + longitude + "Location in Words: "+ locationAddress);

        } else {
            lblLocation.setText("Could not get Location. Enable Location");
        }
    }

    private void toggleLocationUpdate()
    {
        if(!mRequestLocationUpdates)
        {
            btnStartLocationUpdates.setText(getString(R.string.btn_stop_location_updates));
            mRequestLocationUpdates=true;
            startLocationUpdates();
        }
        else
        {
            btnStartLocationUpdates.setText(getString(R.string.btn_start_location_updates));
            mRequestLocationUpdates = false;
            stopLocationUpdates();
        }

    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

    }

    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(displacement);

    }

    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!= ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }else
            {
                Toast.makeText(getApplicationContext(),"This device is not Supported", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void startLocationUpdates() {
        try {
                       LocationServices.FusedLocationApi.requestLocationUpdates(
                               mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e)
        {
            e.printStackTrace();
        }
    }

    protected void stopLocationUpdates()
    {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        catch  (Exception e )
        {
           e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        displayLocation();

        if(mRequestLocationUpdates){
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
            mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
        Toast.makeText(MainActivity.this, "Location Changed", Toast.LENGTH_SHORT).show();
    }

    //CHECK IF GPS IS ENABLED OR NOT
    private boolean CheckEnableGPS(){
        String provider = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.equals("")){
            //GPS Enabled
            Toast.makeText(this, "GPS Enabled: " + provider,
                    Toast.LENGTH_LONG).show();
            return true;
        }else{

            // GPS NOT ENABLED. Show Dialogue and take user to settings

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("GPS not enabled. Please enable GPS");

            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(viewIntent);
                }
            });
            alertDialogBuilder.show();
            return false;
        }

    }

    private boolean CheckEnableInternet()
    {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if(!(netInfo != null && netInfo.isConnectedOrConnecting()))
        {
           /* AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Internet Not Enabled");

            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Intent viewIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(viewIntent);
                }
            });
            alertDialogBuilder.show();*/

            Toast.makeText(this, "Please enable Internet",
                    Toast.LENGTH_SHORT).show();
        }

        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    /*
    //<editor-fold desc="Description">
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    //</editor-fold>
    */

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG,"Connection Failed:" + connectionResult.getErrorCode());

    }
}


