/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata

import com.lightcrafts.image.metadata.GPSTags.*
import com.lightcrafts.image.metadata.ImageMetaType.*
import com.lightcrafts.image.metadata.providers.GPSProvider
import com.lightcrafts.image.metadata.values.UnsignedRationalMetaValue
import com.lightcrafts.utils.Rational
import java.util.*

/**
 * A `GPSDirectory` is-an [ImageMetadataDirectory] for holding
 * GPS metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
class GPSDirectory : ImageMetadataDirectory(), GPSProvider {

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;GPSF&quot;.
     */
    override fun getName(): String = "GPS"

    override fun getTagInfoFor(id: Int?) = m_tagsByID[id]!!

    override fun getTagInfoFor(name: String) = m_tagsByName[name]!!

    override fun getTagLabelBundle() = m_tagBundle

    override fun getTagsInterface() = GPSTags::class.java

    override fun getGPSLatitude() = readGPSCoordinate(GPS_LATITUDE, GPS_LATITUDE_REF, "N")

    override fun getGPSLongitude() = readGPSCoordinate(GPS_LONGITUDE, GPS_LONGITUDE_REF, "E")

    override fun getGPSLatitudeDMS() = readGPSCoordinateDMS(GPS_LATITUDE, GPS_LATITUDE_REF)

    override fun getGPSLongitudeDMS() = readGPSCoordinateDMS(GPS_LONGITUDE, GPS_LONGITUDE_REF)

    private fun readGPSCoordinate(tagID: Int, refTagID: Int, orientation: String): Double? {
        val (values, refString) = readMetadata(tagID, refTagID) ?: return null
        val sign = if (refString.equals(orientation, ignoreCase = true)) 1 else -1

        return sign * (values[0].toDouble()
                + values[1].toDouble() / 60
                + values[2].toDouble() / 3600)
    }

    private fun readGPSCoordinateDMS(tagID: Int, refTagID: Int): String {
        val (values, refString) = readMetadata(tagID, refTagID) ?: return ""

        return (values[0].toInt().toString() + "\u00B0"
                + values[1].toInt() + "'"
                + values[2].toFloat() + "\""
                + refString)
    }

    private fun readMetadata(tagID: Int, refTagID: Int): Pair<Array<Rational>, String>? {
        val metaValue = getValue(tagID) ?: return null
        val values = (metaValue as UnsignedRationalMetaValue).rationalValues
        if (values.size != 3) {
            return null
        }

        val refMetaValue = getValue(refTagID) ?: return null
        val refString = refMetaValue.stringValue ?: return null

        return Pair(values, refString)
    }

    private companion object {

        /**
         * This is where the actual labels for the tags are.
         */
        private val m_tagBundle = ResourceBundle.getBundle(
                "com.lightcrafts.image.metadata.GPSTags"
        )!!

        /**
         * A mapping of tags by ID.
         */
        private val m_tagsByID = HashMap<Int, ImageMetaTagInfo>()

        /**
         * A mapping of tags by name.
         */
        private val m_tagsByName = HashMap<String, ImageMetaTagInfo>()

        /**
         * Add the tag mappings.
         *
         * @param id The tag's ID.
         * @param name The tag's name.
         * @param type The tag's [ImageMetaType].
         */
        private fun add(id: Int, name: String, type: ImageMetaType) {
            val tagInfo = ImageMetaTagInfo(id, name, type, false)
            m_tagsByID[id] = tagInfo
            m_tagsByName[name] = tagInfo
        }

        init {
            add(GPS_ALTITUDE, "GPSAltitude", META_URATIONAL)
            add(GPS_ALTITUDE_REF, "GPSAltitudeRef", META_UBYTE)
            add(GPS_AREA_INFORMATION, "GPSAreaInformation", META_UNDEFINED)
            add(GPS_DATE_STAMP, "GPSDateStamp", META_STRING)
            add(GPS_DEST_BEARING, "GPSDestBearing", META_URATIONAL)
            add(GPS_DEST_BEARING_REF, "GPSDestBearingRef", META_STRING)
            add(GPS_DEST_DISTANCE, "GPSDestDistance", META_URATIONAL)
            add(GPS_DEST_DISTANCE_REF, "GPSDestDistanceRef", META_STRING)
            add(GPS_DEST_LATITUDE, "GPSDestLatitude", META_URATIONAL)
            add(GPS_DEST_LATITUDE_REF, "GPSDestLatitudeRef", META_STRING)
            add(GPS_DEST_LONGITUDE, "GPSDestLongitude", META_URATIONAL)
            add(GPS_DEST_LONGITUDE_REF, "GPSDestLongitudeRef", META_STRING)
            add(GPS_DIFFERENTIAL, "GPSDifferential", META_USHORT)
            add(GPS_DOP, "GPSDop", META_URATIONAL)
            add(GPS_IMG_DIRECTION, "GPSImgDirection", META_URATIONAL)
            add(GPS_IMG_DIRECTION_REF, "GPSImgDirectionRef", META_STRING)
            add(GPS_LATITUDE, "GPSLatitude", META_URATIONAL)
            add(GPS_LATITUDE_REF, "GPSLatitudeRef", META_STRING)
            add(GPS_LONGITUDE, "GPSLongitude", META_URATIONAL)
            add(GPS_LONGITUDE_REF, "GPSLongitudeRef", META_STRING)
            add(GPS_MAP_DATUM, "GPSMapDatum", META_STRING)
            add(GPS_MEASURE_MODE, "GPSMeasureMode", META_STRING)
            add(GPS_PROCESSING_METHOD, "GPSProcessingMethod", META_UNDEFINED)
            add(GPS_SATELLITES, "GPSSatellites", META_STRING)
            add(GPS_SPEED, "GPSSpeed", META_URATIONAL)
            add(GPS_SPEED_REF, "GPSSpeedRef", META_STRING)
            add(GPS_STATUS, "GPSStatus", META_STRING)
            add(GPS_TIME_STAMP, "GPSTimeStamp", META_URATIONAL)
            add(GPS_TRACK, "GPSTrack", META_URATIONAL)
            add(GPS_TRACK_REF, "GPSTrackRef", META_STRING)
            add(GPS_VERSION_ID, "GPSVersionID", META_UBYTE)
        }
    }
}
/* vim:set et sw=4 ts=4: */
