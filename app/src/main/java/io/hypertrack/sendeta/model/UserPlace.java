package io.hypertrack.sendeta.model;

import android.text.TextUtils;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;
import com.hypertrack.lib.internal.common.models.GeoJSONLocation;

import java.io.Serializable;

/**
 * Created by piyush on 10/06/16.
 */
public class UserPlace extends com.hypertrack.lib.models.Place implements Serializable {

    public final static String HOME = "Home";
    public final static String WORK = "Work";

    @SerializedName("google_places_id")
    private String googlePlacesID;

    public UserPlace() {

    }

    public UserPlace(Double latitude, Double longitude) {
        if (latitude == null || longitude == null)
            return;

        setLocation(new GeoJSONLocation(latitude, longitude));
    }


    public UserPlace(LatLng latLng) {
        if (latLng == null)
            return;
        setLocation(new GeoJSONLocation(latLng.latitude, latLng.longitude));
    }

    public UserPlace(String name, LatLng latLng) {
        this(latLng);
        setName(name);
    }

    public UserPlace(String name, Double latitude, Double longitude) {
        this(latitude, longitude);
        setName(name);
    }

    public UserPlace(UserPlace place) {
        this(place.getLatLng());
        setName(place.getName());
        this.googlePlacesID = place.getGooglePlacesID();
        setAddress(place.getAddress());
        setId(place.getId());

    }

    public UserPlace(Place place) {
        this(place.getName().toString(), place.getLatLng());
        this.googlePlacesID = place.getId();
        setAddress(place.getAddress().toString());
    }

    public UserPlace(String name) {
        setName(name);
    }


    public String getGooglePlacesID() {
        return googlePlacesID;
    }

    public void setGooglePlacesID(String googlePlacesID) {
        this.googlePlacesID = googlePlacesID;
    }


    public void update(UserPlace place) {

        setName(place.getName());
        this.googlePlacesID = place.getGooglePlacesID();
        setAddress(place.getAddress());
        setId(place.getId());
        setLocation(place.getLocation());
    }

    public boolean isHome() {
        if (TextUtils.isEmpty(getName()))
            return false;
        return getName().equalsIgnoreCase(HOME);
    }

    public boolean isWork() {
        if (TextUtils.isEmpty(getName()))
            return false;
        return getName().equalsIgnoreCase(WORK);
    }


    public boolean isEqualPlace(UserPlace place) {
        if (TextUtils.isEmpty(place.getId()) || TextUtils.isEmpty(getId())) {
            return false;
        }

        return (getId() == place.getId());
    }

    public LatLng getLatLng() {
        return new LatLng(getLocation().getLatitude(), getLocation().getLongitude());
    }

}
