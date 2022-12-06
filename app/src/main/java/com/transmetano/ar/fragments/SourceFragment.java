package com.transmetano.ar.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.Job;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalQueryParameters;
import com.esri.arcgisruntime.portal.PortalQueryResultSet;
import com.esri.arcgisruntime.security.UserCredential;
import com.esri.arcgisruntime.tasks.offlinemap.DownloadPreplannedOfflineMapJob;
import com.esri.arcgisruntime.tasks.offlinemap.DownloadPreplannedOfflineMapParameters;
import com.esri.arcgisruntime.tasks.offlinemap.DownloadPreplannedOfflineMapResult;
import com.esri.arcgisruntime.tasks.offlinemap.OfflineMapTask;
import com.esri.arcgisruntime.tasks.offlinemap.PreplannedMapArea;
import com.esri.arcgisruntime.tasks.offlinemap.PreplannedUpdateMode;
import com.transmetano.ar.R;
import com.transmetano.ar.objects.CurrentLayer;
import com.transmetano.ar.objects.EntityLayer;
import com.transmetano.ar.objects.ServiceResponse;
import com.transmetano.ar.retrofit.ApiClient;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Select the source of data
 */
public class SourceFragment extends Fragment implements ProgressDialogFragment.OnProgressDialogDismissListener {

    private MenuFragment.FrCallback listener;

    private GridLayout glFeatures;


    private static final String TOKEN_PREFS = "token";
    private static final String USER_PREFS = "username";
    private static final String PASS_PREFS = "password";
    private static final String EXPIRES_TOKEN_PREFS = "expiresToken";
    private static final String PREFS = "LoginPreferences";

    private static final String PREFS_LAYERS = "LayerPreferences";
    private static final String LAYER_PREFS = "LastLayer";
    private String layers_prefs_change = "";

    private SharedPreferences prefs;
    private String token = "";
    private String expiresToken = "";

    private Portal mPortal;
    private ListenableFuture<PortalQueryResultSet<PortalItem>> mMoreResults;
    private List<PortalItem> mPortalItemList;
    private PortalQueryResultSet<PortalItem> mPortalQueryResultSet;

    private static final String TAG = SourceFragment.class.getSimpleName();

    private File mOfflineMapDirectory;

    private ListView mPreplannedAreasListView;
    private List<String> mPreplannedMapAreaNames;
    private ArrayAdapter<String> mPreplannedMapAreasAdapter;
    private ListView mDownloadedMapAreasListView;
    private List<String> mDownloadedMapAreaNames;
    private ArrayAdapter<String> mDownloadedMapAreasAdapter;
    private final List<ArcGISMap> mDownloadedMapAreas = new ArrayList<>();
    private Button mDownloadButton;

    private PreplannedMapArea mSelectedPreplannedMapArea;
    private List<PreplannedMapArea> mPreplannedMapAreas;
    private DownloadPreplannedOfflineMapJob mDownloadPreplannedOfflineMapJob;
    private MapView mMapView;
    private GraphicsOverlay mAreasOfInterestGraphicsOverlay;
    private OfflineMapTask mOfflineMapTask;
    private PortalItem portalItem;

    private UserCredential credential;

    private String state = "SIN CONEXION";
    private File directoryArchive;
    private String name_webmap = "";

    public SourceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SourceFragment.
     */
    public static SourceFragment newInstance() {
        return new SourceFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_source, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {

        glFeatures = view.findViewById(R.id.glFeatures);
        getFeatureLayerFromServer();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof MenuFragment.FrCallback) {
            listener = (MenuFragment.FrCallback) context;
        }
    }

