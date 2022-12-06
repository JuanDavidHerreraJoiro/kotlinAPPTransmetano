package com.transmetano.ar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}