package com.transmetano.ar.arOffline.compass

import com.transmetano.ar.arOffline.orientation.OrientationData
import com.transmetano.ar.objects.DotLocation

data class CompassData(
    val orientationData: OrientationData,
    val destinations: List<DestinationData>,
    val maxDistance: Int,
    val minDistance: Int,
    val currentLocation: DotLocation
)