    private void getFeatureLayerFromServer() {

        prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        token = prefs.getString(TOKEN_PREFS, "");
        expiresToken = prefs.getString(EXPIRES_TOKEN_PREFS, "");

        statusChange();
        directoryPreplanned();
        if (state == "SIN CONEXION") {

            for (String rest : mOfflineMapDirectory.list()) {
                if (rest.contains("MapTF2_")) {

                    float dp = getContext().getResources().getDisplayMetrics().density;

                    int s8 = (int) (16 / dp);
                    int s14 = (int) (20 / dp);

                    LinearLayoutCompat lyFeature1 = new LinearLayoutCompat(getContext());

                    LinearLayout linearLayout = new LinearLayout(getContext());
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(1000, 1));
                    linearLayout.setBackgroundColor(getContext().getColor(R.color.light_gray));
                    linearLayout.setPadding(0, 10, 0, 10);
                    linearLayout.setWeightSum(1);

                    LinearLayoutCompat lyFeature = new LinearLayoutCompat(getContext());
                    lyFeature.setPadding(s14, s14, s14, s14);

                    ImageView iv_avatar = new ImageView(getContext());
                    iv_avatar.setLayoutParams(new LinearLayout.LayoutParams((int) (320 / dp), (int) (320 / dp)));
                    iv_avatar.setForegroundGravity(Gravity.CENTER_VERTICAL);
                    iv_avatar.setBackground(getContext().getDrawable(R.drawable.mapa_f2));

                    TextView tv_name = new TextView(getContext());
                    tv_name.setLayoutParams(new LinearLayout.LayoutParams(600, (int) (320 / dp)));
                    tv_name.setText(rest);
                    tv_name.setTextColor(getContext().getColor(R.color.white));
                    tv_name.setGravity(Gravity.START);
                    tv_name.setPadding(50, 13, 0, 0);
                    tv_name.setTextSize(14);

                    //Validar cuando este descargado el archivo
                    //Validar cuando el archivo ya exista en el dispositivo
                    boolean change_icon = false;

                    for (String restOffline : mOfflineMapDirectory.list()) {
                        if (restOffline.equals(rest)) {
                            change_icon = true;
                        }
                    }

                    Button bt_Download = new Button(getContext());

                    if (change_icon == true) {
                        bt_Download.setLayoutParams(new LinearLayout.LayoutParams((int) (250 / dp), (int) (250 / dp)));
                        bt_Download.setGravity(Gravity.CENTER);
                        bt_Download.setPadding(13, 13, 13, 13);
                        bt_Download.setBackground(getContext().getDrawable(R.drawable.download_offline_f2));

                    } else {
                        bt_Download.setLayoutParams(new LinearLayout.LayoutParams((int) (250 / dp), (int) (250 / dp)));
                        bt_Download.setGravity(Gravity.CENTER);
                        bt_Download.setPadding(13, 13, 13, 13);
                        bt_Download.setBackground(getContext().getDrawable(R.drawable.download_f2));
                    }

                    TextView tv_spacer = new TextView(getContext());
                    tv_spacer.setLayoutParams(new LinearLayout.LayoutParams(50, (int) (320 / dp)));
                    tv_spacer.setGravity(Gravity.CENTER);
                    tv_spacer.setPadding(13, 13, 13, 13);

                    //Validar cuando el archivo tenga cambios
                    //Validar cuando este con internet
                    Button bt_changes = new Button(getContext());
                    bt_changes.setLayoutParams(new LinearLayout.LayoutParams((int) (250 / dp), (int) (250 / dp)));
                    bt_changes.setGravity(Gravity.CENTER);
                    bt_changes.setPadding(0, 13, 13, 13);
                    bt_changes.setBackground(getContext().getDrawable(R.drawable.not_changes_f2));

                    tv_name.setOnClickListener(view -> {
                        searchWebmap_offline(rest);
                    });

                    lyFeature1.addView(linearLayout);
                    lyFeature.addView(iv_avatar);
                    lyFeature.addView(tv_name);
                    lyFeature.addView(bt_Download);
                    lyFeature.addView(tv_spacer);

                    glFeatures.addView(lyFeature1);
                    glFeatures.addView(lyFeature);
                }
            }

        } else if (state == "EN LINEA") {
            searchWebmap(getString(R.string.key_words));
        }
    }

    private void searchWebmap_offline(String rest) {
        try {
            String delete = "_MapArea";
            delete = rest.replace(delete, "");
            String cambiopref = "Layers_" + delete;

            SharedPreferences prefslayers = getContext().getSharedPreferences(PREFS_LAYERS, Context.MODE_PRIVATE);
            layers_prefs_change = prefslayers.getString(LAYER_PREFS, "");

            String _layer = String.valueOf(new JSONObject(prefslayers.getString(LAYER_PREFS, "")).get("name"));

            String cambio_pref_old = layers_prefs_change.replace(_layer, cambiopref);

            SharedPreferences.Editor prefsEditor = prefslayers.edit();
            prefsEditor.putString(LAYER_PREFS, cambio_pref_old);
            prefsEditor.apply();

            Toast.makeText(getContext(), getString(R.string.source_update_successfull),
                    Toast.LENGTH_LONG).show();

            Log.d("prefs 12", cambio_pref_old);

            listener.closeMenu();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void searchWebmap(String keyword) {
        mPortal = new Portal(getString(R.string.portal_url));
        prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String username = prefs.getString(USER_PREFS, "");
        String password = prefs.getString(PASS_PREFS, "");

        credential = new UserCredential(username, password);
        mPortal.setCredential(credential);
        mPortal.loadAsync();
        // create query parameters specifying the type webmap
        PortalQueryParameters params = new PortalQueryParameters();
        params.setQuery(PortalItem.Type.WEBMAP, null, keyword);
        // find matching portal items. This search may field a large number of results (limited to 10 be default). Set the
        // results limit field on the query parameters to change the default amount.
        ListenableFuture<PortalQueryResultSet<PortalItem>> results = mPortal.findItemsAsync(params);
        results.addDoneListener(() -> {
            try {
                // hide search instructions
                //mSearchInstructionsTextView.setVisibility(View.GONE);
                // update the results list view with matching items
                mPortalQueryResultSet = results.get();
                mPortalItemList = mPortalQueryResultSet.getResults();

                float dp = getContext().getResources().getDisplayMetrics().density;

                for (int i = 0; i < mPortalItemList.size(); i++) {

                    int s8 = (int) (16 / dp);
                    int s14 = (int) (20 / dp);

                    LinearLayoutCompat lyFeature1 = new LinearLayoutCompat(getContext());

                    LinearLayout linearLayout = new LinearLayout(getContext());
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(1000, 1));
                    linearLayout.setBackgroundColor(getContext().getColor(R.color.light_gray));
                    linearLayout.setPadding(0, 10, 0, 10);
                    linearLayout.setWeightSum(1);

                    LinearLayoutCompat lyFeature = new LinearLayoutCompat(getContext());
                    lyFeature.setPadding(s14, s14, s14, s14);

                    int finalI = i;

                    ImageView iv_avatar = new ImageView(getContext());
                    iv_avatar.setLayoutParams(new LinearLayout.LayoutParams((int) (320 / dp), (int) (320 / dp)));
                    iv_avatar.setForegroundGravity(Gravity.CENTER_VERTICAL);
                    iv_avatar.setBackground(getContext().getDrawable(R.drawable.mapa_f2));

                    TextView tv_name = new TextView(getContext());
                    tv_name.setLayoutParams(new LinearLayout.LayoutParams(600, (int) (320 / dp)));
                    tv_name.setText(String.valueOf(mPortalItemList.get(i).getTitle()));
                    tv_name.setTextColor(getContext().getColor(R.color.white));
                    tv_name.setGravity(Gravity.START);
                    tv_name.setPadding(50, 13, 0, 0);
                    tv_name.setTextSize(14);

                    //Validar cuando este descargado el archivo
                    //Validar cuando el archivo ya exista en el dispositivo
                    boolean change_icon = false;

                    for (String rest : mOfflineMapDirectory.list()) {
                        if (rest.equals(mPortalItemList.get(finalI).getTitle() + getString(R.string.key_pre_planned))) {
                            change_icon = true;
                        }
                    }

                    Button bt_Download = new Button(getContext());
                    bt_Download.setLayoutParams(new LinearLayout.LayoutParams((int) (250 / dp), (int) (250 / dp)));
                    bt_Download.setGravity(Gravity.CENTER);
                    bt_Download.setPadding(13, 13, 13, 13);

                    if (change_icon) {
                        bt_Download.setBackground(getContext().getDrawable(R.drawable.download_offline_f2));
                    } else {
                        bt_Download.setBackground(getContext().getDrawable(R.drawable.download_f2));
                    }

                    TextView tv_spacer = new TextView(getContext());
                    tv_spacer.setLayoutParams(new LinearLayout.LayoutParams(50, (int) (320 / dp)));
                    tv_spacer.setGravity(Gravity.CENTER);
                    tv_spacer.setPadding(13, 13, 13, 13);

                    //Validar cuando el archivo tenga cambios
                    //Validar cuando este con internet
                    Button bt_changes = new Button(getContext());
                    bt_changes.setLayoutParams(new LinearLayout.LayoutParams((int) (250 / dp), (int) (250 / dp)));
                    bt_changes.setGravity(Gravity.CENTER);
                    bt_changes.setPadding(0, 13, 13, 13);
                    bt_changes.setBackground(getContext().getDrawable(R.drawable.not_changes_f2));

                    tv_name.setOnClickListener(view -> {
                        Log.d("ERROR 0.0", String.valueOf(mPortalItemList.get(finalI).getTitle()));

                        searchLayers(String.valueOf(mPortalItemList.get(finalI).getTitle()));

                        Toast.makeText(getContext(), "View", Toast.LENGTH_LONG).show();
                    });

                    if (change_icon == false) {
                        bt_Download.setOnClickListener(view -> {
                            createPreplannedAreas(String.valueOf(mPortalItemList.get(finalI).getItemId()));
                            searchLayers(String.valueOf(mPortalItemList.get(finalI).getTitle()));
                            Toast.makeText(getContext(), "Download", Toast.LENGTH_LONG).show();
                            listener.closeMenu();
                        });
                    } else {
                        bt_Download.setOnClickListener(view -> {
                            searchLayers(String.valueOf(mPortalItemList.get(finalI).getTitle()));
                            Toast.makeText(getContext(), "Local File", Toast.LENGTH_LONG).show();
                            listener.closeMenu();
                        });
                    }

                    bt_changes.setOnClickListener(view -> {
                        Toast.makeText(getContext(), "Not Changes", Toast.LENGTH_LONG).show();
                        listener.closeMenu();
                    });

                    lyFeature1.addView(linearLayout);
                    lyFeature.addView(iv_avatar);
                    lyFeature.addView(tv_name);
                    lyFeature.addView(bt_Download);
                    lyFeature.addView(tv_spacer);
                    lyFeature.addView(bt_changes);

                    glFeatures.addView(lyFeature1);
                    glFeatures.addView(lyFeature);
                }

            } catch (Exception e) {
                String error = "Error getting portal query result set: " + e.getMessage();
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                Log.e("TAG", error);
            }
        });
    }

    private void searchLayers(String texto) {
        Call<ServiceResponse> call = ApiClient.getInstance(getString(R.string.base_url))
                .getApi().getFeatureLayersPrivate(token);

        call.enqueue(new Callback<ServiceResponse>() {
            @Override
            public void onResponse(Call<ServiceResponse> call, Response<ServiceResponse> response) {
                ServiceResponse serviceResponse = response.body();
                String nameLayers = getString(R.string.key_layers) + texto;
                if (response.isSuccessful()) {
                    for (EntityLayer entityLayer : serviceResponse.getServices()) {
                        if (nameLayers.equals(entityLayer.getName())) {
                            CurrentLayer.setCurrent(entityLayer);
                            listener.layerChange();
                            listener.closeMenu();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ServiceResponse> call, Throwable t) {
                Log.e("TAG", t.getMessage());
            }
        });
    }

    private void createPreplannedAreas(String id_webmap) {

        Portal portal = new Portal(getString(R.string.portal_url));
        portal.setCredential(credential);

        // create a portal item using the portal and the item id of a map service
        portalItem = new PortalItem(portal, id_webmap);

        mOfflineMapTask = new OfflineMapTask(portalItem);

        mPreplannedMapAreas = new ArrayList<>();
        mPreplannedMapAreaNames = new ArrayList<>();

        mOfflineMapTask.setCredential(credential);

        ListenableFuture<List<PreplannedMapArea>> preplannedMapAreasFuture = mOfflineMapTask.getPreplannedMapAreasAsync();
        preplannedMapAreasFuture.addDoneListener(() -> {
            try {
                mPreplannedMapAreas = preplannedMapAreasFuture.get();
                for (PreplannedMapArea preplannedMapArea : mPreplannedMapAreas) {
                    mPreplannedMapAreaNames.add(preplannedMapArea.getPortalItem().getTitle());
                    mSelectedPreplannedMapArea = preplannedMapArea;
                }

                String pathname = getActivity().getCacheDir()
                        + getString(R.string.preplanned_offline_map_dir)
                        + File.separator
                        + mSelectedPreplannedMapArea.getPortalItem().getTitle();

                boolean file = new File(pathname).exists();


                downloadPreplannedArea();
            } catch (InterruptedException | ExecutionException e) {
                String error = "Failed to get the Preplanned Map Areas from the Offline Map Task.";
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                Log.e(TAG, error);
            }
        });
    }

    private void directoryPreplanned() {
        mOfflineMapDirectory = new File(getActivity().getCacheDir() + getString(R.string.preplanned_offline_map_dir));

        if (mOfflineMapDirectory.mkdirs()) {
            Log.i(TAG, "Created directory for offline map in " + mOfflineMapDirectory.getPath());
        } else if (mOfflineMapDirectory.exists()) {
            Log.i(TAG,
                    "Did not create a new offline map directory, one already exists at " + mOfflineMapDirectory.getPath());
        } else {
            Log.e(TAG, "Error creating offline map directory at: " + mOfflineMapDirectory.getPath());
        }
    }

    private void downloadPreplannedArea() {
        if (mSelectedPreplannedMapArea != null) {
            // create default download parameters from the offline map task
            ListenableFuture<DownloadPreplannedOfflineMapParameters> offlineMapParametersFuture = mOfflineMapTask
                    .createDefaultDownloadPreplannedOfflineMapParametersAsync(mSelectedPreplannedMapArea);
            offlineMapParametersFuture.addDoneListener(() -> {
                try {
                    // get the offline map parameters
                    DownloadPreplannedOfflineMapParameters offlineMapParameters = offlineMapParametersFuture.get();
                    // set the update mode to not receive updates
                    offlineMapParameters.setUpdateMode(PreplannedUpdateMode.NO_UPDATES);
                    // create a job to download the preplanned offline map to a temporary directory
                    mDownloadPreplannedOfflineMapJob = mOfflineMapTask.downloadPreplannedOfflineMap(offlineMapParameters,
                            mOfflineMapDirectory.getPath() + File.separator + mSelectedPreplannedMapArea.getPortalItem().getTitle());
                    // start the job
                    mDownloadPreplannedOfflineMapJob.start();

                    // show progress dialog for download, includes tracking progress
                    showProgressDialog();

                    // when the job finishes
                    mDownloadPreplannedOfflineMapJob.addJobDoneListener(() -> {
                        dismissDialog();
                        // if there's a result from the download preplanned offline map job
                        if (mDownloadPreplannedOfflineMapJob.getStatus() == Job.Status.SUCCEEDED) {
                            DownloadPreplannedOfflineMapResult downloadPreplannedOfflineMapResult = mDownloadPreplannedOfflineMapJob
                                    .getResult();

                            if (mDownloadPreplannedOfflineMapJob != null && !downloadPreplannedOfflineMapResult.hasErrors()) {
                                String error = "Descarga exitosa";
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, error);

                            } else {
                                // collect the layer and table errors into a single alert message
                                StringBuilder stringBuilder = new StringBuilder("Errors: ");
                                Map<Layer, ArcGISRuntimeException> layerErrors = downloadPreplannedOfflineMapResult.getLayerErrors();
                                for (Map.Entry<Layer, ArcGISRuntimeException> layer : layerErrors.entrySet()) {
                                    stringBuilder.append("Layer: ").append(layer.getKey().getName()).append(". Exception: ")
                                            .append(layer.getValue().getMessage()).append(". ");
                                }
                                Map<FeatureTable, ArcGISRuntimeException> tableErrors = downloadPreplannedOfflineMapResult
                                        .getTableErrors();

                                for (Map.Entry<FeatureTable, ArcGISRuntimeException> table : tableErrors.entrySet()) {
                                    stringBuilder.append("Table: ").append(table.getKey().getTableName()).append(". Exception: ")
                                            .append(table.getValue().getMessage()).append(". ");
                                }

                                String error = "Descarga fallida";
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, error);
                            }

                        } else {
                            String error = "Job finished with an error: " + mDownloadPreplannedOfflineMapJob.getError();
                            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, error);
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    String error = "Failed to generate default parameters for the download job: " + e.getCause().getMessage();
                    //Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, error);
                }
            });
        }
    }

    private void dismissDialog() {
        // dismiss progress dialog
        if (findProgressDialogFragment() != null) {
            findProgressDialogFragment().dismiss();
        }
    }

    private ProgressDialogFragment findProgressDialogFragment() {
        return (ProgressDialogFragment) getActivity().getSupportFragmentManager()
                .findFragmentByTag(ProgressDialogFragment.class.getSimpleName());
    }

    @Override
    public void onProgressDialogDismiss() {
        if (mDownloadPreplannedOfflineMapJob != null) {
            mDownloadPreplannedOfflineMapJob.cancel();
        }
    }

    private void showProgressDialog() {
        // show progress of the download preplanned offline map job in a dialog
        if (findProgressDialogFragment() == null) {
            ProgressDialogFragment progressDialogFragment = ProgressDialogFragment
                    .newInstance(getString(R.string.title), getString(R.string.message), getString(R.string.cancel));
            progressDialogFragment.show(getActivity().getSupportFragmentManager(), ProgressDialogFragment.class.getSimpleName());

            // track progress
            mDownloadPreplannedOfflineMapJob.addProgressChangedListener(() -> {
                if (findProgressDialogFragment() != null) {
                    findProgressDialogFragment().setProgress(mDownloadPreplannedOfflineMapJob.getProgress());
                }
            });
        }
    }

    private void statusChange() {

        if (isOnlineNet() == true) {
            state = "EN LINEA";
            Log.d("net accInternet", Boolean.toString(isOnlineNet()));
        } else {
            state = "SIN CONEXION";
        }
    }

    public Boolean isOnlineNet() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.es");

            int val = p.waitFor();
            boolean reachable = (val == 0);

            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }
}