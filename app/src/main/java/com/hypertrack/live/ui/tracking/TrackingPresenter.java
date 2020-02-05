package com.hypertrack.live.ui.tracking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.Tokens;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.wrappers.InstantApps;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.hypertrack.live.BuildConfig;
import com.hypertrack.live.R;
import com.hypertrack.live.ui.places.SearchPlaceFragment;
import com.hypertrack.live.utils.AppUtils;
import com.hypertrack.live.utils.MapUtils;
import com.hypertrack.live.utils.SimpleLocationListener;
import com.hypertrack.maps.google.widget.GoogleMapAdapter;
import com.hypertrack.maps.google.widget.GoogleMapConfig;
import com.hypertrack.sdk.AsyncResultHandler;
import com.hypertrack.sdk.HyperTrack;
import com.hypertrack.sdk.TrackingError;
import com.hypertrack.sdk.TrackingStateObserver;
import com.hypertrack.sdk.views.DeviceUpdatesHandler;
import com.hypertrack.sdk.views.HyperTrackViews;
import com.hypertrack.sdk.views.QueryResultHandler;
import com.hypertrack.sdk.views.dao.Location;
import com.hypertrack.sdk.views.dao.StatusUpdate;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.sdk.views.maps.GpsLocationProvider;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.hypertrack.sdk.views.maps.Predicate;
import com.hypertrack.sdk.views.maps.TripSubscription;
import com.hypertrack.trips.AsyncTokenProvider;
import com.hypertrack.trips.ResultHandler;
import com.hypertrack.trips.ShareableTrip;
import com.hypertrack.trips.TripConfig;
import com.hypertrack.trips.TripsManager;

import io.reactivex.disposables.CompositeDisposable;

@SuppressWarnings("WeakerAccess")
class TrackingPresenter implements DeviceUpdatesHandler {
    private static final String TAG = "TrackingPresenter";

    private final View view;
    private final TrackingState state;

    private final Handler handler = new Handler();
    private final Context context;
    private final HyperTrack hyperTrack;
    private final TripsManager tripsManager;
    private final HyperTrackViews hyperTrackViews;
    private GoogleMap googleMap;
    private HyperTrackMap hyperTrackMap;
    private GoogleMapConfig mapConfig;
    private TrackingStateObserver.OnTrackingStateChangeListener onTrackingStateChangeListener;

    private Marker destinationMarker;
    private TripSubscription tripSubscription;

    private boolean mapDestinationMode = false;

    protected final CompositeDisposable disposables = new CompositeDisposable();

    public TrackingPresenter(@NonNull Context context, @NonNull final View view, @NonNull String hyperTrackPubKey) {
        this.context = context.getApplicationContext() == null ? context : context.getApplicationContext();
        this.view = view;
        state = new TrackingState(this.context, hyperTrackPubKey);

        hyperTrack = HyperTrack.getInstance(context, hyperTrackPubKey);
        hyperTrackViews = HyperTrackViews.getInstance(context, state.getHyperTrackPubKey());
        tripsManager = TripsManager.getInstance(context, new AsyncTokenProvider() {
            @Override
            public void getAuthenticationToken(@NonNull final ResultHandler<String> resultHandler) {
                AWSMobileClient.getInstance().getTokens(new Callback<Tokens>() {
                    @Override public void onResult(Tokens result) {
                        resultHandler.onResult(result.getIdToken().getTokenString());
                    }

                    @Override public void onError(Exception e) { resultHandler.onError(e); }
                });
            }
        });

        mapConfig = MapUtils.getBuilder(context).build();
    }

