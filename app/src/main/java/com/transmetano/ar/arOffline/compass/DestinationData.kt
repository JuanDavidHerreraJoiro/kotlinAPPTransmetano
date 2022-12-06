package com.transmetano.ar.arOffline.compass

import com.transmetano.ar.objects.DotLocation

data class DestinationData(
    val currentDestinationAzimuth: Float,
    val distanceToDestination: Int,
    val destinationLocation: DotLocation
)
