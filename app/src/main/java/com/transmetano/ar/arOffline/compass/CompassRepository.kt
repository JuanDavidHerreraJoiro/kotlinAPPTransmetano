package com.transmetano.ar.arOffline.compass

import android.location.Location
import com.transmetano.ar.objects.DotLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class CompassRepository {

    companion object {
        private const val MAXIMUM_ANGLE = 360
    }

    var destinationsLocation: List<DotLocation> = listOf()

    fun getMaxDistance(destinations: List<DestinationData>) =
        destinations.maxBy { it.distanceToDestination }.distanceToDestination

    fun getMinDistance(destinations: List<DestinationData>) =
        destinations.minBy { it.distanceToDestination }.distanceToDestination

    fun handleDestination(
        currentLocation: DotLocation,
        destinationLocation: DotLocation,
        currentAzimuth: Float
    ): DestinationData {

        val headingAngle = calculateHeadingAngle(currentLocation, destinationLocation)

        val currentDestinationAzimuth =
            (headingAngle - currentAzimuth + MAXIMUM_ANGLE) % MAXIMUM_ANGLE

        val distanceToDestination = getDistanceBetweenPoints(
            currentLocation,
            destinationLocation
        )

        return DestinationData(
            currentDestinationAzimuth,
            distanceToDestination,
            destinationLocation
        )
    }

    fun getDistanceBetweenPoints(
        currentLocation: DotLocation?,
        destinationLocation: DotLocation?
    ): Int {

        val locationA = Location("A")
        locationA.latitude = currentLocation?.lat ?: 0.0
        locationA.longitude = currentLocation?.lon ?: 0.0

        val locationB = Location("B")
        locationB.latitude = destinationLocation?.lat ?: 0.0
        locationB.longitude = destinationLocation?.lon ?: 0.0

        return locationA.distanceTo(locationB).roundToInt()
    }

    private fun calculateHeadingAngle(
        currentLocation: DotLocation,
        destinationLocation: DotLocation
    ): Float {
        val currentLatitudeRadians = Math.toRadians(currentLocation.lat)
        val destinationLatitudeRadians = Math.toRadians(destinationLocation.lat)
        val deltaLongitude =
            Math.toRadians(destinationLocation.lon - currentLocation.lon)

        val y = cos(currentLatitudeRadians) * sin(destinationLatitudeRadians) -
                sin(currentLatitudeRadians) * cos(destinationLatitudeRadians) * cos(deltaLongitude)
        val x = sin(deltaLongitude) * cos(destinationLatitudeRadians)
        val headingAngle = Math.toDegrees(atan2(x, y)).toFloat()

        return (headingAngle + MAXIMUM_ANGLE) % MAXIMUM_ANGLE
    }

}
