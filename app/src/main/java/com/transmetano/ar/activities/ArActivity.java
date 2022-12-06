/*
 *  Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.transmetano.ar.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.NavigationConstraint;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.MultilayerPolylineSymbol;
import com.esri.arcgisruntime.symbology.SceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSceneSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.SolidStrokeSymbolLayer;
import com.esri.arcgisruntime.symbology.StrokeSymbolLayer;
import com.esri.arcgisruntime.symbology.SymbolLayer;
import com.esri.arcgisruntime.toolkit.ar.ArLocationDataSource;
import com.esri.arcgisruntime.toolkit.ar.ArcGISArView;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.tooltip.Tooltip;
import com.transmetano.ar.R;
import com.transmetano.ar.arOffline.arview.ARView;
import com.transmetano.ar.arOffline.compass.CompassData;
import com.transmetano.ar.arOffline.compass.CompassRepository;
import com.transmetano.ar.arOffline.compass.DestinationData;
import com.transmetano.ar.arOffline.orientation.OrientationData;
import com.transmetano.ar.arOffline.orientation.OrientationProvider;
import com.transmetano.ar.fragments.AboutFragment;
import com.transmetano.ar.fragments.ConfigFragment;
import com.transmetano.ar.fragments.MenuFragment;
import com.transmetano.ar.fragments.SourceFragment;
import com.transmetano.ar.objects.CurrentLayer;
import com.transmetano.ar.objects.DotLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class ArActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener, MenuFragment.FrCallback {

    private final String PREFS_OFFLINE = "OfflinePreferences";
    private final String VALOR_OFFLINE = "ValorOffline";
    private String valor_feature = "";

    // Activity Name
    private static final String TAG = ArActivity.class.getSimpleName();

    // Preferences
    private static final String PREFS = "LayerPreferences";
    private static final String LAYER_PREFS = "LastLayer";
    private String name_layer = "Layers_MapTF2_D";

    // token
    private static final String PREFS_TOKEN = "LoginPreferences";
    private static final String USER_PREFS = "username";
    private static final String PASS_PREFS = "password";

    // Arcgis elements
    private ARView arLabelView;
    private ArcGISArView mArView;
    private ArcGISScene mScene;
    private GraphicsOverlay tubeOverlay;
    private GraphicsOverlay sphereOverlay;
    private UserCredential credential;

    // Fragments
    FragmentTransaction ft;
    FrameLayout mediaFragment;
    AboutFragment aboutFragment;
    ConfigFragment configFragment;
    MenuFragment menuFragment;
    SourceFragment sourceFragment;

    // Others
    private ConstraintLayout transitionsContainer;
    private ImageView ivCompass;
    private TextView tvRange;
    private Slider sRange;
    private String state = "EN LINEA";

    private MenuItem toolbarState;
    private MenuItem toolbarRefresh;
    private MenuItem toolbarMenu;
    private MenuItem toolbarClose;

    // Compass
    private final float[] mGravity = new float[3];
    private final float[] mGeomagnetic = new float[3];
    private float currentAzimuth = 0f;
    private SensorManager sensorManager;

    // OfflineAr
    private DotLocation currentLocation = new DotLocation(0.0, 0.0, 0.0);
    private CompassRepository compassRepository;
    private OrientationProvider orientationProvider;

    // Create Line
    private DotLocation[] dotLocations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        bindViews();
        loadDots();
        setCompassSensor();
    }

    @SuppressLint("DefaultLocale")
    private void bindViews() {
        transitionsContainer = findViewById(R.id.transitionsContainer);
        mediaFragment = findViewById(R.id.mediaFragment);
        mArView = findViewById(R.id.arView);
        tvRange = findViewById(R.id.tvRange);
        sRange = findViewById(R.id.sRange);
        ivCompass = findViewById(R.id.ivCompass);
        Toolbar toolbar = findViewById(R.id.toolbar);

        arLabelView = findViewById(R.id.ar_label_view);

        setSupportActionBar(toolbar);

        sRange.addOnChangeListener((slider, value, fromUser) -> {
            CurrentLayer.setRange((int) value);
            mArView.setClippingDistance(CurrentLayer.getRange());
            tvRange.setText(String.format("%s%d%s",
                    getString(R.string.ar_max_dist_1),
                    CurrentLayer.getRange(),
                    getString(R.string.ar_max_dist_2)));
        });

        ivCompass.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            //Log.d("ERROR 1", String.valueOf(dotTubes.));
            intent.putExtra("name_layer", name_layer);
            intent.putExtra(getString(R.string.layer_dots), dotLocations);
            startActivity(intent);
        });
    }

    @SuppressLint("DefaultLocale")
    private void setParameters() {
        new Tooltip.Builder(ivCompass)
                .setGravity(Gravity.TOP)
                .setText(R.string.ar_see_map)
                .setBackgroundColor(getColor(R.color.primary))
                .setDismissOnClick(true)
                .setCancelable(true)
                .setCornerRadius(12f)
                .setTextColor(getColor(R.color.white))
                .show();
        Camera camera = mArView.getOriginCamera();
        CurrentLayer.setHeading((int) (camera.getHeading()));
        headChange(CurrentLayer.getHeading());
        altitudeChange(CurrentLayer.getAltitude());
        //surfaceChange(CurrentLayer.getBaseSurface());
        sRange.setValue(CurrentLayer.getRange());
        sRange.setValueTo(CurrentLayer.getMaxRange());
        mArView.setClippingDistance(CurrentLayer.getRange());
        tvRange.setText(String.format("%s%d%s",
                getString(R.string.ar_max_dist_1),
                CurrentLayer.getRange(),
                getString(R.string.ar_max_dist_2)));
    }

    private void loadDots() {

        try {
            String url = CurrentLayer.getCurrent(this).getUrl();
            SharedPreferences prefs = getSharedPreferences(PREFS_TOKEN, MODE_PRIVATE);
            String username = prefs.getString(USER_PREFS, "");
            String password = prefs.getString(PASS_PREFS, "");
            final ServiceFeatureTable serviceFT = new ServiceFeatureTable(url);

            credential = new UserCredential(username, password);
            serviceFT.setCredential(credential);

            serviceFT.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.MANUAL_CACHE);
            serviceFT.loadAsync();

            serviceFT.addDoneLoadingListener(() -> {

                //clausula de busqueda
                QueryParameters queryParam = new QueryParameters();
                queryParam.setWhereClause("1=1");
                queryParam.setOutSpatialReference(SpatialReference.create(4326));

                // set all outfields
                List<String> outFields = new ArrayList<>();
                outFields.add("*");

                final ListenableFuture<FeatureQueryResult> featureQResult =
                        serviceFT.populateFromServiceAsync(queryParam, true, outFields);

                featureQResult.addDoneListener(() -> {
                    try {
                        ArrayList<DotLocation> tubeList = new ArrayList<>();

                        int num = 1;
                        for (Feature feature : featureQResult.get()) {
                            if (feature.getGeometry() != null) {

                                name_layer = feature.getFeatureTable().getTableName();

                                SharedPreferences prefs1 =
                                        getSharedPreferences(PREFS_OFFLINE + "_" + name_layer,
                                                MODE_PRIVATE);
                                valor_feature = prefs1.getString(VALOR_OFFLINE + "_" + num, "");

                                tubeList.add(new Gson().fromJson(
                                        feature.getGeometry().toJson(),
                                        DotLocation.class));

                                valor_feature = feature.getGeometry().toJson();

                                SharedPreferences.Editor editor =
                                        getSharedPreferences(PREFS_OFFLINE + "_" +
                                                name_layer, MODE_PRIVATE).edit();
                                editor.putString(VALOR_OFFLINE + "_" + num, valor_feature);
                                editor.apply();
                                num++;

                            } else {
                                Log.e(TAG, "Error in FeatureQueryResult: null");
                            }
                        }

                        ArrayList<DotLocation> desLocations = new ArrayList<>();
                        for (DotLocation point : tubeList) {
                            desLocations.add(new DotLocation(
                                    point.getLon(), point.getLat(), point.getAlt()));
                        }
                        compassRepository.setDestinationsLocation(desLocations);
                        dotLocations = tubeList.toArray(new DotLocation[0]);

                        if (tubeOverlay == null) {
                            requestPermissions();
                        } else {
                            mArView.getSceneView().getGraphicsOverlays().remove(tubeOverlay);
                            renderDots();
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        Log.e(TAG, "Error in FeatureQueryResult: " + e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in FeatureQueryResult: " + e.getMessage());
        }

        offline_AR();

    }

    private void offline_AR() {
        SharedPreferences prefslayers = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {

            name_layer = String.valueOf(new JSONObject(prefslayers.getString(LAYER_PREFS, ""))
                    .get("name"));
            SharedPreferences prefsoffline = getSharedPreferences(PREFS_OFFLINE + "_" + name_layer, MODE_PRIVATE);

            if (Objects.equals(state, "SIN CONEXION") && prefsoffline.getAll().size() > 0) {

                ArrayList<DotLocation> tubeList = new ArrayList<>();
                try {
                    int num = prefsoffline.getAll().size();
                    String value_json;
                    for (int i = 1; i <= num; i++) {
                        valor_feature = prefsoffline.getString(VALOR_OFFLINE + "_" + i, "");
                        value_json = String.valueOf(new JSONObject(valor_feature));
                        tubeList.add(new Gson().fromJson(value_json, DotLocation.class));
                    }

                    dotLocations = tubeList.toArray(new DotLocation[0]);
                    if (tubeOverlay == null) {
                        requestPermissions();
                    } else {
                        mArView.getSceneView().getGraphicsOverlays().remove(tubeOverlay);
                        renderDots();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setCompassSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    protected void displayMenu() {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        toolbarState.setVisible(true);
        //statusChange();
        toolbarState.setTitle(state);
        toolbarMenu.setVisible(false);
        toolbarRefresh.setVisible(false);
        toolbarClose.setVisible(true);
        mediaFragment.setVisibility(View.VISIBLE);
        ft = getSupportFragmentManager().beginTransaction();
        menuFragment = MenuFragment.newInstance();
        ft.replace(R.id.mediaFragment, menuFragment);
        ft.commit();
    }

    protected void displayConfig() {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        ft = getSupportFragmentManager().beginTransaction();
        configFragment = ConfigFragment.newInstance();
        ft.replace(R.id.mediaFragment, configFragment);
        ft.commit();
    }

    protected void displaySource() {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        ft = getSupportFragmentManager().beginTransaction();
        sourceFragment = SourceFragment.newInstance();
        ft.replace(R.id.mediaFragment, sourceFragment);
        ft.commit();
    }

    protected void displayAbout() {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        ft = getSupportFragmentManager().beginTransaction();
        aboutFragment = AboutFragment.newInstance();
        ft.replace(R.id.mediaFragment, aboutFragment);
        ft.commit();
    }

    /**
     * Request read external storage for API level 23+.
     */
    private void requestPermissions() {
        // define permission to request
        String[] reqPermission = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };

        statusChange();

        if (ContextCompat.checkSelfPermission(this, reqPermission[2])
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, reqPermission[3])
                == PackageManager.PERMISSION_GRANTED) {
            tryAR();
        } else {
            // request permission
            ActivityCompat.requestPermissions(this, reqPermission, 4);
        }
    }

    /**
     * Handle the permissions request response.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        try {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryAR();
            } else {
                // report to user that permission was denied
                Toast.makeText(this, getString(R.string.navigate_ar_permission_denied),
                        Toast.LENGTH_SHORT).show();
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } catch (NullPointerException e) {
            Toast.makeText(this, getString(R.string.navigate_ar_permission_denied),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void tryAR() {
        try {
            SharedPreferences prefslayers = getSharedPreferences(PREFS, MODE_PRIVATE);
            name_layer = String.valueOf(new JSONObject(prefslayers.getString(LAYER_PREFS, "")).get("name"));
            SharedPreferences prefsoffline = getSharedPreferences(PREFS_OFFLINE + "_" + name_layer, MODE_PRIVATE);

            if (Objects.equals(state, "EN LINEA")) {
                generateAr();
            } else if (Objects.equals(state, "SIN CONEXION") && prefsoffline.getAll().size() > 0) {
                mArView.getSceneView().getGraphicsOverlays().remove(tubeOverlay);
                renderDots();
            }

            getCoordinates();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getCoordinates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000L, 5f, this);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void generateAr() {

        // get a reference to the ar view
        mArView.setVisibility(View.VISIBLE);
        mArView.registerLifecycle(getLifecycle());
        // create a scene and add it to the scene view
        mScene = new ArcGISScene(Basemap.createImagery());
        mArView.getSceneView().setScene(mScene);
        // create and add an elevation surface to the scene
        ArcGISTiledElevationSource elevationSource =
                new ArcGISTiledElevationSource(getString(R.string.elevation_url));
        Surface elevationSurface = new Surface();
        elevationSurface.getElevationSources().add(elevationSource);
        mArView.getSceneView().getScene().setBaseSurface(elevationSurface);
        // allow the user to navigate underneath the surface
        elevationSurface.setNavigationConstraint(NavigationConstraint.NONE);
        // hide the basemap. The image feed provides map context while navigating in AR
        elevationSurface.setOpacity(0f);
        // disable plane visualization. It is not useful for this AR scenario.
        if (mArView.getArSceneView() != null) {
            mArView.getArSceneView().getPlaneRenderer().setEnabled(false);
            mArView.getArSceneView().getPlaneRenderer().setVisible(false);
        }
        // add an ar location data source to update location
        mArView.setLocationDataSource(new ArLocationDataSource(this));

        renderDots();

        mArView.setClippingDistance(CurrentLayer.getRange());

        mArView.startTracking(ArcGISArView.ARLocationTrackingMode.CONTINUOUS);

        String url = CurrentLayer.getCurrent(this).getUrl();
        final ServiceFeatureTable serviceFT = new ServiceFeatureTable(url);

        serviceFT.setCredential(credential);
        FeatureLayer mFeatureLayer = new FeatureLayer(serviceFT);
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_IMAGERY);

        serviceFT.loadAsync();
        mFeatureLayer.loadAsync();
        //Log.d("Value FL ARA",mFeatureLayer.toString());
        // add the layer to the map
        map.getOperationalLayers().add(mFeatureLayer);

        // this step is handled on the back end anyways,
        // but we're applying a vertical offset to every
        // update as per the calibration step above
        Objects.requireNonNull(mArView.getLocationDataSource()).addLocationChangedListener(locationChangedEvent -> {
            Point updatedLocation = locationChangedEvent.getLocation().getPosition();
            mArView.setOriginCamera(new Camera(new Point(updatedLocation.getX(), updatedLocation.getY(), updatedLocation.getZ()),
                    mArView.getOriginCamera().getHeading(),
                    mArView.getOriginCamera().getPitch(),
                    mArView.getOriginCamera().getRoll()));
        });

        // disable touch interactions with the scene view
        mArView.getSceneView().setOnTouchListener((view, motionEvent) -> true);

    }

    private void renderDots() {

        // create and add a graphics overlay for showing the tube line
        tubeOverlay = new GraphicsOverlay();
        sphereOverlay = new GraphicsOverlay();

        // load the overlay over the scene
        mArView.getSceneView().getGraphicsOverlays().add(tubeOverlay);
        mArView.getSceneView().getGraphicsOverlays().add(sphereOverlay);

        // create a renderer for the tube geometry
        SolidStrokeSymbolLayer strokeSymbolLayer = new SolidStrokeSymbolLayer(
                0.3, getColor(R.color.white), new LinkedList<>(), StrokeSymbolLayer.LineStyle3D.TUBE);
        strokeSymbolLayer.setCapStyle(StrokeSymbolLayer.CapStyle.ROUND);
        SimpleMarkerSceneSymbol sphere = SimpleMarkerSceneSymbol.createSphere(
                getColor(R.color.primary), 0.5, SceneSymbol.AnchorPosition.CENTER);

        // create a new polyline that represent the tubes
        PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());
        for (DotLocation dot : dotLocations) {
            Point point = new Point(dot.getLat(), dot.getLon(), dot.getAlt());
            sphereOverlay.getGraphics().add(new Graphic(point, sphere));
            pointCollection.add(point);
        }

        Polyline mPolyline = new Polyline(pointCollection);
        Graphic polylineGraphic = new Graphic(mPolyline);
        tubeOverlay.getGraphics().add(polylineGraphic);

        // display the graphic above the ground
        LayerSceneProperties.SurfacePlacement sp = LayerSceneProperties.SurfacePlacement.ABSOLUTE;
        if (dotLocations[0].getAlt() == 0) sp = LayerSceneProperties.SurfacePlacement.RELATIVE;
        tubeOverlay.getSceneProperties().setSurfacePlacement(sp);
        sphereOverlay.getSceneProperties().setSurfacePlacement(sp);

        ArrayList<SymbolLayer> layers = new ArrayList<>();
        layers.add(strokeSymbolLayer);
        MultilayerPolylineSymbol polylineSymbol = new MultilayerPolylineSymbol(layers);
        SimpleRenderer polylineRenderer = new SimpleRenderer(polylineSymbol);
        tubeOverlay.setRenderer(polylineRenderer);

        setParameters();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        if (mArView != null) {
            mArView.stopTracking();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        toolbarRefresh = menu.findItem(R.id.refresh);
        toolbarMenu = menu.findItem(R.id.menu);
        toolbarClose = menu.findItem(R.id.close);
        toolbarState = menu.findItem(R.id.state);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu) {
            displayMenu();
            return true;
        } else if (itemId == R.id.state) {
            statusChange();
            return true;
        } else if (itemId == R.id.close) {
            closeMenu();
            return true;
        } else if (itemId == R.id.refresh) {
            Intent intent = new Intent(this, ArActivity.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);

        if (mArView != null) {
            mArView.startTracking(ArcGISArView.ARLocationTrackingMode.CONTINUOUS);
        }

        orientationProvider = new OrientationProvider(this.getWindowManager());
        compassRepository = new CompassRepository();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void closeMenu() {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        toolbarState.setVisible(true);
        toolbarState.setTitle(state);
        toolbarClose.setVisible(false);
        toolbarMenu.setVisible(true);
        toolbarRefresh.setVisible(true);
        mediaFragment.setVisibility(View.GONE);
        sRange.setValueTo(CurrentLayer.getMaxRange());
        mArView.setClippingDistance(CurrentLayer.getRange());
        tvRange.setText(String.format("%s%d%s",
                getString(R.string.ar_max_dist_1),
                CurrentLayer.getRange(),
                getString(R.string.ar_max_dist_2)));
    }

    @Override
    public void onSourceClick() {
        displaySource();
    }

    @Override
    public void onConfigClick() {
        displayConfig();
    }

    @Override
    public void onAboutClick() {
        displayAbout();
    }

    @Override
    public void onLogoutClick() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void headChange(int value) {
        // get the origin camera
        Camera camera = mArView.getOriginCamera();
        // get a camera with a new heading
        Camera newCam = camera.rotateTo(value, camera.getPitch(), camera.getRoll());
        // apply the new origin camera
        mArView.setOriginCamera(newCam);
    }

    @Override
    public void altitudeChange(int value) {
        tubeOverlay.getSceneProperties().setAltitudeOffset(value);
        sphereOverlay.getSceneProperties().setAltitudeOffset(value);
    }

    @Override
    public void surfaceChange(float value) {
        Log.d("prefs 5", String.valueOf(value));
        mScene.getBaseSurface().setOpacity((float) 0.1);
    }

    @Override
    public void layerChange() {
        SharedPreferences mPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        String jsonLayer = new Gson().toJson(CurrentLayer.getCurrent(this));
        prefsEditor.putString(LAYER_PREFS, jsonLayer);
        prefsEditor.apply();
        Toast.makeText(this, getString(R.string.source_update_successfull),
                Toast.LENGTH_LONG).show();
        loadDots();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * sensorEvent.values[2];
            }

            float[] r = new float[9];
            float[] i = new float[9];

            ChangeArOfflineView(sensorEvent);

            boolean success = SensorManager.getRotationMatrix(r, i, mGravity, mGeomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(r, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                Animation anim = new RotateAnimation(-currentAzimuth, -azimuth,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                currentAzimuth = azimuth;

                anim.setDuration(500);
                anim.setRepeatCount(0);
                anim.setFillAfter(true);

                ivCompass.startAnimation(anim);

            }
        }
    }

    private void ChangeArOfflineView(SensorEvent sensorEvent) {

        if (compassRepository.getDestinationsLocation().size() > 0) {

            OrientationData orientationData = orientationProvider.handleSensorEvent(sensorEvent);

            ArrayList<DestinationData> destinations = new ArrayList<>();

            for (DotLocation loc : compassRepository.getDestinationsLocation()) {
                destinations.add(
                        compassRepository.handleDestination(currentLocation, loc,
                                orientationData.getCurrentAzimuth()));
            }

            CompassData compassData = new CompassData(
                    orientationData,
                    destinations,
                    compassRepository.getMaxDistance(destinations),
                    compassRepository.getMinDistance(destinations),
                    currentLocation
            );

            arLabelView.setCompassData(compassData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Nothing to do
    }

    private void statusChange() {
        state = (isOnlineNet() && isNetDisponible()) ? "EN LINEA" : "SIN CONEXION";
        if (toolbarState != null) toolbarState.setTitle(state);
    }

    private boolean isNetDisponible() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo actNetInfo = connectivityManager.getActiveNetworkInfo();
        return (actNetInfo != null && actNetInfo.isConnected());
    }

    public Boolean isOnlineNet() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.es");
            int val = p.waitFor();
            return val == 0;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = new DotLocation(
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude());
    }
}
