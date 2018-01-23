/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.providers

/**
 * A `GPSProvider` provides the latitude and longitude of an
 * image.
 *
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
interface GPSProvider : ImageMetadataProvider {
    /**
     * Gets the latitude of the image.
     *
     * @return Returns said latitude or null if it's unavailable.
     */
    fun getGPSLatitude() : Double?

    /**
     * Gets the longitude of the image.
     *
     * @return Returns said longitude or null if it's unavailable.
     */
    fun getGPSLongitude() : Double?

    /**
     * Gets the latitude of the image as DMS (degrees, minutes, and seconds)
     * string, e.g. 4124'12.2"N
     *
     * @return Returns said string or empty string if it's unavailable.
     */
    fun getGPSLatitudeDMS() : String

    /**
     * Gets the longitude of the image as DMS (degrees, minutes, and seconds)
     * string, e.g. 210'26.5"E
     *
     * @return Returns said string or empty string if it's unavailable.
     */
    fun getGPSLongitudeDMS() : String
}
