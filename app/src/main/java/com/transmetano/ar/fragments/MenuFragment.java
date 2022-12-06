package com.transmetano.ar.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.transmetano.ar.R;

import org.jetbrains.annotations.NotNull;

/**
 * Show the option that the user have
 */
public class MenuFragment extends Fragment {

    private FrCallback listener;

    public MenuFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MenuFragment.
     */
    public static MenuFragment newInstance() {
        return new MenuFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {

        AppCompatTextView tvSource = view.findViewById(R.id.tvSource);
        AppCompatTextView tvConfig = view.findViewById(R.id.tvConfig);
        AppCompatTextView tvAbout = view.findViewById(R.id.tvAbout);
        AppCompatTextView tvLogout = view.findViewById(R.id.tvLogout);

        tvSource.setOnClickListener(v -> listener.onSourceClick());
        tvConfig.setOnClickListener(v -> listener.onConfigClick());
        tvAbout.setOnClickListener(v -> listener.onAboutClick());
        tvLogout.setOnClickListener(v -> listener.onLogoutClick());

    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof FrCallback) {
            listener = (FrCallback) context;
        }
    }

    public interface FrCallback {
        void onSourceClick();

        void onConfigClick();

        void onAboutClick();

        void onLogoutClick();

        void closeMenu();

        void headChange(int value);

        void altitudeChange(int value);

        void surfaceChange(float value);

        void layerChange();
    }

}