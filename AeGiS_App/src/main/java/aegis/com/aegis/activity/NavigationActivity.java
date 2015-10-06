package aegis.com.aegis.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import aegis.com.aegis.R;


public class NavigationActivity extends ActionBarActivity {

    private GoogleMap mMap;
    private Toolbar mToolbar;
    private GroundOverlay gov;
    private GroundOverlayOptions goo;
    private Location l;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_navigation);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setNavigationBarColor(getResources().getColor(R.color.navigationBarColor));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        if(mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        }
        l = (Location)getIntent().getSerializableExtra("Loc");
        if(l==null)
            l = new Location("O.R International Tambo" ,-26.1314138,28.2323354);

        setUpMapIfNeeded();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(getApplicationContext(),"Cleaning up..",Toast.LENGTH_LONG).show();
        gov.remove();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

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
            startActivity(new Intent(getApplicationContext(),SettingsActivity.class));
            return true;
        }

        if(id == R.id.action_search){
            Toast.makeText(getApplicationContext(), "Search action is selected!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null)
            {
                setUpMap();
            }
        }
    }

    private void setUpMap()
    {
        LatLng campus = new LatLng(-25.6840875,28.1315539);
        goo = new GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.floor_plan2)).position(campus,200f, 200f).bearing(19f);
        gov = mMap.addGroundOverlay(goo);


        if(!mMap.isIndoorEnabled())
        {
            Toast.makeText(this, "Indoor maps is currently unavailable", Toast.LENGTH_LONG).show();
            mMap.setIndoorEnabled(true);
            return;
        }

        mMap.setBuildingsEnabled(true);
        mMap.setMyLocationEnabled(true);
        UiSettings uis = mMap.getUiSettings();
        uis.setCompassEnabled(true);

        Toast.makeText(this,String.format("Buildings: %s My Location: %s "+l.getName()+" %s",mMap.isBuildingsEnabled(),mMap.isMyLocationEnabled(),mMap.getUiSettings().isCompassEnabled()),Toast.LENGTH_LONG).show();

                      mMap.setBuildingsEnabled(true);

        //Show current indoor map for this item
        mMap.animateCamera(CameraUpdateFactory
                                   .newCameraPosition(new CameraPosition.Builder()
                                                              .target(new LatLng(l.getLat(), l.getLng())).zoom(19).build()));
        MarkerOptions where = new MarkerOptions().position(new LatLng(l.getLat(), l.getLng())).title("Navigated to "+ l.getName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
        mMap.addMarker(where);

        MarkerOptions inzeta = new MarkerOptions().position(new LatLng(-25.6842879, 28.1311748)).title("Zeta");
        mMap.addMarker(inzeta);

        MarkerOptions newcenter = new MarkerOptions().position(new LatLng(-25.6841985,28.1315539)).title("new center zone").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
        mMap.addMarker(newcenter);
    }
}
