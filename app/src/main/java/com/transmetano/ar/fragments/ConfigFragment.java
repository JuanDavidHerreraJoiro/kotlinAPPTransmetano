package com.transmetano.ar.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.transmetano.ar.R;
import com.transmetano.ar.objects.CurrentLayer;

import org.jetbrains.annotations.NotNull;

public class ConfigFragment extends Fragment {

    private MenuFragment.FrCallback listener;

    public ConfigFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConfigFragment.
     */
    public static ConfigFragment newInstance() {
        return new ConfigFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {

        // wire up joystick seek bars to allow manual calibration of height and heading
        Slider sHeading = view.findViewById(R.id.sHeading);
        Slider sAltitude = view.findViewById(R.id.sAltitude);
        Slider sBaseSurface = view.findViewById(R.id.sBaseSurface);

        sHeading.setValue(CurrentLayer.getHeading());
        sAltitude.setValue(CurrentLayer.getAltitude());
        sBaseSurface.setValue(CurrentLayer.getBaseSurface());

        EditText etDistance = view.findViewById(R.id.etDistance);
        etDistance.setText(String.valueOf(CurrentLayer.getMaxRange()));

        etDistance.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER ||
                            keyCode == EditorInfo.IME_ACTION_DONE)) {
                if (etDistance.getText().toString().isEmpty() ||
                        Integer.parseInt(etDistance.getText().toString()) < 10 ||
                        Integer.parseInt(etDistance.getText().toString()) > 100000) {
                    etDistance.setText(getContext().getString(R.string.config_dis_value));
                    Toast.makeText(getContext(), getContext().getString(R.string.config_alert),
                            Toast.LENGTH_SHORT).show();
                } else {
                    CurrentLayer.setMaxRange(Integer.parseInt(etDistance.getText().toString()));
                }
                return true;
            }
            return false;
        });

        // listen for calibration value changes for heading
        sHeading.addOnChangeListener((slider, value, fromUser) -> {
            CurrentLayer.setHeading((int) value);
            listener.headChange((int) value);
        });


        // listen for calibration value changes for altitude
        sAltitude.addOnChangeListener((slider, value, fromUser) -> {
            CurrentLayer.setAltitude((int) value);
            listener.altitudeChange((int) value);
        });

        // listen for visibility of surface changes for altitude
        sBaseSurface.addOnChangeListener((slider, value, fromUser) -> {
            CurrentLayer.setBaseSurface(value);
            listener.surfaceChange(value);
        });

    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof MenuFragment.FrCallback) {
            listener = (MenuFragment.FrCallback) context;
        }
    }

}