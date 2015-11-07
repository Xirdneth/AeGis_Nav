package aegis.com.aegis.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import aegis.com.aegis.R;
import aegis.com.aegis.logic.CustomLocation;
import aegis.com.aegis.route.AbstractRouting;
import aegis.com.aegis.route.Route;
import aegis.com.aegis.route.Routing;
import aegis.com.aegis.route.RoutingListener;
import aegis.com.aegis.utility.DirectionProvider;
import aegis.com.aegis.utility.IntentNames;
import aegis.com.aegis.utility.SphericalUtil;


public class NavigationActivity extends ActionBarActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener,
                                                                     View.OnClickListener, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationChangeListener, RoutingListener {

    ListView lv;
    TextView tv;
    ArrayList<Integer> circles = new ArrayList<Integer>();
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    StringBuilder sb;
    ArrayAdapter<String> adapter;
    Context context;
    Button b;
    View myView;
    MarkerOptions aegisdroppin;
    Marker myMarker;
    private GoogleMap mMap;
    private Toolbar mToolbar;
    private GroundOverlay gov;
    private GroundOverlayOptions goo;
    private CustomLocation l;
    private SharedPreferences applicationSettings;
    private FloatingActionButton fab_mylocation;
    private BitmapDescriptor overlay;
    private LatLng startp;
    private LatLng stopp;
    private DirectionProvider direction;
    private Location mylocation;
    private boolean isnavigating;
    private Polyline mPolyline;
    private double distance;
    private TextView distDisplay;
    private Marker destinationM;
    private ProgressDialog progressDialog;
    private ArrayList<Polyline> polylines;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_navigation);

        context = this;


        mainWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        context.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (mainWifi.isWifiEnabled() == false) {
            mainWifi.setWifiEnabled(true);
        }



        direction = new DirectionProvider(this);
        direction.onResume();
        l = (CustomLocation) getIntent().getSerializableExtra(IntentNames.MAP_INTENT_KEY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setNavigationBarColor(getResources().getColor(R.color.navigationBarColor));

        mToolbar = (Toolbar) findViewById(R.id.toolbar_maps);

        overlay = BitmapDescriptorFactory.fromResource(R.drawable.floor_plan_mp);
        distDisplay = (TextView) findViewById(R.id.distance);
        fab_mylocation = (FloatingActionButton)findViewById(R.id.fab_findme);
        fab_mylocation.setOnClickListener(this);
        if(mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
            mToolbar.setLogo(R.drawable.ic_navi);
            if(l!=null)
                mToolbar.setTitle(" " + l.getName());
            else
                mToolbar.setTitle(" AeGis Free Mode");
        }
        applicationSettings = PreferenceManager.getDefaultSharedPreferences(this);

        if(l==null)
            l = new CustomLocation("Belgium Campus", -25.6840875, 28.1315539);
        distDisplay.setVisibility(View.INVISIBLE);
        setUpMapIfNeeded();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        gov.remove();
        direction.onPause();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_maps, menu);
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
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

