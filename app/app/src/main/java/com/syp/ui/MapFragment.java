// Package
package com.syp.ui;

// View Imports
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Firebase GeoLocation Imports
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;

// Google GeoLocation Imports
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

// Firebase Database Imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

// Internal Class Imports
import com.syp.IOnLoadLocationListener;
import com.syp.MyLatLng;
import com.syp.model.Cafe;
import com.syp.MainActivity;
import com.syp.R;
import com.syp.model.Singleton;

// Data Structure Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// -----------------------------------------------
// Fragment for showing map and markers for cafes
// -----------------------------------------------
public class MapFragment extends Fragment {

    public LinearLayout infoBox;
    public Button viewCafeButton;
    public TextView shopName;
    public TextView shopAddress;
    public TextView shopHours;
    public MainActivity mainActivity;
    public GoogleMap mMap;
    public List<Marker> markers;
    public Button TESTinvisibleRedMarkerButton_PotofChange;
    public Button TESTinvisibleRedMarkerButton_PotofCha;
    public Marker potOfChang;
    public Marker potOfCha;
    public View v;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate View
        v = inflater.inflate(R.layout.fragment_map, container, false);

        // Get Main Activity
        mainActivity = (MainActivity) getActivity();
        markers = new ArrayList<>();

        // Assign views to variables
        infoBox = v.findViewById(R.id.cafe_infobox);
        shopName = v.findViewById(R.id.map_shopName);
        shopAddress = v.findViewById(R.id.map_shopAddress);
        shopHours = v.findViewById(R.id.map_shopTime);
        TESTinvisibleRedMarkerButton_PotofChange = v.findViewById(R.id.TESTinvisibleRedMarker_PotOfChang);
        TESTinvisibleRedMarkerButton_PotofCha = v.findViewById(R.id.TESTinvisibleRedMarker_PotOfCha);
        TESTinvisibleRedMarkerButton_PotofCha.setVisibility(View.VISIBLE);
        TESTinvisibleRedMarkerButton_PotofChange.setVisibility(View.VISIBLE);
        addTESTInvisibleRedMarkerButtonOnClick();

        // View Cafe Button & On Click Listener
        viewCafeButton = v.findViewById(R.id.view_cafe_button);
        setViewCafeOnClickListener();

        getMap();

        return v;
    }

    public void getMap(){
        // Create Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync((GoogleMap map) -> {

            // Set map & settings
            mainActivity.mapViewGoogleMap = map;
            mMap = map;
            setMapSettings();

            fetchCafes();
            setAllMarkerOnClickListeners();
        });
    }

    private void setMapSettings(){

        // Clear Previous markers
        mMap.clear();

        // Map Type & Zoom
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        setMapCamera();
        mMap.setMyLocationEnabled(true);
    }

    private void setMapCamera(){

        // Set max min
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(20.0f);

        // Camera settings & start spot
        CameraPosition googlePlex = CameraPosition.builder()
                .target(new LatLng(34.022404 ,-118.285109)).zoom(12)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(googlePlex));
    }

   public LatLng getUserLatitudeLongitude(){
        return new LatLng(mainActivity.latitude, mainActivity.longitude);
    }

    private void setViewCafeOnClickListener(){
        viewCafeButton.setOnClickListener((View v) ->
            Navigation.findNavController(v).navigate(MapFragmentDirections.actionMapFragmentToCafeFragment())
        );
    }

    private void fetchCafes(){

        // Get Database Reference to cafes
        DatabaseReference cafeRef = Singleton.get(mainActivity).getDatabase()
                .child("cafes");

        // Add Listener when info is recieved or changed
        cafeRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, MyLatLng> latLngList = new HashMap<>();

                for (DataSnapshot locationSnapshot : dataSnapshot.getChildren()) {


                    MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                    Cafe cafe = locationSnapshot.getValue(Cafe.class);

                    latLngList.put(locationSnapshot.getKey(), latLng);

                    Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(cafe.getLatitude(), cafe.getLongitude())).title(cafe.getName()));
                    marker.setTag(cafe.getId());
                    marker.showInfoWindow();
                    markers.add(marker);
                    if(cafe.getName().equalsIgnoreCase("Pot of Chang"))
                        potOfChang = marker;
                    if(cafe.getName().equalsIgnoreCase("Pot of Cha"))
                        potOfCha = marker;

                }
                //listener.onLoadLocationSuccess(latLngList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }

    private void setAllMarkerOnClickListeners(){

        // Set onClickListener for each marker
        mMap.setOnMarkerClickListener((Marker marker)->{
            Singleton.get(mainActivity).setCurrentCafeId((String) marker.getTag());
            showInfoBox();
            return false;
        });
    }

    private void addTESTInvisibleRedMarkerButtonOnClick(){
        TESTinvisibleRedMarkerButton_PotofCha.setOnClickListener((View v)->{
            Singleton.get(mainActivity).setCurrentCafeId((String) potOfCha.getTag());
            showInfoBox();
        });

        TESTinvisibleRedMarkerButton_PotofChange.setOnClickListener((View v)->{
            Singleton.get(mainActivity).setCurrentCafeId((String) potOfChang.getTag());
            showInfoBox();
        });
    }

    private void showInfoBox(){
        fetchCafeInfo();
        infoBox.setVisibility(View.VISIBLE);
    }

    private void fetchCafeInfo(){
        DatabaseReference ref = Singleton.get(mainActivity).getDatabase()
                .child("cafes").child(Singleton.get(mainActivity).getCurrentCafeId());

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Get cafe as snapshot
                Cafe cafe = dataSnapshot.getValue(Cafe.class);

                // Check cafe null
                if(cafe == null)
                    return;

                // Set cafe info
                setCafeInfo(cafe);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void setCafeInfo(Cafe cafe){
        shopName.setText(cafe.getName());
        shopAddress.setText(cafe.getAddress());
        shopHours.setText(cafe.getHours());
    }
}
