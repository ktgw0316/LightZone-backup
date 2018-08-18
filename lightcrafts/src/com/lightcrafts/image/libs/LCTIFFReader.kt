/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs

import com.lightcrafts.image.BadColorProfileException
import com.lightcrafts.image.metadata.TIFFTags.*
import com.lightcrafts.image.types.TIFFConstants
import com.lightcrafts.image.types.TIFFConstants.*
import com.lightcrafts.jai.JAIContext.*
import com.lightcrafts.jai.opimage.CachedImage
import com.lightcrafts.utils.UserCanceledException
import com.lightcrafts.utils.thread.ProgressThread
import java.awt.*
import java.awt.color.ColorSpace
import java.awt.color.ICC_ColorSpace
import java.awt.color.ICC_Profile
import java.awt.geom.AffineTransform
import java.awt.image.*
import java.io.UnsupportedEncodingException
import javax.media.jai.ImageLayout
import javax.media.jai.PlanarImage
import javax.media.jai.TileCache
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * An `LCTIFFReader` is a Java wrapper around the LibTIFF library.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see [LibTIFF](http://www.remotesensing.org/libtiff/)
 */
internal class LCTIFFReader
/**
 * Construct an `LCTIFFReader` and open a TIFF file.
 *
 * @param fileName The name of the TIFF file to open.
 * @param read2nd If `true`, read the second TIFF image (if present).
 */
@Throws(LCImageLibException::class, UnsupportedEncodingException::class)
@JvmOverloads constructor(fileName: String,
                          private val read2nd: Boolean = false) : LCTIFFCommon(), LCImageReader {

    /**
     * Get the ICC profile of the TIFF image.
     *
     * @return Returns the [ICC_Profile] or `null` if the image doesn't have a
     * color profile.
     */
    val iccProfile: ICC_Profile?
        @Throws(BadColorProfileException::class)
        get() {
            val iccProfileData = getICCProfileData() ?: return null
            try {
                return ICC_Profile.getInstance(iccProfileData)
            } catch (e: IllegalArgumentException) {
                throw BadColorProfileException(null)
            }
        }

    /**
     * Gets the number of strips in the TIFF image.  The image must be stored in strips and not
     * tiles.
     *
     * @return Returns said number of strips.
     * @see .isTiled
     */
    external fun getNumberOfStrips(): Int

    /**
     * Gets the number of tiles in the TIFF image.  The image must be stored in tiles and not
     * strips.
     *
     * @return Returns said number of tiles.
     * @see .isTiled
     */
    external fun getNumberOfTiles(): Int

    /**
     * Returns the size of the strips in bytes.  The image must be stored in strips and not in
     * tiles.
     *
     * @return Returns said size.
     * @see .isTiled
     */
    external fun getStripSize(): Int

    /**
     * Returns the size of the tiles in bytes.  The image must be stored in tiles and not in
     * strips.
     *
     * @return Returns said size.
     * @see .isTiled
     */
    external fun getTileSize(): Int

    /**
     * Returns whether the TIFF image is stored in tiles.
     *
     * @return Returns `true` only if the image is stored in tiles.
     */
    external fun isTiled(): Boolean

    /**
     * Gets the raw ICC profile data (minus the header) from the TIFF image.
     *
     * @return Returns said data as a `byte` array.
     */
    private external fun getICCProfileData(): ByteArray?

    /**
     * Checks whether the current TIFF file is a 2-page (layered) TIFF file created by LightZone.
     *
     * @return Returns `true` only if the TIFF file is a LightZone layers TIFF file.
     */
    private val isLightZoneLayeredTIFF: Boolean
        get() {
            val software = getStringField(TIFF_SOFTWARE)
            if (software == null || !software.startsWith("LightZone")) {
                return false
            }
            //
            // The TIFF has to have exactly 2 pages.
            //
            val pages = getIntField2(TIFF_PAGE_NUMBER, true)
            return pages == 2
        }

    internal val format: TIFF_Format
        @Throws(LCImageLibException::class)
        get() = TIFF_Format()

    /**
     * The actual end-result image.
     */
    private var m_image: PlanarImage? = null

    init {
        openForReading(fileName)
        //
        // If openForReading() fails, it will store 0 in the native pointer.
        //
        if (m_nativePtr == 0L) {
            throw LCImageLibException("Could not open $fileName")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(LCImageLibException::class, UserCanceledException::class)
    override fun getImage(): PlanarImage? = getImage(null)

    /**
     * Gets the TIFF image.
     *
     * @param thread The thread doing the getting.
     * @return Returns said image.
     */
    @Synchronized
    @Throws(LCImageLibException::class, UserCanceledException::class)
    fun getImage(thread: ProgressThread?): PlanarImage? {
        if (m_image == null) {
            try {
                if (read2nd && isLightZoneLayeredTIFF &&
                        !nextDirectory()) {
                    return null
                }
                readImage(thread)
            } finally {
                dispose()
            }
        }
        return m_image
    }

    /**
     * Gets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to get.  The ID should be that of a tag whose
     * value is an integer ([TIFFConstants.TIFF_FIELD_TYPE_USHORT] or [ ][TIFFConstants.TIFF_FIELD_TYPE_ULONG]) and not a string.
     * @return Returns said value.
     * @throws IllegalArgumentException if `tagID` isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see .getStringField
     */
    external fun getIntField(tagID: Int): Int

    /**
     * Gets one of the values of the given TIFF two-value integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to get.  The ID should be that of a tag whose
     * value is an integer ([TIFFConstants.TIFF_FIELD_TYPE_USHORT] or [ ][TIFFConstants.TIFF_FIELD_TYPE_ULONG]) and not a string.
     * @param getSecond If `true`, gets the second value; otherwise gets the first.
     * @return Returns said value.
     * @throws IllegalArgumentException if `tagID` isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see .getStringField
     */
    external fun getIntField2(tagID: Int, getSecond: Boolean): Int

    /**
     * Gets the value of the given TIFF string metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is a string ([TIFFConstants.TIFF_FIELD_TYPE_ASCII]).
     * @return Returns the string value.
     * @throws IllegalArgumentException if `tagID` isn't that of an string metadata field
     * or is otherwise unsupported.
     * @see .getIntField
     * @see .getIntField2
     */
    external fun getStringField(tagID: Int): String?

    /**
     * Reads the next TIFF directory replacing the current one.
     *
     * @return Returns `true` if the next directory was read successfully.
     */
    @Throws(LCImageLibException::class)
    private external fun nextDirectory(): Boolean

    /**
     * Opens a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    private fun openForReading(fileName: String) {
        val fileNameUtf8 = (fileName + '\u0000').toByteArray(charset("UTF-8"))
        openForReading(fileNameUtf8)
    }

    @Throws(LCImageLibException::class)
    private external fun openForReading(fileNameUtf8: ByteArray)

    inner class TIFF_Format @Throws(LCImageLibException::class)
    internal constructor() {

        internal val imageWidth = getIntField(TIFF_IMAGE_WIDTH)
        internal val imageHeight = getIntField(TIFF_IMAGE_LENGTH)
        internal val bitsPerSample = getIntField(TIFF_BITS_PER_SAMPLE)
        internal val samplesPerPixel = getIntField(TIFF_SAMPLES_PER_PIXEL)
        internal val sampleFormat = getIntField(TIFF_SAMPLE_FORMAT)
        internal val extraSamples = getIntField(TIFF_EXTRA_SAMPLES)
        internal val photometric = getIntField(TIFF_PHOTOMETRIC_INTERPRETATION)
        internal val planarConfig = getIntField(TIFF_PLANAR_CONFIGURATION)
        internal val planes = if (planarConfig == TIFF_PLANAR_CONFIGURATION_CHUNKY)
            1
        else
            samplesPerPixel

        internal val tiled = isTiled()
        internal val tiles = if (tiled)
            getNumberOfTiles() / planes
        else
            getNumberOfStrips() / planes

        internal val size = if (tiled) getTileSize() else getStripSize()
        internal val tiffTileWidth = if (tiled) getIntField(TIFF_TILE_WIDTH) else imageWidth
        internal val tiffTileHeight = if (tiled)
            getIntField(TIFF_TILE_LENGTH)
        else
            size / (imageWidth * (samplesPerPixel / planes) * bitsPerSample / 8)

        internal val hasAlpha = extraSamples == TIFF_EXTRA_SAMPLES_ASSOC_ALPHA || extraSamples == TIFF_EXTRA_SAMPLES_UNASSOS_ALPHA
        private val isAlphaPremultiplied = extraSamples == TIFF_EXTRA_SAMPLES_ASSOC_ALPHA

        internal val profile: ICC_Profile?
        internal val colorSpace: ColorSpace
        internal val tiffCcm: ComponentColorModel
        internal val tiffTsm: SampleModel

        init {
            if (bitsPerSample != 8 && bitsPerSample != 16) {
                throw LCImageLibException(
                        "Unsupported TIFF Bit per Sample Value: $bitsPerSample")
            }

            var p: ICC_Profile? = null
            try {
                p = iccProfile
                // TODO: deal with weird photometric interpretations
                if (p == null && photometric == TIFF_PHOTOMETRIC_CIELAB) {
                    p = labProfile
                }
            } catch (ignored: BadColorProfileException) {
            }
            profile = p

            colorSpace = if (profile != null) {
                ICC_ColorSpace(profile)
            } else {
                when (samplesPerPixel - (if (hasAlpha) 1 else 0)) {
                    1 -> gray22ColorSpace
                    3 -> sRGBColorSpace
                    4 -> CMYKColorSpace
                    else -> throw LCImageLibException(
                            "Bad image: $samplesPerPixel samples per pixel."
                    )
                }
            }

            // Color model for the tiff image, can have an alpha channel

            tiffCcm = ComponentColorModel(
                    colorSpace,
                    hasAlpha,
                    isAlphaPremultiplied,
                    Transparency.OPAQUE,
                    if (bitsPerSample == 8)
                        DataBuffer.TYPE_BYTE
                    else if (sampleFormat == TIFF_SAMPLE_FORMAT_INT)
                        DataBuffer.TYPE_SHORT
                    else
                        DataBuffer.TYPE_USHORT
            )

            // Sample model for the readout buffer, large enough to hold a tile or a strip of the TIFF image, can be banded
            tiffTsm = if (planarConfig == TIFF_PLANAR_CONFIGURATION_CHUNKY) {
                tiffCcm.createCompatibleSampleModel(tiffTileWidth, tiffTileHeight)
            } else {
                BandedSampleModel(tiffCcm.transferType, tiffTileWidth,
                        tiffTileHeight, samplesPerPixel)
            }

        }
    }

    class TIFFImage @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    constructor(path: String) : PlanarImage() {

        internal val reader = LCTIFFReader(path)
        private val tf = reader.format

        @Transient
        var tileCache: TileCache? = defaultTileCache
            protected set

        init {
            val layout = ImageLayout(0, 0, tf.imageWidth, tf.imageHeight,
                    0, 0,
                    tf.tiffTileWidth, tf.tiffTileHeight,
                    tf.tiffTsm, tf.tiffCcm)
            this.setImageLayout(layout)
        }

        override fun dispose() {
            super.dispose()
            reader.dispose()
        }

        private fun getTileFromCache(tileX: Int, tileY: Int): Raster? {
            return if (tileCache != null) tileCache!!.getTile(this, tileX, tileY) else null
        }

        private fun addTileToCache(tileX: Int,
                                   tileY: Int,
                                   tile: Raster) {
            if (tileCache != null) {
                tileCache!!.add(this, tileX, tileY, tile, null)
            }
        }

        override fun getTile(tileX: Int, tileY: Int): Raster? {
            val tileN = tileX + tileY * this.getNumXTiles()

            // Make sure the requested tile is inside this image's boundary.
            if (tileX < minTileX || tileX > maxTileX || tileY < minTileY || tileY > maxTileY) {
                return null
            }

            // Check if tile is available in the cache.
            var tile = getTileFromCache(tileX, tileY)
            if (tile != null) {
                return tile
            }

            // tile not in cache
            tile = Raster.createWritableRaster(
                    tf.tiffTsm, Point(tileX * tf.tiffTileWidth, tileY * tf.tiffTileHeight))

            try {
                for (plane in 0 until tf.planes) {
                    readPlane(tile!!, tileY, tileN, plane)
                }
            } catch (e: LCImageLibException) {
                e.printStackTrace()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

            // Cache the result tile.
            addTileToCache(tileX, tileY, tile)

            return tile
        }

        @Throws(LCImageLibException::class)
        private fun readPlane(tile: Raster, tileY: Int, tileN: Int, plane: Int) {
            val buf = tile.dataBuffer
            if (tf.bitsPerSample == 8) {
                val buffer = (buf as DataBufferByte).getData(plane)
                if (tf.tiled) {
                    reader.readTileByte(tileN + plane * tf.tiles, buffer, 0, tf.size)
                } else {
                    reader.readStripByte(tileY + plane * tf.tiles, buffer, 0, tf.size)
                }
            } else {
                val buffer = (buf as DataBufferUShort).getData(plane)
                if (tf.tiled) {
                    reader.readTileShort(tileN + plane * tf.tiles, buffer, 0, tf.size)
                } else {
                    reader.readStripShort(tileY + plane * tf.tiles, buffer, 0, tf.size)
                }
            }
        }
    }

    /**
     * Reads the TIFF image.
     *
     * @param thread The thread that is doing the reading.
     */
    @Throws(LCImageLibException::class, UserCanceledException::class)
    private fun readImage(thread: ProgressThread?) {
        val tf = TIFF_Format()

        // Color model for a LightZone Image, no alpha chaannel
        val imageCcm = ComponentColorModel(
                tf.colorSpace, false, false, Transparency.OPAQUE,
                when {
                    tf.bitsPerSample == 8 -> DataBuffer.TYPE_BYTE
                    tf.sampleFormat == TIFF_SAMPLE_FORMAT_INT -> DataBuffer.TYPE_SHORT
                    else -> DataBuffer.TYPE_USHORT
                }
        )

        // The readout buffer itself
        val db =
                if (!(tf.tiled && !tf.hasAlpha)) {
                    // If the TIFF image is tiled and it doesn't have an alpha channel
                    // then we can read directly into the destination image buffer,
                    // don't allocate the buffer
                    if (tf.bitsPerSample == 8) {
                        if (tf.planes == 1) DataBufferByte(tf.size)
                        else DataBufferByte(tf.size, tf.planes)
                    } else {
                        if (tf.planes == 1) DataBufferUShort(tf.size / 2)
                        else DataBufferUShort(tf.size / 2, tf.planes)
                    }
                } else {
                    null
                }

        val imageTileWidth = if (tf.tiled) tf.tiffTileWidth else TILE_WIDTH
        val imageTileHeight = if (tf.tiled) tf.tiffTileHeight else TILE_HEIGHT

        // Sample model for the output image, interleaved
        val tsm = imageCcm
                .createCompatibleSampleModel(imageTileWidth, imageTileHeight)

        // Layout of the output image
        val layout = ImageLayout(0, 0, tf.imageWidth, tf.imageHeight,
                0, 0, imageTileWidth, imageTileHeight,
                tsm, imageCcm)

        // The output image itself, directly allocated in the file cache
        val image = CachedImage(layout, fileCache)

        val maxTileX = image.getNumXTiles()

        val indicator = thread?.progressIndicator
        indicator?.setMaximum(tf.tiles)

        val imageBounds = Rectangle(0, 0, tf.imageWidth, tf.imageHeight)

        for (tile in 0 until tf.tiles) {
            if (thread?.isCanceled == true) throw UserCanceledException()

            val tileX = if (tf.tiled) tile % maxTileX else 0
            val tileY = if (tf.tiled) tile / maxTileX else tile

            // The actual tile bounds, clipping on the image bounds
            val tileBounds = Rectangle(
                    tileX * tf.tiffTileWidth, tileY * tf.tiffTileHeight,
                    tf.tiffTileWidth, tf.tiffTileHeight).intersection(imageBounds)

            // the corresponding tile data
            val tileData = (tf.samplesPerPixel / tf.planes) * tileBounds.width * tileBounds.height * (if (tf.bitsPerSample == 8) 1 else 2)

            // If the TIFF image is tiled and it doesn't have an alpha channel
            // then we can read directly into the destination image buffer,
            // don't allocate an intermediate raster

            var raster =
                    if (!tf.tiled || tf.hasAlpha)
                        Raster.createWritableRaster(tf.tiffTsm, db, Point(tileBounds.x, tileBounds.y))
                    else
                        null

            for (plane in 0 until tf.planes) {
                val read =
                        if (tf.tiled) {
                            if (!tf.hasAlpha) {
                                raster = image.getWritableTile(tileX, tileY)
                            }
                            val rdb = raster!!.dataBuffer
                            val tileIndex = tile + plane * tf.tiles
                            if (tf.bitsPerSample == 8) {
                                val buffer = (rdb as DataBufferByte).getData(plane)
                                readTileByte(tileIndex, buffer, 0, tileData)
                            } else {
                                val buffer = (rdb as DataBufferUShort).getData(plane)
                                readTileShort(tileIndex, buffer, 0, tileData)
                            }
                        } else {
                            val stripIndex = tileY + plane * tf.tiles
                            if (tf.bitsPerSample == 8) {
                                val buffer = (db as DataBufferByte).getData(plane)
                                readStripByte(stripIndex, buffer, 0, tileData)
                            } else {
                                val buffer = (db as DataBufferUShort).getData(plane)
                                readStripShort(stripIndex, buffer, 0, tileData)
                            }
                        }
                if (read != tileData) throw LCImageLibException("Broken TIFF File")
            }
            if (raster != null) { // !tf.tiled || tf.hasAlpha
                val bandList = IntArray(tf.samplesPerPixel) { it }
                image.data = raster.createChild(
                        raster.minX, raster.minY, raster.width, raster.height,
                        raster.minX, raster.minY, bandList)
            }
            indicator?.incrementBy(1)
        }
        indicator?.setIndeterminate(true)

        m_image = image
    }

    /*
     * NOTE: TIFF read functions can be called in parallel for different tiles, make them synchronized...
     */

    /**
     * Reads and decodes and encoded strip from the TIFF image.  The image must be stored in strips
     * and not tiles.
     *
     * @param stripIndex The index of the strip to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see .isTiled
     * @see .getStripSize
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    private external fun readStripByte(stripIndex: Int, buf: ByteArray, offset: Long,
                                       stripSize: Int): Int

    /**
     * Reads and decodes and encoded strip from the TIFF image.  The image must be stored in strips
     * and not tiles.
     *
     * @param stripIndex The index of the strip to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see .isTiled
     * @see .getStripSize
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    private external fun readStripShort(stripIndex: Int, buf: ShortArray, offset: Long,
                                        stripSize: Int): Int

    /**
     * Reads and decodes and encoded tile from the TIFF image.  The image must be stored in tiles
     * and not strips.
     *
     * @param tileIndex The index of the tile to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see .isTiled
     * @see .getTileSize
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    private external fun readTileByte(tileIndex: Int, buf: ByteArray, offset: Long,
                                      tileSize: Int): Int

    /**
     * Reads and decodes and encoded tile from the TIFF image.  The image must be stored in tiles
     * and not strips.
     *
     * @param tileIndex The index of the tile to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see .isTiled
     * @see .getTileSize
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    private external fun readTileShort(tileIndex: Int, buf: ShortArray, offset: Long,
                                       tileSize: Int): Int

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val image = TIFFImage(args[0])
                val imagePanel = object : JPanel() {
                    public override fun paintComponent(g: Graphics) {
                        (g as Graphics2D).drawRenderedImage(image, AffineTransform())
                    }
                }
                imagePanel.preferredSize = Dimension(image.getWidth(), image.getHeight())
                val frame = JFrame("TIFF Image")
                with(frame) {
                    contentPane = JScrollPane(imagePanel)
                    pack()
                    setSize(800, 600)
                    isVisible = true
                }
            } catch (e: LCImageLibException) {
                e.printStackTrace()
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: HeadlessException) {
                e.printStackTrace()
            }

        }
    }
}

/* vim:set et sw=4 ts=4: */
