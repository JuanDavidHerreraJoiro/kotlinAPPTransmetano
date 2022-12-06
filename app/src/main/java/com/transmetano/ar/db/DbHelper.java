package com.transmetano.ar.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "geoData.db";
    public static final String TABLE_POINTS = "t_points";

    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_POINTS + "(" +
                "FID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Espesor_p REAL," +
                "Diametro_p INTEGER," +
                "Longitud_m REAL," +
                "No__tubo TEXT," +
                "No__colada TEXT," +
                "Longitud REAL," +
                "Latitud REAL," +
                "Altitud TEXT," +
                "Observacio TEXT," +
                "x REAL," +
                "y REAL," +
                "z TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_POINTS);
        onCreate(sqLiteDatabase);
    }
}

