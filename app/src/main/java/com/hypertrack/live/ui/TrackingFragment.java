package com.hypertrack.live.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.hypertrack.live.GpsWorkStatusObserver;
import com.hypertrack.live.R;
import com.hypertrack.live.map.mylocation.MyLocationGoogleMap;
import com.hypertrack.sdk.HyperTrack;
import com.hypertrack.sdk.TrackingInitDelegate;
import com.hypertrack.sdk.TrackingInitError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TrackingFragment extends SupportMapFragment
        implements GpsWorkStatusObserver.OnStatusChangedListener {
    private static final String TAG = TrackingFragment.class.getSimpleName();

    private Snackbar turnOnLocationSnackbar;
    private FloatingActionButton trackingButton;
    private FloatingActionButton locationButton;
    private View trackingButtonTips;
    private Button shareButton;
    private LoaderDecorator loader;

    private GoogleMap mMap;
    private GpsWorkStatusObserver gpsWorkStatusObserver;
    private MyLocationGoogleMap myLocationGoogleMap;
    private TrackingInitDelegate trackingInitDelegate = new TrackingInitDelegate() {
        @Override
        public void onError(@NonNull TrackingInitError trackingInitError) {
            Log.e(TAG, "Initialization failed with error");
        }

        @Override
        public void onSuccess() {
            onStartTracking();
        }
    };

    private String hyperTrackPublicKey;

    public static Fragment newInstance(String hyperTrackPublicKey) {
        TrackingFragment fragment = new TrackingFragment();
        Bundle bundle = new Bundle();
        bundle.putString("HYPER_TRACK_PUBLIC_KEY", hyperTrackPublicKey);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            hyperTrackPublicKey = bundle.getString("HYPER_TRACK_PUBLIC_KEY");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentLayout = inflater.inflate(R.layout.fragment_tracking, null);
        FrameLayout frameLayout = fragmentLayout.findViewById(R.id.content_frame);
        frameLayout.addView(super.onCreateView(inflater, container, savedInstanceState));
        return fragmentLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gpsWorkStatusObserver = new GpsWorkStatusObserver(getContext());
        gpsWorkStatusObserver.register(this);

        myLocationGoogleMap = new MyLocationGoogleMap(getActivity());

        trackingButton = view.findViewById(R.id.trackingButton);
        trackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(getString(R.string.app_name), Activity.MODE_PRIVATE).edit();
                if (HyperTrack.isTracking()) {
                    HyperTrack.stopTracking();
                    onStopTracking();
                    editor.putBoolean("is_tracking", false).commit();
                } else {
                    if (gpsWorkStatusObserver.isGpsEnabled()) {
                        HyperTrack.startTracking(true, trackingInitDelegate);
                        editor.putBoolean("is_tracking", true).commit();
                    } else {
                        onGpsStopped();
                    }
                }
            }
        });
        locationButton = view.findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myLocationGoogleMap.moveToMyLocation(mMap);
            }
        });
        trackingButtonTips = view.findViewById(R.id.trackingButtonTips);
        shareButton = view.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTracking();
            }
        });

        loader = new LoaderDecorator(getContext());

        if (gpsWorkStatusObserver.isGpsEnabled()) {
            if (HyperTrack.isTracking()) {
                onStartTracking();
            } else {
                onStopTracking();
            }
        } else {
            onGpsStopped();
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(getActivity(),
//                    new String[]{Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS},
//                    MainActivity.PERMISSIONS_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//        }
    }

    private void updateMap(GoogleMap googleMap) {
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setCompassEnabled(HyperTrack.isTracking());
        if (getActivity() != null) {
            int mapStyle = gpsWorkStatusObserver.isGpsEnabled() ? R.raw.style_map : R.raw.style_map_silver;
            try {
                // Customise the styling of the base map using a JSON object defined
                // in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                getActivity(), mapStyle));

                if (!success) {
                    Log.e(TAG, "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Can't find style. Error: ", e);
            }
        }
    }

    private void onStartTracking() {
        Log.e("onStartTracking", "call");
        trackingButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
        trackingButton.setImageResource(R.drawable.ic_on);
        locationButton.show();
        shareButton.setEnabled(true);
        trackingButtonTips.setVisibility(View.GONE);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateMap(googleMap);
                myLocationGoogleMap.addTo(googleMap);
            }
        });
    }

    private void onStopTracking() {
        Log.e("onStopTracking", "call");
        trackingButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        trackingButton.setImageResource(R.drawable.ic_on_disabled);
        locationButton.hide();
        shareButton.setEnabled(false);
        trackingButtonTips.setVisibility(View.VISIBLE);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                myLocationGoogleMap.removeFrom(googleMap);
                updateMap(googleMap);
            }
        });
    }

    private void shareTracking() {
        loader.start();
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getContext());
        final String shareUrl = "https://trck.at/%s";
        String url = "https://7kcobbjpavdyhcxfvxrnktobjm.appsync-api.us-west-2.amazonaws.com/graphql";

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject("{\n" +
                    "  \"query\": \"query getPublicTrackingIdQuery($publishableKey: String!, $deviceId: String!){\\\\\n  getPublicTrackingId(publishable_key: $publishableKey, device_id: $deviceId){\\\\\n    tracking_id\\\\\n  }\\\\\n}\"," +
                    "  \"variables\": {" +
                    "    \"publishableKey\": \"" + hyperTrackPublicKey + "\",\n" +
                    "    \"deviceId\": \"" + HyperTrack.getDeviceId() + "\"" +
                    "  }," +
                    "  \"operationName\": \"getPublicTrackingIdQuery\"" +
                    "}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // Request a json response from the provided URL.
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loader.stop();
                        // Display the first 500 characters of the response string.
                        try {
                            String trackingId = response.getJSONObject("data")
                                    .getJSONObject("getPublicTrackingId")
                                    .getString("tracking_id");
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(shareUrl, trackingId));
                            sendIntent.setType("text/plain");
                            startActivity(sendIntent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loader.stop();
                error.printStackTrace();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Api-Key", "da2-nt5vwlflmngjfbe6cbsone4emm");
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    @Override
    public void onGpsStarted() {
        if (getActivity() != null && getActivity().getSharedPreferences(getString(R.string.app_name), Activity.MODE_PRIVATE)
                .getBoolean("is_tracking", false)) {
            HyperTrack.startTracking(true, trackingInitDelegate);
        }
        if (turnOnLocationSnackbar != null) turnOnLocationSnackbar.dismiss();
    }

    @Override
    public void onGpsStopped() {
        HyperTrack.stopTracking();
        onStopTracking();
        trackingButtonTips.setVisibility(View.GONE);
        turnOnLocationSnackbar = Snackbar.make(trackingButton, "", Snackbar.LENGTH_INDEFINITE)
                .setAction("Tap to turn on location in settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        turnOnLocationSnackbar.setActionTextColor(Color.WHITE);
        turnOnLocationSnackbar.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gpsWorkStatusObserver.unregister(this);
        myLocationGoogleMap.removeFrom(mMap);
    }
}
