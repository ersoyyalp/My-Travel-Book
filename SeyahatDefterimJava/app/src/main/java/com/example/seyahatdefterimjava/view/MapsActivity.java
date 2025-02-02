package com.example.seyahatdefterimjava.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.seyahatdefterimjava.R;
import com.example.seyahatdefterimjava.databinding.ActivityMapsBinding;
import com.example.seyahatdefterimjava.model.Place;
import com.example.seyahatdefterimjava.roomdb.PlaceDao;
import com.example.seyahatdefterimjava.roomdb.PlaceDataBase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    SharedPreferences sharedPreferences;
    boolean info;
    //DATABASE
    PlaceDataBase db;
    PlaceDao placeDao;

    Double selectedLatitude;
    Double selectedLongitude;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    Place selectedPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();

        sharedPreferences = this.getSharedPreferences("com.example.seyahatdefterimjava", MODE_PRIVATE);
        info=false;
        //DATABASE
        db= Room.databaseBuilder(getApplicationContext(),PlaceDataBase.class,"Places").build();
        placeDao=db.placeDao();

        selectedLatitude=0.0;
        selectedLongitude=0.0;

        binding.saveButton.setEnabled(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");

        if (intentInfo.equals("new")){

            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            locationManager =(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {

                @Override
                public void onLocationChanged(@NonNull Location location) {

                    info = sharedPreferences.getBoolean("info", false);

                    if (info == false) {
                        //KULLANICININ LOKASYONUNA GÖRE HAREKET
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));
                        sharedPreferences.edit().putBoolean("info",true).apply();
                    }
                }
            };
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //İZİN İSTEME
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(),"Haritalar için izin gerekli",Snackbar.LENGTH_INDEFINITE).setAction("İzim Ver", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //İZİN İSTEME
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                }else{
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                // BU METHOD ZORUNLU DEĞİL - YENİ KONUM GİRİLMEDEN EN SON BİLİNEN KONUMA GİDER
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null){
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16));
                }
                mMap.setMyLocationEnabled(true);
            }


        }else{

            mMap.clear();

            selectedPlace= (Place) intent.getSerializableExtra("place");

            LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,16));

            binding.placeNameText.setText(selectedPlace.name);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }

        //casting BULUNDUĞUMUZ KONUMU ALMA


        //latitude ->enlem
        //longitude ->boylam
        //LatLng
        //MANUEL KONUM GİRME
       /* LatLng kaleici = new LatLng(36.8841,30.7066);
        mMap.addMarker(new MarkerOptions().position(kaleici).title("Antalya Kaleiçi"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kaleici,15));*/

    }

    private void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //izin verildi
                    if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                        // BU METHOD ZORUNLU DEĞİL - YENİ KONUM GİRİLMEDEN EN SON BİLİNEN KONUMA GİDER
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastLocation != null){
                            LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16));
                        }

                    }

                }else{
                    //kullanıcı reddetti
                    Toast.makeText(MapsActivity.this,"İzin gerekli",Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));


        selectedLatitude =latLng.latitude;
        selectedLongitude=latLng.longitude;
        //Kullanıcı bir yer seçtiğinde kayıt butonu görulecek
        binding.saveButton.setEnabled(true);

    }

    public void save (View view){

        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);

        //threading -> Main

        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();

        //disposable
        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)
        );
    }
    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void delete(View view){

        if (selectedPlace != null){

            compositeDisposable.add(placeDao.delete(selectedPlace)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::handleResponse)

            );

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}