/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs

import com.lightcrafts.image.types.AdobeEmbedJPEGSegmentFilter
import com.lightcrafts.image.types.AdobeJPEGSegmentFilter
import com.lightcrafts.image.types.JPEGConstants.JPEG_APPC_MARKER
import com.lightcrafts.image.types.JPEGConstants.JPEG_APPE_MARKER
import com.lightcrafts.image.types.JPEGImageInfo
import com.lightcrafts.jai.JAIContext.*
import com.lightcrafts.jai.opimage.CachedImage
import com.lightcrafts.utils.UserCanceledException
import com.lightcrafts.utils.thread.ProgressThread
import java.awt.Point
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.Raster
import java.io.FileNotFoundException
import java.io.UnsupportedEncodingException
import javax.media.jai.ImageLayout
import javax.media.jai.PlanarImage

/**
 * An `LCJPEGReader` is a Java wrapper around the LibJPEG library for reading JPEG
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see [LibJPEG](http://www.ijg.org/)
 */
class LCJPEGReader : LCImageReader {

    /**
     * This is `true` only if the JPEG file has an Adobe (APPE) segment.
     */
    private val hasAdobeSegment: Boolean

    /**
     * This is `true` only if the JPEG file has an Adobe Embed (APPC) segment.
     */
    private val hasAdobeEmbedMarker: Boolean

    /**
     * The actual end-result image.
     */
    private var image: PlanarImage? = null

    /**
     * The number of colors per pixel. This is set from native code.
     */
    @Suppress("unused")
    var colorsPerPixel: Int = 0

    /**
     * The colorspace of the image, one of: `CS_GRAYSCALE`, `CS_RGB`, `
     * CS_YCbRr`, `CS_CMYK`, `CS_YCCK`, or `CS_UNKNOWN`. This
     * is set from native code.
     */
    @Suppress("unused")
    private var colorSpace: Int = 0

    /**
     * The image width. This is set from native code.
     */
    @Suppress("unused")
    var width: Int = 0

    /**
     * The image height. This is set from native code.
     */
    @Suppress("unused")
    var height: Int = 0

    /**
     * This is where the native code stores a pointer to the `JPEG` native data
     * structure. Do not touch this from Java except to compare it to zero.
     */
    @Suppress("unused")
    private var m_nativePtr: Long = 0

    /**
     * Construct an `LCJPEGReader`.
     *
     * @param fileName The name of the JPEG file to read.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param jpegInfo The [JPEGImageInfo] of the image, or `null`.
     */
    @Throws(FileNotFoundException::class, LCImageLibException::class, UnsupportedEncodingException::class)
    @JvmOverloads constructor(fileName: String, maxWidth: Int = 0, maxHeight: Int = 0, jpegInfo: JPEGImageInfo? = null) {
        openForReading(fileName, maxWidth, maxHeight)
        hasAdobeSegment = jpegInfo?.getFirstSegmentFor(JPEG_APPE_MARKER, AdobeJPEGSegmentFilter()) != null
        hasAdobeEmbedMarker = jpegInfo?.getFirstSegmentFor(JPEG_APPC_MARKER, AdobeEmbedJPEGSegmentFilter()) != null
    }

    /**
     * Construct an `LCJPEGReader`.
     *
     * @param provider The [LCImageDataProvider] to get image data from.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    @Throws(LCImageLibException::class)
    @JvmOverloads constructor(provider: LCImageDataProvider, maxWidth: Int = 0, maxHeight: Int = 0) {
        hasAdobeSegment = false
        hasAdobeEmbedMarker = false
        beginRead(provider, DEFAULT_BUF_SIZE, maxWidth, maxHeight)
    }

    /**
     * Construct an `LCJPEGReader`.
     *
     * @param provider The [LCImageDataProvider] to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    @Throws(LCImageLibException::class)
    constructor(provider: LCImageDataProvider, bufSize: Int, maxWidth: Int = 0, maxHeight: Int = 0) {
        hasAdobeSegment = false
        hasAdobeEmbedMarker = false
        beginRead(provider, bufSize, maxWidth, maxHeight)
    }

    /**
     * Dispose of an `LCJPEGReader`.
     */
    @Throws(LCImageLibException::class)
    external fun dispose()

    @Throws(LCImageLibException::class, UserCanceledException::class)
    override fun getImage(): PlanarImage? = getImage(null, null)

    @Throws(LCImageLibException::class, UserCanceledException::class)
    fun getImage(cs: ColorSpace?): PlanarImage? = getImage(null, cs)

