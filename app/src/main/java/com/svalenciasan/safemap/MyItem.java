package com.svalenciasan.safemap;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

public class MyItem implements ClusterItem {
    private final LatLng mPosition;
    private final String mTitle;
    private final String mSnippet;
    private BitmapDescriptor icon;

    public MyItem(double lat, double lng, String title, String snippet) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
        switch (mTitle) {
            case "THEFT":
            case "BURGLARY":
            case "MOTOR VEHICLE THEFT":
            case "ROBBERY":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.stealingicon);
                break;
            case "ARSON":
            case "CRIMINAL DAMAGE":
            case "CRIMINAL TRESPASS":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.damagesicon);
                break;
            case "OFFENSE INVOLVING CHILDREN":
            case "BATTERY":
            case "SEX OFFENSE":
            case "HOMICIDE":
            case "CRIMINAL SEXUAL ASSAULT":
            case "ASSAULT":
            case "STALKING":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.violenceicon);
                break;
            case "WEAPONS VIOLATION":
            case "NARCOTICS":
                icon = BitmapDescriptorFactory.fromResource(R.drawable.illegalpossessionicon);
                break;
            default:
                icon = BitmapDescriptorFactory.fromResource(R.drawable.othericon);
        }
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }

    public BitmapDescriptor getIcon() {
        return icon;
    }
}
