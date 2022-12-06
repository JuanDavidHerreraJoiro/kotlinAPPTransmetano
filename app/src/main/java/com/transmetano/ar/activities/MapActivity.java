package com.transmetano.ar.activities;

import static android.view.Gravity.CENTER;
import static com.google.ar.sceneform.rendering.HeadlessEngineWrapper.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Multipoint;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISVectorTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseJob;
import com.esri.arcgisruntime.tasks.geodatabase.SyncGeodatabaseParameters;
import com.esri.arcgisruntime.tasks.geodatabase.SyncLayerOption;
import com.transmetano.ar.R;
import com.transmetano.ar.objects.CurrentLayer;
import com.transmetano.ar.objects.DotLocation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MapActivity extends AppCompatActivity {

    private MapView mMapView;
    private LocationDisplay mLocationDisplay;

    private LinearLayout transitionsContainer;
    private LinearLayout lyInfo;
    private LinearLayout lyTable;
    private TextView tvTubeNumber;
    private ImageButton ibEdit;
    private LinearLayout lyButtons;
    private Button btnSave;
    private Button btnCancel;
    private EditText etObservation;
    private Button mGeodatabaseButton;

    private GeodatabaseSyncTask mGeodatabaseSyncTask;
    private GraphicsOverlay mGraphicsOverlay;
    private EditState mCurrentEditState;

    // token
    private static final String PREFS_TOKEN = "LoginPreferences";
    private static final String USER_PREFS = "username";
    private static final String PASS_PREFS = "password";

    private UserCredential credential;

    private Geodatabase geodatabase;
    private FeatureLayer featureLayer;

    // Map
    private Callout mCallout;
    private ServiceFeatureTable mServiceFeatureTable;
    private FeatureLayer mFeatureLayer;
    private ArcGISFeature mSelectedArcGISFeature;
    private boolean mFeatureUpdated;
    private Geodatabase mGeodatabase;

    // File
    private File mOfflineMapDirectory;
    private File mOfflineMapDirectoryGeodatabase;
    private File directoryArchive;

    // Preferences
    private DotLocation[] dotLocations;

    private String state = "SIN CONEXION";
    private String path_map = "";
    private String path_geodatabase = "";

    private MenuItem toolbarState;
    private MenuItem toolbarClose;
    private String name_layer = "";

    private List<Feature> mSelectedFeatures;
    private final Graphic downloadAreaGraphic = new Graphic();

    private final int requestCode = 2;
    private final String[] reqPermissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        lyInfo = findViewById(R.id.lyInfo);
        lyTable = findViewById(R.id.lyTable);
        tvTubeNumber = findViewById(R.id.tvTubeNumber);
        ibEdit = findViewById(R.id.ibEdit);
        lyButtons = findViewById(R.id.lyButtons);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        transitionsContainer = findViewById(R.id.transitionsContainer);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dotLocations = (DotLocation[]) getIntent().getSerializableExtra(getString(R.string.layer_dots));

        // set edit state to not ready until geodatabase job has completed successfully
        mCurrentEditState = EditState.NotReady;

        // create a map view and add a map
        mMapView = findViewById(R.id.mapView);
        // create a graphics overlay and symbol to mark the extent
        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

        name_layer = getIntent().getStringExtra("name_layer");
        name_layer = name_layer.replaceAll("Layers_", "");
        directoryPreplanned();
        directoryGeodatabase();
        // add listener to handle generate/sync geodatabase button
        mGeodatabaseButton = findViewById(R.id.geodatabaseButton);
        mGeodatabaseButton.setOnClickListener(v -> {
            if (mCurrentEditState == EditState.NotReady) {
                generateGeodatabase();
            } else if (mCurrentEditState == EditState.Ready) {
                syncGeodatabase();
            }
        });
        // add listener to handle motion events, which only responds once a geodatabase is loaded
        mMapView.setOnTouchListener(
                new DefaultMapViewOnTouchListener(MapActivity.this, mMapView) {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                        if (mCurrentEditState == EditState.Ready) {
                            selectFeaturesAt(mapPointFrom(motionEvent), 10);
                        } else if (mCurrentEditState == EditState.Editing) {
                            //moveSelectedFeatureTo(mapPointFrom(motionEvent));
                            //showEditableView(feature);
                        }
                        return true;
                    }
                });

        // use local tile package for the base map
        for (String rest : mOfflineMapDirectory.list()) {
            if (rest.contains(name_layer)) {
                String sub_ruta = rest + "/p13";
                String ruta_archive = mOfflineMapDirectory.getPath() + "/" + sub_ruta;
                directoryArchive = new File(ruta_archive);
                for (String directory : directoryArchive.list()) {
                    if (directory.contains(".vtpk")) {
                        path_map = directoryArchive + "/" + directory;
                    }
                }
            }
        }

        ArcGISVectorTiledLayer localTiledLayer = new ArcGISVectorTiledLayer(path_map);
        Basemap basemap = new Basemap(localTiledLayer);
        final ArcGISMap map = new ArcGISMap(basemap);
        mMapView.setMap(map);
    }

    private void generateGeodatabase() {
        SharedPreferences prefs = getSharedPreferences(PREFS_TOKEN, MODE_PRIVATE);
        String username = prefs.getString(USER_PREFS, "");
        String password = prefs.getString(PASS_PREFS, "");
        credential = new UserCredential(username, password);

        String url = CurrentLayer.getCurrent(this).getUrl();
        url = url.replace("/0", "");
        Log.e("ERROR 22", url);
        // define geodatabase sync task
        mGeodatabaseSyncTask = new GeodatabaseSyncTask(url);
        mGeodatabaseSyncTask.setCredential(credential);
        mGeodatabaseSyncTask.loadAsync();
        mGeodatabaseSyncTask.addDoneLoadingListener(() -> {
            final SimpleLineSymbol boundarySymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 5);
            // show the extent used as a graphic
            final Envelope extent = mMapView.getVisibleArea().getExtent();
            Graphic boundary = new Graphic(extent, boundarySymbol);
            mGraphicsOverlay.getGraphics().add(boundary);
            // create generate geodatabase parameters for the current extent
            final ListenableFuture<GenerateGeodatabaseParameters> defaultParameters = mGeodatabaseSyncTask
                    .createDefaultGenerateGeodatabaseParametersAsync(extent);
            defaultParameters.addDoneListener(() -> {
                try {
                    // set parameters and don't include attachments
                    GenerateGeodatabaseParameters parameters = defaultParameters.get();
                    parameters.setReturnAttachments(false);
                    // define the local path where the geodatabase will be stored
                    //directoryPreplanned();
                    final String localGeodatabasePath = mOfflineMapDirectoryGeodatabase.getPath() + File.separator + name_layer + ".geodatabase";
                    // create and start the job
                    final GenerateGeodatabaseJob generateGeodatabaseJob = mGeodatabaseSyncTask
                            .generateGeodatabase(parameters, localGeodatabasePath);
                    generateGeodatabaseJob.start();
                    createProgressDialog(generateGeodatabaseJob);
                    // get geodatabase when done
                    generateGeodatabaseJob.addJobDoneListener(() -> {
                        if (generateGeodatabaseJob.getStatus() == Job.Status.SUCCEEDED) {
                            mGeodatabase = generateGeodatabaseJob.getResult();
                            mGeodatabase.loadAsync();
                            mGeodatabase.addDoneLoadingListener(() -> {
                                if (mGeodatabase.getLoadStatus() == LoadStatus.LOADED) {
                                    // get only the first table which, contains points
                                    GeodatabaseFeatureTable pointsGeodatabaseFeatureTable = mGeodatabase
                                            .getGeodatabaseFeatureTables().get(0);
                                    pointsGeodatabaseFeatureTable.loadAsync();
                                    FeatureLayer geodatabaseFeatureLayer = new FeatureLayer(pointsGeodatabaseFeatureTable);
                                    // add geodatabase layer to the map as a feature layer and make it selectable
                                    mMapView.getMap().getOperationalLayers().add(geodatabaseFeatureLayer);
                                    mGeodatabaseButton.setVisibility(View.GONE);
                                    Log.i(TAG, "Local geodatabase stored at: " + localGeodatabasePath);
                                } else {
                                    Log.e(TAG, "Error loading geodatabase: " + mGeodatabase.getLoadError().getMessage());
                                }
                            });
                            // set edit state to ready
                            mCurrentEditState = EditState.Ready;
                        } else if (generateGeodatabaseJob.getError() != null) {
                            Log.e(TAG, "Error generating geodatabase: " + generateGeodatabaseJob.getError().getMessage());
                            Toast.makeText(this,
                                    "Error generating geodatabase: " + generateGeodatabaseJob.getError().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "Unknown Error generating geodatabase");
                            Toast.makeText(this, "Unknown Error generating geodatabase", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Error generating geodatabase parameters : " + e.getMessage());
                    Toast.makeText(this, "Error generating geodatabase parameters: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void syncGeodatabase() {
        // create parameters for the sync task
        SyncGeodatabaseParameters syncGeodatabaseParameters = new SyncGeodatabaseParameters();
        syncGeodatabaseParameters.setSyncDirection(SyncGeodatabaseParameters.SyncDirection.BIDIRECTIONAL);
        syncGeodatabaseParameters.setRollbackOnFailure(false);
        // get the layer ID for each feature table in the geodatabase, then add to the sync job
        for (GeodatabaseFeatureTable geodatabaseFeatureTable : mGeodatabase.getGeodatabaseFeatureTables()) {
            long serviceLayerId = geodatabaseFeatureTable.getServiceLayerId();
            SyncLayerOption syncLayerOption = new SyncLayerOption(serviceLayerId);
            syncGeodatabaseParameters.getLayerOptions().add(syncLayerOption);
        }

        final SyncGeodatabaseJob syncGeodatabaseJob = mGeodatabaseSyncTask
                .syncGeodatabase(syncGeodatabaseParameters, mGeodatabase);

        syncGeodatabaseJob.start();

        createProgressDialog(syncGeodatabaseJob);

        syncGeodatabaseJob.addJobDoneListener(() -> {
            if (syncGeodatabaseJob.getStatus() == Job.Status.SUCCEEDED) {
                Toast.makeText(this, "Sync complete", Toast.LENGTH_SHORT).show();
                mGeodatabaseButton.setVisibility(View.INVISIBLE);
            } else {
                Log.e(TAG, "Database did not sync correctly!");
                Toast.makeText(this, "Database did not sync correctly!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createProgressDialog(Job job) {

        ProgressDialog syncProgressDialog = new ProgressDialog(this);
        syncProgressDialog.setTitle("Sync geodatabase job");
        syncProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        syncProgressDialog.setCanceledOnTouchOutside(false);
        syncProgressDialog.show();

        job.addProgressChangedListener(() -> syncProgressDialog.setProgress(job.getProgress()));

        job.addJobDoneListener(syncProgressDialog::dismiss);
    }

    private void selectFeaturesAt(Point point, int tolerance) {
        // define the tolerance for identifying the feature
        final double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
        // create objects required to do a selection with a query
        Envelope envelope = new Envelope(point.getX() - mapTolerance, point.getY() - mapTolerance,
                point.getX() + mapTolerance, point.getY() + mapTolerance, mMapView.getSpatialReference());
        QueryParameters query = new QueryParameters();
        query.setGeometry(envelope);
        mSelectedFeatures = new ArrayList<>();
        // select features within the envelope for all features on the map
        for (Layer layer : mMapView.getMap().getOperationalLayers()) {
            final FeatureLayer featureLayer = (FeatureLayer) layer;
            final ListenableFuture<FeatureQueryResult> featureQueryResultFuture = featureLayer
                    .selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
            // add done loading listener to fire when the selection returns
            featureQueryResultFuture.addDoneListener(() -> {
                // Get the selected features
                final ListenableFuture<FeatureQueryResult> featureQueryResultFuture1 = featureLayer.getSelectedFeaturesAsync();
                featureQueryResultFuture1.addDoneListener(() -> {
                    try {
                        FeatureQueryResult layerFeatures = featureQueryResultFuture1.get();
                        for (Feature feature : layerFeatures) {
                            // Only select points for editing
                            if (feature.getGeometry().getGeometryType() == GeometryType.POINT) {
                                mSelectedFeatures.add(feature);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Select feature failed: " + e.getMessage());
                    }
                });
                // set current edit state to editing
                mCurrentEditState = EditState.Editing;
            });
        }
    }

    private void moveSelectedFeatureTo(Point point) {
        for (Feature feature : mSelectedFeatures) {
            feature.setGeometry(point);
            feature.getFeatureTable().updateFeatureAsync(feature);
        }
        mSelectedFeatures.clear();
        mCurrentEditState = EditState.Ready;
        mGeodatabaseButton.setText("SYNC GEODATABASE");
        mGeodatabaseButton.setVisibility(View.VISIBLE);
    }

    private Point mapPointFrom(MotionEvent motionEvent) {
        // get the screen point
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
                Math.round(motionEvent.getY()));
        // return the point that was clicked in map coordinates
        return mMapView.screenToLocation(screenPoint);
    }

    private void directoryPreplanned() {
        mOfflineMapDirectory = new File(this.getCacheDir() + getString(R.string.preplanned_offline_map_dir));

        if (mOfflineMapDirectory.mkdirs()) {
            Log.i(TAG, "Created directory for offline map in " + mOfflineMapDirectory.getPath());
        } else if (mOfflineMapDirectory.exists()) {
            Log.i(TAG,
                    "Did not create a new offline map directory, one already exists at " + mOfflineMapDirectory.getPath());
        } else {
            Log.e(TAG, "Error creating offline map directory at: " + mOfflineMapDirectory.getPath());
        }
    }

    private void directoryGeodatabase() {
        mOfflineMapDirectoryGeodatabase = new File(this.getCacheDir() + getString(R.string.geodatabase_offline_map_dir));

        if (mOfflineMapDirectoryGeodatabase.mkdirs()) {
            Log.i(TAG, "Created directory for offline map in " + mOfflineMapDirectoryGeodatabase.getPath());
        } else if (mOfflineMapDirectoryGeodatabase.exists()) {
            Log.i(TAG,
                    "Did not create a new offline map directory, one already exists at " + mOfflineMapDirectoryGeodatabase.getPath());
        } else {
            Log.e(TAG, "Error creating offline map directory at: " + mOfflineMapDirectoryGeodatabase.getPath());
        }
    }

    private void showEditableView(@NonNull Feature feature) {
        TransitionManager.beginDelayedTransition(transitionsContainer);
        lyButtons.setVisibility(View.VISIBLE);
        lyTable.removeAllViews();
        lyInfo.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        mMapView.pause();


        btnSave.setOnClickListener(view -> {
            try {
                if (getString(R.string.key_observation).contains("Observacio")) {
                    updateAttributes(getString(R.string.key_observation), etObservation.getText().toString());
                    btnCancel.performClick();
                }

            } catch (NullPointerException e) {
                Log.d("ERROR 1", "Encontrado...");
                Log.d("ERROR 1", e.getMessage());
            }
        });

        btnCancel.setOnClickListener(view -> {
            mMapView.resume();
            mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
            TransitionManager.beginDelayedTransition(transitionsContainer);
            lyTable.removeAllViews();
            lyInfo.setLayoutParams(new LinearLayout.LayoutParams(-1, 520));
            lyInfo.setVisibility(View.GONE);
            lyButtons.setVisibility(View.GONE);
            ibEdit.setVisibility(View.VISIBLE);
        });

        // create a map of all available attributes as name value pairs
        Map<String, Object> attr = feature.getAttributes();
        Set<String> keys = attr.keySet();
        for (String key : keys) {
            Object value = attr.get(key);
            if (value != null) {
                // format observed field value as date
                if (value instanceof GregorianCalendar) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                    value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                }
            } else {
                value = "";
            }
            renderRow(key, value, true);
        }
    }

    private void renderRow(String key, Object value, boolean isEditable) {

        if (key.equals("Tubo")) {
            tvTubeNumber.setText("Tubo #" + value.toString());
        } else if (key.equals("GlobalID")) {
            return;
        }

        LinearLayout lyRow = new LinearLayout(this);
        lyRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        lyRow.setPaddingRelative(6, 6, 6, 6);
        if (isEditable && getString(R.string.key_observation).contains(key)) {
            lyRow.setBackgroundResource(R.color.light_primary);
        } else {
            lyRow.setBackgroundResource(R.color.light_gray);
        }

        TextView tvField = new TextView(this);
        tvField.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        tvField.setTextColor(getResources().getColor(R.color.dark_gray));
        tvField.setGravity(CENTER);
        tvField.setText(key);
        tvField.setTextSize(14);

        View lineViewV = new LinearLayout(this);
        lineViewV.setLayoutParams(new LinearLayout.LayoutParams(2, -1));
        lineViewV.setBackgroundResource(R.color.dark_gray);

        lyRow.addView(tvField);
        lyRow.addView(lineViewV);

        if (isEditable) {
            EditText etValue = new EditText(this);
            etValue.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2));
            etValue.setTextColor(getResources().getColor(R.color.dark_gray));
            etValue.setGravity(CENTER);
            etValue.setText(value.toString());
            etValue.setTextSize(14);
            etValue.setSingleLine(true);
            etValue.setInputType(InputType.TYPE_CLASS_TEXT);
            etValue.setImeOptions(EditorInfo.IME_ACTION_DONE);
            etValue.setEnabled(key.contains(getString(R.string.key_observation)));

            if (getString(R.string.key_observation).contains(key)) {
                etObservation = etValue;
                lyRow.addView(etObservation);
            } else {
                lyRow.addView(etValue);
            }

        } else {
            TextView tvValue = new TextView(this);
            tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2));
            tvValue.setTextColor(getResources().getColor(R.color.dark_gray));
            tvValue.setGravity(CENTER);
            tvValue.setText(value.toString());
            tvValue.setTextSize(14);
            lyRow.addView(tvValue);
        }

        View lineViewH = new LinearLayout(this);
        lineViewH.setLayoutParams(new LinearLayout.LayoutParams(-1, 2));
        lineViewH.setBackgroundResource(R.color.dark_gray);

        lyTable.addView(lyRow);
        lyTable.addView(lineViewH);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission was granted. This would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again.
            mLocationDisplay.startAsync();
        } else {
            // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
            // request permission UX will be shown again, option should be shown to allow never showing the UX again.
            // Alternative would be to disable functionality so request is not shown again.
            Toast.makeText(this, getString(R.string.navigate_ar_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }

    private Envelope createEnvelope() {
        return new Envelope(-75.6, 6.24, -75.5, 6.25,
                SpatialReferences.getWgs84());
    }

    private Multipoint createMultipoint() {
        PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());
        for (DotLocation dot : dotLocations) {
            pointCollection.add(dot.getLat(), dot.getLon());
        }
        return new Multipoint(pointCollection);
    }

    private Polyline createPolyline() {
        PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());
        for (DotLocation dot : dotLocations) {
            pointCollection.add(dot.getLat(), dot.getLon());
        }
        return new Polyline(pointCollection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem toolbarMenu = menu.findItem(R.id.menu);
        toolbarClose = menu.findItem(R.id.close);
        toolbarState = menu.findItem(R.id.state);
        MenuItem toolbarRefresh = menu.findItem(R.id.refresh);

        toolbarMenu.setVisible(false);
        toolbarClose.setVisible(true);
        toolbarRefresh.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.close) {
            Intent intent = new Intent(this, ArActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (itemId == R.id.state) {
            statusChange();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void statusChange() {
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        unregisterReceiver(networkStateReceiver);
        toolbarState.setTitle(state);
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission")
            NetworkInfo ni = manager.getActiveNetworkInfo();
            onNetworkChange(ni);
        }
    };

    private void onNetworkChange(NetworkInfo networkInfo) {
        if (networkInfo != null) {
            if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                if (networkInfo.isConnected()) {
                    // CONNECTED
                    Log.d("State", networkInfo.getState().toString());
                    state = "EN LINEA";
                }
            } else {
                // DISCONNECTED"
                Log.d("State", networkInfo.getState().toString());
                state = "SIN CONEXION";
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onRestart() {
        unregisterReceiver(networkStateReceiver);
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }

    /**
     * Applies changes to the feature, Service Feature Table, and server.
     */
    private boolean updateAttributes(final String key, final String value) {

        // load the selected feature
        mSelectedArcGISFeature.loadAsync();

        // update the selected feature
        mSelectedArcGISFeature.addDoneLoadingListener(() -> {

            if (mSelectedArcGISFeature.getLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
                Log.e(TAG, "Error while loading feature");
            }

            // update the Attributes map with the new selected value for "Observacio"
            mSelectedArcGISFeature.getAttributes().put(key, value);

            try {
                // update feature in the feature table
                ListenableFuture<Void> mapViewResult = mServiceFeatureTable.updateFeatureAsync(mSelectedArcGISFeature);
                mapViewResult.addDoneListener(() -> {
                    // apply change to the server
                    ListenableFuture<List<FeatureEditResult>> serverResult = mServiceFeatureTable.applyEditsAsync();
                    serverResult.addDoneListener(() -> {
                        try {
                            // check if server result successful
                            List<FeatureEditResult> edits = serverResult.get();
                            if (!edits.isEmpty()) {
                                if (!edits.get(0).hasCompletedWithErrors()) {
                                    Toast.makeText(this,
                                            getString(R.string.feature_update_successfull),
                                            Toast.LENGTH_LONG).show();
                                    mFeatureUpdated = true;
                                }
                            } else {
                                Toast.makeText(this,
                                        getString(R.string.feature_update_fail),
                                        Toast.LENGTH_LONG).show();
                                Log.e(TAG, "The attribute type was not changed");
                                mFeatureUpdated = false;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "applying changes to the server failed: " + e.getMessage());
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "updating feature in the feature table failed: " + e.getMessage());
            }
        });
        return mFeatureUpdated;
    }

    enum EditState {
        NotReady,
        Editing,
        Ready
    }
}