    /**
     * Gets the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param cs The [ColorSpace] to use.
     * @return Returns said image.
     */
    @Synchronized
    @Throws(LCImageLibException::class, UserCanceledException::class)
    fun getImage(thread: ProgressThread? = null, cs: ColorSpace? = null): PlanarImage? {
        if (image == null) {
            var userCanceled = false
            try {
                readImage(thread, cs)
            } catch (e: UserCanceledException) {
                userCanceled = true
                throw e
            } finally {
                try {
                    dispose()
                } catch (e: LCImageLibException) {
                    //
                    // The JPEG library will complain if dispose() is called
                    // before the entire image has been read ("Application
                    // transferred too few scanlines") because the user clicked
                    // the "Cancel" button.  Therefore, ignore any exception if
                    // this is the case, but rethrow it otherwise.
                    //
                    if (!userCanceled) {
                        throw e
                    }
                }

            }
        }
        return image
    }

    /**
     * Reads and decodes and encoded set of scanlines from the JPEG image.
     *
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param numLines The number of scanlines to read.
     * @return Returns the number of scanlines read.
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    external fun readScanLines(buf: ByteArray, offset: Long, numLines: Int): Int

    /**
     * Finalize this class by calling [.dispose].
     */
    @Throws(Throwable::class)
    protected fun finalize() = dispose()

    /**
     * Begin using the [LCImageDataProvider] to get JPEG image data.
     *
     * @param provider The [LCImageDataProvider] to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    @Throws(LCImageLibException::class)
    private external fun beginRead(
            provider: LCImageDataProvider, bufSize: Int, maxWidth: Int, maxHeight: Int)

    /**
     * Open a JPEG file for reading.
     *
     * @param fileName The name of the JPEG file to open.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    @Throws(FileNotFoundException::class, LCImageLibException::class, UnsupportedEncodingException::class)
    private fun openForReading(fileName: String, maxWidth: Int, maxHeight: Int) {
        val fileNameUtf8 = (fileName + '\u0000').toByteArray(charset("UTF-8"))
        openForReading(fileNameUtf8, maxWidth, maxHeight)
    }

    @Throws(FileNotFoundException::class, LCImageLibException::class)
    private external fun openForReading(fileNameUtf8: ByteArray, maxWidth: Int, maxHeight: Int)

    /**
     * Reads the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param colorSpace The [ColorSpace] to use.
     */
    @Throws(LCImageLibException::class, UserCanceledException::class)
    private fun readImage(thread: ProgressThread?, colorSpace: ColorSpace?) {
        val indicator = thread?.progressIndicator
        indicator?.setMaximum(height)

        // TODO: deal with color models other than rgb and grayscale
        val cs = colorSpace ?: when (colorsPerPixel) {
            1 -> gray22ColorSpace
            3 -> sRGBColorSpace
            else -> CMYKColorSpace
        }

        // Color model for the image (and everything else).
        val ccm = ComponentColorModel(cs!!, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE)

        // Sample model for the readout buffer large enough to hold a tile or a strip of the image.
        val jpegTsm = ccm.createCompatibleSampleModel(width, TILE_HEIGHT)

        // The readout buffer itself.
        val db = DataBufferByte(colorsPerPixel * width * TILE_HEIGHT)

        // Sample model for the output image.
        val tsm = ccm.createCompatibleSampleModel(TILE_WIDTH, TILE_HEIGHT)

        // Layout of the output image.
        val layout = ImageLayout(0, 0, width, height, 0, 0, TILE_WIDTH, TILE_HEIGHT, tsm, ccm)

        // The output image itself, directly allocated in the file cache.
        val cachedImage = CachedImage(layout, fileCache)

        // Load Image Data
        var tileY = 0
        var totalLinesRead = 0
        while (totalLinesRead < height) {
            if (thread?.isCanceled == true) {
                throw UserCanceledException()
            }

            val tileHeight = Math.min(TILE_HEIGHT, height - totalLinesRead)

            // Wrap the data buffer with a Raster representing the input data.
            val raster = Raster.createWritableRaster(jpegTsm, db, Point(0, tileY * TILE_HEIGHT))

            val linesRead = readScanLines(db.data, 0, tileHeight)
            if (linesRead <= 0) {
                println("Problem with readScanLines, returned: $linesRead")
                break
            }

            if (hasAdobeSegment && colorsPerPixel == 4 && !hasAdobeEmbedMarker) {
                //
                // CMYK JPEG images generated by Photoshop are inverted, so we
                // have to invert the data to make it look right.
                //
                LCImageLibUtil.invert(db)
            }

            totalLinesRead += linesRead

            cachedImage.data = raster
            indicator?.incrementBy(linesRead)
            tileY++
        }
        indicator?.setIndeterminate(true)
        image = cachedImage
    }

    companion object {

        /**
         * The default buffer size for use with [.LCJPEGReader]
         */
        private const val DEFAULT_BUF_SIZE = 32 * 1024

        init {
            System.loadLibrary("LCJPEG")
        }
    }
}
/* vim:set et sw=4 ts=4: */
