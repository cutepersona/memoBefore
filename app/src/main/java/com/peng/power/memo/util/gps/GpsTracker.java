package com.peng.power.memo.util.gps;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;


public class GpsTracker extends Service implements LocationListener {

    private final Context mContext;
    Location mLocation;
    double latitude;
    double longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    protected LocationManager locationManager;


    public GpsTracker(Context context) {
        this.mContext = context;
    }


    public Location getmLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {

            } else {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION);


                if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                        hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

                    ;
                } else
                    return null;


                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (locationManager != null)
                    {
                        mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (mLocation != null)
                        {
                            latitude = mLocation.getLatitude();
                            longitude = mLocation.getLongitude();
                        }
                    }
                }


                if (isGPSEnabled)
                {
                    if (mLocation == null)
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null)
                        {
                            mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (mLocation != null)
                            {
                                latitude = mLocation.getLatitude();
                                longitude = mLocation.getLongitude();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Log.d("@@@", ""+e.toString());
        }

        return mLocation;
    }

    public double getLatitude()
    {
        if(mLocation != null)
        {
            latitude = mLocation.getLatitude();
        }

        return latitude;
    }

    public double getLongitude()
    {
        if(mLocation != null)
        {
            longitude = mLocation.getLongitude();
        }

        return longitude;
    }

    @Override
    public void onLocationChanged(Location location)
    {
//        DEBUG.d("# onLocationChanged ");
        mLocation = location;
    }

    @Override
    public void onProviderDisabled(String provider)
    {
//        DEBUG.d("# onProviderDisabled");
    }

    @Override
    public void onProviderEnabled(String provider)
    {
//        DEBUG.d("# onProviderEnabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
//        DEBUG.d("# onStatusChanged");
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }


    public void stopUsingGPS()
    {
        if(locationManager != null)
        {
            locationManager.removeUpdates(GpsTracker.this);
        }
    }
}