    public void initMap(@NonNull GoogleMap googleMap) {

        this.googleMap = googleMap;
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(final LatLng latLng) {
                if (!mapDestinationMode) {
                    updateDestination(latLng);
                }
            }
        });

        onTrackingStateChangeListener = new TrackingStateObserver.OnTrackingStateChangeListener() {
            @Override
            public void onError(TrackingError trackingError) {
                view.onError(trackingError);
            }

            @Override
            public void onTrackingStart() {
                view.onTrackingStart();
            }

            @Override
            public void onTrackingStop() {
                view.onTrackingStop();
            }
        };
        hyperTrack.addTrackingListener(onTrackingStateChangeListener);
        hyperTrackViews.subscribeToDeviceUpdates(hyperTrack.getDeviceID(), this);
        GoogleMapAdapter mapAdapter = new GoogleMapAdapter(googleMap, mapConfig);
        mapAdapter.addTripFilter(new Predicate<Trip>() {
            @Override
            public boolean apply(Trip trip) {
                return trip.getTripId().equals(state.getTripId());
            }
        });
        hyperTrackMap = HyperTrackMap.getInstance(context, mapAdapter)
                .bind(new GpsLocationProvider(context));

        if (TextUtils.isEmpty(state.getTripId())) {
            hyperTrackMap.setLocationUpdatesListener(new SimpleLocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {
                    hyperTrackMap.moveToMyLocation();
                    hyperTrackMap.setLocationUpdatesListener(null);
                }
            });
        } else {
            view.showProgressBar();

            hyperTrackViews.getTrip(state.getTripId(), new com.hypertrack.sdk.views.QueryResultHandler<Trip>() {
                @Override
                public void onQueryResult(Trip trip) {
                    view.hideProgressBar();

                    state.setShareableUrl(trip.getViews().getSharedUrl());
                    hyperTrackMap.moveToTrip(trip);
                    if (trip.getStatus().equals("active")) {
                        startHyperTrackTracking();
                        view.showTripInfo(trip);
                    } else {
                        view.showTripSummaryInfo(trip);
                    }
                    tripSubscription = hyperTrackMap.subscribeTrip(state.getTripId());
                }

                @Override
                public void onQueryFailure(Exception e) {
                    Log.e(TAG, "get trip failure", e);
                    view.hideProgressBar();
                }
            });
        }

        if (BuildConfig.DEBUG) {
            hyperTrack.start();
        }
    }

    public void resume() {
        if (AppUtils.isGpsProviderEnabled(context)) {
            view.onTrackingStart();
        } else {
            view.onTrackingStop();
        }
    }

    public void setCameraFixedEnabled(boolean enabled) {
        if (hyperTrackMap != null) {
            if (TextUtils.isEmpty(state.getTripId())) {
                hyperTrackMap.moveToMyLocation();
            } else {
                hyperTrackMap.adapter().setCameraFixedEnabled(enabled);
            }
        }
    }

    public void setMyLocationEnabled(boolean enabled) {
        if (hyperTrackMap != null) {
            hyperTrackMap.setMyLocationEnabled(enabled);
        }
    }

    private void startHyperTrackTracking() {
        if (AppUtils.isGpsProviderEnabled(context)) {
            hyperTrackMap.bind(hyperTrackViews, hyperTrack.getDeviceID());
            hyperTrack.start();
        } else {
            actionLocationSourceSettings();
        }
    }

    private void stopHyperTrackTracking() {
        hyperTrackMap.unbindHyperTrackViews();
        hyperTrack.stop();
    }

    public void share() {
        if (mapDestinationMode) {
            stopMapDestinationMode();
            updateDestination(googleMap.getCameraPosition().target);
            view.popBackStack();
            startTrip();
            return;
        }

        if (state.getDestination() == null) {
            view.addFragment(new SearchPlaceFragment());
        } else {
            startTrip();
        }
    }

    public void shareHyperTrackUrl() {
        String url = state.getShareableUrl();
        if (!TextUtils.isEmpty(url)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, url);
            sendIntent.setType("text/plain");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(sendIntent);
        }
    }

    public void startTrip() {
        view.showProgressBar();

        ResultHandler<ShareableTrip> resultHandler = new ResultHandler<ShareableTrip>() {
            @Override
            public void onResult(@NonNull ShareableTrip trip) {
                updateDestination(null);

                state.setTripId(trip.getTripId());
                state.setShareableUrl(trip.getShareUrl());
                shareHyperTrackUrl();

                tripSubscription = hyperTrackMap.subscribeTrip(trip.getTripId());
                hyperTrackViews.getTrip(trip.getTripId(), new QueryResultHandler<Trip>() {
                    @Override public void onQueryResult(Trip trip) {
                        view.hideProgressBar();

                        view.showTripInfo(trip);
                        hyperTrackMap.moveToTrip(trip);
                        startHyperTrackTracking();
                    }

                    @Override public void onQueryFailure(Exception e) {
                        Log.e(TAG, "start trip failure", e);
                        view.hideProgressBar();
                    }
                });

            }

            @Override
            public void onError(@NonNull Exception error) {
                Log.e(TAG, "start trip failure", error);
                view.hideProgressBar();

            }

        };
        TripConfig tripRequest;
        if (state.getDestination() == null) {
            tripRequest = new TripConfig.Builder().build();
        } else {
            tripRequest = new TripConfig.Builder()
                    .setDestinationLatitude(state.getDestination().latitude)
                    .setDestinationLongitude(state.getDestination().longitude)
                    .build();
        }
        tripsManager.createTrip(tripRequest, resultHandler);
    }

    public void endTrip() {
        view.showProgressBar();

        tripsManager.completeTrip(state.getTripId(), new ResultHandler<String>() {

            @Override public void onResult(@NonNull String result) {
                view.hideProgressBar();
                stopHyperTrackTracking();
            }
            @Override public void onError(@NonNull Exception error) {
                view.hideProgressBar();
                Log.e(TAG, "complete trip failure", error);
            }
        });
    }

    public void removeTrip() {
        state.setTripId(null);
        state.setShareableUrl(null);
        if (tripSubscription != null) {
            tripSubscription.remove();
            tripSubscription = null;
        }
        hyperTrackMap.adapter().notifyDataSetChanged();
        view.dismissTrip();
    }

    public void actionLocationSourceSettings() {
        if (!InstantApps.isInstantApp(context)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TrackingFragment.AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                if (data.getExtras() != null && data.getExtras().get(SearchPlaceFragment.SELECTED_PLACE_KEY) != null) {
                    Place place = (Place) data.getExtras().get(SearchPlaceFragment.SELECTED_PLACE_KEY);
                    updateDestination(place.getLatLng());
                    if (hyperTrackMap != null && place.getLatLng() != null) {
                        hyperTrackMap.moveToLocation(place.getLatLng().latitude, place.getLatLng().longitude);
                    }
                } else {
                    updateDestination(null);
                    startTrip();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {

                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null) Log.i(TAG, status.getStatusMessage());
            }
        } else if (requestCode == TrackingFragment.SET_ON_MAP_REQUEST_CODE) {
            startMapDestinationMode();
        }
    }

    private void updateDestination(LatLng latLng) {
        state.setDestination(latLng);
        if (latLng == null) {
            if (destinationMarker != null) {
                destinationMarker.remove();
            }
        } else {
            if (destinationMarker == null) {
                destinationMarker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination))
                );
            } else {
                destinationMarker.setPosition(latLng);
            }
        }
    }

    private void startMapDestinationMode() {
        mapDestinationMode = true;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                disposables.add(MapUtils.getAddress(context, googleMap.getCameraPosition().target)
                        .subscribe(new io.reactivex.functions.Consumer<String>() {
                            @Override
                            public void accept(String s) {
                                view.onDestinationChanged(s);
                            }
                        }));
            }
        };
        googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
            }
        });
    }

    private void stopMapDestinationMode() {
        mapDestinationMode = false;
        googleMap.setOnCameraMoveCanceledListener(null);
    }

    public void destroy() {
        if (hyperTrackMap != null) {
            hyperTrackMap.destroy();
            hyperTrackMap = null;
        }

        hyperTrack.removeTrackingListener(onTrackingStateChangeListener);
        hyperTrackViews.stopAllUpdates();
        disposables.clear();
    }

    @Override
    public void onLocationUpdateReceived(@NonNull Location location) {

    }

    @Override
    public void onBatteryStateUpdateReceived(int i) {

    }

    @Override
    public void onStatusUpdateReceived(@NonNull StatusUpdate statusUpdate) {

    }

    @Override
    public void onTripUpdateReceived(@NonNull Trip trip) {
        if (trip.getTripId().equals(state.getTripId())) {
            if (trip.getStatus().equals("completed")) {
                view.showTripSummaryInfo(trip);
            } else {
                view.showTripInfo(trip);
            }
            state.setShareableUrl(trip.getViews().getSharedUrl());
        }
    }

    @Override
    public void onError(@NonNull Exception e, @NonNull String s) {

    }

    @Override
    public void onCompleted(@NonNull String s) {

    }

    public interface View extends TrackingStateObserver.OnTrackingStateChangeListener {

        void onDestinationChanged(String address);

        void showTripInfo(Trip trip);

        void showTripSummaryInfo(Trip trip);

        void dismissTrip();

        void showProgressBar();

        void hideProgressBar();

        void addFragment(Fragment fragment);

        void popBackStack();
    }
}
