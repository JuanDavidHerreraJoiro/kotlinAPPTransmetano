package com.transmetano.ar.retrofit;


import com.transmetano.ar.objects.ServiceResponse;
import com.transmetano.ar.objects.TokenResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiInterface {

    //public layers query
    @GET("rest/services?f=json")
    @Streaming
    Call<ServiceResponse> getFeatureLayers();

    @GET("rest/services/Puntos_Donayd/FeatureServer/0/{id}")
    Call<ServiceResponse> getPoint(@Path("id") int id_point);

    @GET("/sharing/generateToken?f=json&referer=https//.arcgis.com/")
    Call<TokenResponse> getToken(@Query("username") String username, @Query("password") String password);

    //private and public layers query
    @GET("rest/services?f=json")
    Call<ServiceResponse> getFeatureLayersPrivate(@Query("token") String token);
}