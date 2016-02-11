package com.skeptek.travelnotifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by tor611 on 2/11/16.
 */
public class LocationService extends Service {
    public static final String BROADCAST_ACTION = "Hello World";
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    private static final String TAG = "LOCATION_SERVICE";
    private static final double USA_N_LAT = 48.4943;
    private static final double USA_S_LAT = 32.5247;
    private static final double USA_W_LONG = -124.769;
    private static final double USA_E_LONG = -75.0100;


    Intent intent;
    int counter = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Log.d(TAG, "onStart Location Service");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();

        try {
            if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);

            if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        } catch (SecurityException se) {
            se.printStackTrace();
        }



    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }



    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }



    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        Log.v("STOP_SERVICE", "DONE");
        try {
            locationManager.removeUpdates(listener);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc) {
            Log.i("**************************************", "Location changed");

            loc.getLatitude();
            loc.getLongitude();
            Log.d(TAG, "LAT=" + loc.getLatitude());
            Log.d(TAG, "LONG=" + loc.getLongitude());

            // check if country is USA.  If not, send push notification
            if(isBetterLocation(loc, previousBestLocation)) {
                double lat = loc.getLatitude();
                double lng = loc.getLongitude();
                Log.d(TAG, "LAT=" + loc.getLatitude());
                Log.d(TAG, "LONG=" + loc.getLongitude());

                if(lat >=  USA_S_LAT && lat <= USA_N_LAT && lng >= USA_W_LONG && lng <= USA_E_LONG) {
                    Log.d(TAG, "USA Baby!!!");
                    //build notification and push to note bar.
                    Intent resultIntent = new Intent(LocationService.this, LocationService.class);
                    PendingIntent pi = PendingIntent.getActivity(LocationService.this,0,resultIntent,0);
                    Notification notification = new NotificationCompat.Builder(LocationService.this)
                                    .setSmallIcon(R.drawable.btn_check_buttonless_on)
                                    .setContentTitle("My notification")
                                    .setContentText("Hello World!")
                                    .setContentIntent(pi)
                                    .setAutoCancel(true)
                                    .build();

                    NotificationManager notificationManager = (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

                    notificationManager.notify(0, notification);

                } else {
                    Log.d(TAG, "Outside USA");
                }

                /*

                //use geocoder to get address from lat,long
                Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
                List<Address> addresses;

                try {
                    Log.d(TAG, "Getting addresses");
                    addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    Log.d(TAG, "Number of Addresses=" + addresses.size());
                    String country = "foo";
                    if(addresses.size() > 0)
                        country = addresses.get(0).getAddressLine(2);
                    Log.d(TAG, "COUNTRY=" + country);
                } catch (IOException e) {

                }
                */

                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);

            }
        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }
}