/*    @Override
    protected void onStart() {
        mainWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        context.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        TestWifi();
        super.onStart();
    }*/

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        context.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        direction.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        direction.onPause();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView))
                    .getMapAsync(this);
    }

    private void setUpMap()
    {
        LatLng campus = new LatLng(-25.6840875,28.1315539);
        goo = new GroundOverlayOptions().image(overlay).position(campus, 200f, 200f).bearing(199f);
        gov = mMap.addGroundOverlay(goo);
        applyPreference();

        if(!mMap.isIndoorEnabled())
        {
            Toast.makeText(this, "Indoor maps is currently unavailable", Toast.LENGTH_LONG).show();
            mMap.setIndoorEnabled(true);
            return;
        }

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMyLocationChangeListener(this);
        mMap.setOnMyLocationButtonClickListener(this);

        //Show current indoor map for this item
        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(l.getLat(), l.getLng())).zoom(19).build()));

        MarkerOptions where = new MarkerOptions().position(new LatLng(l.getLat(), l.getLng())).title(l.getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker)).draggable(false);

        mMap.addMarker(where);



        MarkerOptions newcenter = new MarkerOptions().position(new LatLng(-25.6841985, 28.1315539)).title("new center zone").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker)).draggable(false);
        mMap.addMarker(newcenter);


        mPolyline = mMap.addPolyline(new PolylineOptions()
                                             .geodesic(true));
    }

    private void drawPath(LatLng start, LatLng stop)
    {
        mPolyline.setPoints(Arrays.asList(start, stop));
        // Polylines are useful for marking paths and routes on the map.
    }

    public void onPickButtonClick()
    {
        //check if we can clear some previous navigation data
        if (isnavigating)
            destinationM.remove();
        isnavigating = false;
        // Construct an intent for the place picker
        try {
            PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
            Intent intent = intentBuilder.build(NavigationActivity.this);
            // Start the Intent by requesting a result, identified by a request code.
            startActivityForResult(intent, IntentNames.Places_Request_Code);
        } catch (GooglePlayServicesRepairableException e) {
            GooglePlayServicesUtil
                    .getErrorDialog(e.getConnectionStatusCode(), NavigationActivity.this, 0);
        } catch (GooglePlayServicesNotAvailableException e) {
            Toast.makeText(NavigationActivity.this, "Google Play Services is not available.",
                           Toast.LENGTH_LONG)
                 .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == IntentNames.Places_Request_Code
                && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            final CharSequence name = place.getName();
            final CharSequence address = place.getAddress();
            String attributions = PlacePicker.getAttributions(data);
            if (attributions == null) {
                attributions = "";
            }
            Toast.makeText(this,name+" "+address+Html.fromHtml(attributions),Toast.LENGTH_LONG).show();
            if(place == null) super.onActivityResult(requestCode, resultCode, data);
                destinationM = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_usermarker))
                        .title("Your Destination")
                        .position(place.getLatLng())
                        .draggable(false)
                        .snippet(name + " " + address + Html.fromHtml(attributions)));
            stopp = place.getLatLng();
            mylocation = mMap.getMyLocation();
            startp = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());

            mMap.animateCamera(CameraUpdateFactory
                                       .newCameraPosition(new CameraPosition.Builder()
                                                                  .target(new LatLng(mylocation.getLatitude(), mylocation.getLongitude())).bearing(direction.getRotation()).zoom(18).build()), 800, null);
            //draw a path between the user and destination
            drawPath(new LatLng(mylocation.getLatitude(), mylocation.getLongitude()), place.getLatLng());
            Route();
            isnavigating = true;
            distDisplay.setVisibility(View.VISIBLE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void applyPreference()
    {
        mMap.setTrafficEnabled(applicationSettings.getBoolean("pref_trafic_enabled",false));
        mMap.setBuildingsEnabled(applicationSettings.getBoolean("pref_buildings_enabled", false));
        mMap.setMyLocationEnabled(applicationSettings.getBoolean("pref_mylocation_enabled", true));
        UiSettings uis = mMap.getUiSettings();
        uis.setCompassEnabled(applicationSettings.getBoolean("pref_compass_enabled", false));
    }

    @Override
    public void onMapClick(LatLng latLng)
    {
        //Check if the clicked item can be a store or something similar
    }

    @Override
    public void onMapLongClick(LatLng latLng)
    {
        MarkerOptions usermarker = new MarkerOptions().position(latLng).title("Users Marker").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_usermarker)).draggable(true);
        mMap.addMarker(usermarker);
        //startp = latLng;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.fab_findme:
                onPickButtonClick();
            break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(mMap == null) {
            mMap = googleMap;
            mainWifi.startScan();
            TestWifi();
            setUpMap();
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Toast.makeText(this,marker.getTitle()+" started moving "+marker.getPosition().toString(),Toast.LENGTH_SHORT).show();
        /*if(stopp != null) {
            startp = stopp;
        }*/
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //this is update on each dragging of the marker
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Toast.makeText(this,marker.getTitle()+" ended moving "+marker.getPosition().toString(),Toast.LENGTH_SHORT).show();
        /*stopp = marker.getPosition();
        drawPath(startp, stopp);*/
    }

    @Override
    public boolean onMyLocationButtonClick() {
        mylocation = mMap.getMyLocation();
        startp = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
        LatLng current = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
        CameraPosition cameraPosition = CameraPosition.builder()
                                                      .target(current)
                                                      .zoom(19)
                                                      .bearing(direction.getRotation())
                                                      .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);
        return true;
    }


    @Override
    public void onMyLocationChange(android.location.Location location) {
        //forces our app to ignore larger inaccurate values
        if (location.getAccuracy() > 12) return;
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
        mainWifi.startScan();
        TestWifi();
        if (isnavigating) {
            mylocation = location;
            //calculate distance display some stuff in here
            mMap.animateCamera(CameraUpdateFactory
                                       .newCameraPosition(new CameraPosition.Builder()
                                                                  .target(new LatLng(location.getLatitude(), location.getLongitude())).bearing(direction.getRotation()).zoom(18).build()), 2000, null);
            UpdateUIDistance();
        }
    }

    private void UpdateUIDistance() {
        LatLng start = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
        distance = SphericalUtil.computeDistanceBetween(stopp, start);

        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }
        distDisplay.setText(String.format("Distance: %4.3f%s", distance, unit));

        drawPath(start, stopp);
    }

    public void Route() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Fetching route information.", true);
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.WALKING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(startp, stopp)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingFailure() {
        // The Routing request failed
        progressDialog.dismiss();
        Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
        // The Routing Request starts
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        progressDialog.dismiss();
        if (polylines != null)
            if (polylines.size() > 0) {
                for (Polyline poly : polylines) {
                    poly.remove();
                }
            }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    public void TestWifi() {
/*        if(this != null){
            tv = (TextView) findViewById(R.id.textView2);
            lv = (ListView) findViewById(R.id.listView);
            sv = (SurfaceView) findViewById(R.id.surfaceView);

            b = (Button) findViewById(R.id.wifibutton);
        }

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // sv = new BubbleSurfaceView(getApplicationContext());
                    Intent myIntent = new Intent(this, ListWifi.class);
                    myIntent.putExtra("Cir", circles);
                    WifiFragment.this.startActivity(myIntent);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });*/



        ArrayList<String> connections;
        ArrayList<Float> Signal_Strenth;
        ArrayList<String> FullSpot;

        connections = new ArrayList<String>();
        Signal_Strenth = new ArrayList<Float>();
        FullSpot = new ArrayList<String>();
        sb = new StringBuilder();
        List<ScanResult> wifiList;
        wifiList = mainWifi.getScanResults();
/*        if (wifiList.size() == 0)
        {
            FullSpot.add("No Wifi's In Area");
            Toast.makeText(context,"No Sectors Found Around You", Toast.LENGTH_LONG).show();
            //tv.setText("Not In Sector");
        }else {*/
        for (int i = 0; i < wifiList.size(); i++) {
            //connections.add(wifiList.get(i).SSID);
            //Signal_Strenth.add((float) wifiList.get(i).level);
            DecimalFormat df = new DecimalFormat("#.##");
            // Log.d(TAG, wifiList.get(i).BSSID + ": "+ wifiList.get(i).level + ", d: " + df.format(calculateDistance((double) wifiList.get(i).level, wifiList.get(i).frequency)) + "m");

            FullSpot.add("WiFi Name: " + wifiList.get(i).SSID + "\nSignal Distance: " + wifiList.get(i).BSSID + ": " + wifiList.get(i).level + ", \nDistance: " + df.format(calculateDistance((double) wifiList.get(i).level, wifiList.get(i).frequency)) + "m");

        }
        for (int k = 0; k < wifiList.size(); k++) {
//                    Toast.makeText(context,wifiList.size(), Toast.LENGTH_LONG).show();
            String idd = wifiList.get(k).BSSID.toString();
            //Toast.makeText(context,idd, Toast.LENGTH_LONG).show();
            if (idd.equals("0e:8b:fd:d4:4d:b5")) {
                //Toast.makeText(context,"In Loop Found Wifi :"+wifiList.get(k).SSID, Toast.LENGTH_LONG).show();
                int level1 = (int) calculateDistance((double) wifiList.get(k).level, wifiList.get(k).frequency);

                //Toast.makeText(context,"level1"+level1, Toast.LENGTH_LONG).show();
                if (level1 <= 10) {
                    DecimalFormat df = new DecimalFormat("#.##");
                    Toast.makeText(context, "Found Wifi :" + df.format(calculateDistance((double) wifiList.get(k).level, wifiList.get(k).frequency)), Toast.LENGTH_LONG).show();

                    if (myMarker != null)
                        myMarker.remove();

                    aegisdroppin = new MarkerOptions().position(new LatLng(-25.684359, 28.132313999999997)).title("Gamma").draggable(true);
                    myMarker = mMap.addMarker(aegisdroppin);
                    //tv.setText(wifiList.get(k).SSID + " Sector");
                    //Toast.makeText(context,"After TextSEt Toast", Toast.LENGTH_LONG).show();
                    return;
                }
                if (level1 >= 10)
                    myMarker.remove();

                aegisdroppin = new MarkerOptions().position(new LatLng(-25.684188, 28.131681999999998)).title("Leuven").draggable(true);
                myMarker = mMap.addMarker(aegisdroppin);
                //tv.setText("No Sectors Found Around You");
            } else {
                if (myMarker != null)
                    myMarker.remove();
                return;
            }
            //}
        }

        /*if (wifiList.size() != 0)
        {
            int level = (int) calculateDistance((double) wifiList.get(0).level, wifiList.get(0).frequency);

            circles.add(level);
        }*/

        //level = (int) calculateDistance((double) wifiList.get(1).level, wifiList.get(1).frequency);
        //circles.add(level);
        //level = (int) calculateDistance((double) wifiList.get(2).level, wifiList.get(2).frequency);
        //circles.add(level);
        /*ArrayList<String> values;
        values = FullSpot;*/

/*        if(this != null){
            ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.mylistcolor ,android.R.id.text1,values);

            lv.setAdapter(adapter);
        }*/


/*        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position;

                String itemValue = (String) lv.getItemAtPosition(position);

                Toast.makeText(context, "Selected: " + itemValue, Toast.LENGTH_LONG).show();
            }
        });
        lv.invalidateViews();
        tv.invalidate();*/
        //Intent myIntent = new Intent(MainActivity.this, ListWifi.class);
        //myIntent.putStringArrayListExtra("Wifi",FullSpot);
        //MainActivity.this.startActivity(myIntent);


    }

    @Override
    public void onPause() {
        context.unregisterReceiver(receiverWifi);
        super.onPause();
    }

/*
    public void doInback()
    {
        Handler handler;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() { // TODO Auto-generated method
                mainWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                receiverWifi = new WifiReceiver();
                context.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                mainWifi.startScan();
                TestWifi();
                ;
            }
        }, 100);
    }*/

    public double calculateDistance(double levelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            ArrayList<String> connections = new ArrayList<String>();
            ArrayList<Float> Signal_Strenth = new ArrayList<Float>();
            sb = new StringBuilder();
            List<ScanResult> wifiList;
            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++) {
                connections.add(wifiList.get(i).SSID);
            }


        }
    }

}

