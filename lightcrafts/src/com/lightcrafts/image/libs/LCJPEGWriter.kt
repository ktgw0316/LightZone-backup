/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs

import com.lightcrafts.image.export.ResolutionOption
import com.lightcrafts.image.export.ResolutionUnitOption
import com.lightcrafts.image.libs.LCJPEGConstants.*
import com.lightcrafts.image.metadata.EXIFDirectory
import com.lightcrafts.image.metadata.EXIFEncoder
import com.lightcrafts.image.metadata.IPTCDirectory
import com.lightcrafts.image.metadata.ImageMetadata
import com.lightcrafts.image.types.AdobeConstants.ADOBE_APPE_SEGMENT_SIZE
import com.lightcrafts.image.types.AdobeConstants.ADOBE_CTT_UNKNOWN
import com.lightcrafts.image.types.JPEGConstants.*
import com.lightcrafts.image.types.JPEGImageType
import com.lightcrafts.image.types.TIFFConstants
import com.lightcrafts.utils.thread.ProgressThread
import com.lightcrafts.utils.xml.XMLUtil
import java.awt.Point
import java.awt.Rectangle
import java.awt.color.ICC_Profile
import java.awt.image.*
import java.io.IOException
import java.nio.ByteBuffer
import javax.media.jai.PlanarImage

/**
 * An `LCJPEGWriter` is a Java wrapper around the LibJPEG library for writing JPEG
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see [LibJPEG](http://www.ijg.org/)
 */
class LCJPEGWriter {

    /**
     * The height of the image as exported.
     */
    private val m_exportHeight: Int

    /**
     * The width of the image as exported.
     */
    private val m_exportWidth: Int

    /**
     * The resolution (in pixels per unit) of the image as exported.
     */
    private val m_resolution: Int

    /**
     * The resolution unit of the image as exported.
     */
    private val m_resolutionUnit: Int

    /**
     * This is where the native code stores a pointer to the `JPEG` native data
     * structure.  Do not touch this from Java except to compare it to zero.
     */
    @Suppress("unused")
    private val m_nativePtr: Long = 0

    /**
     * Construct an `LCJPEGWriter`.
     *
     * @param fileName The name of the JPEG file to write to.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of [ ][LCJPEGConstants.CS_GRAYSCALE], [LCJPEGConstants.CS_RGB], [ ][LCJPEGConstants.CS_YCbRr], [LCJPEGConstants.CS_CMYK], or [ ][LCJPEGConstants.CS_YCCK].
     * @param quality Image quality: 0-100.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either [ ][TIFFConstants.TIFF_RESOLUTION_UNIT_CM] or [TIFFConstants.TIFF_RESOLUTION_UNIT_INCH].
     */
    @Throws(IOException::class, LCImageLibException::class)
    constructor(fileName: String, width: Int, height: Int,
                colorsPerPixel: Int, colorSpace: Int, quality: Int,
                resolution: Int, resolutionUnit: Int) {
        m_exportWidth = width
        m_exportHeight = height
        m_resolution = resolution
        m_resolutionUnit = resolutionUnit
        openForWriting(
                fileName, width, height, colorsPerPixel, colorSpace, quality
        )
    }

    /**
     * Construct an `LCJPEGWriter`.
     *
     * @param receiver The [LCImageDataReceiver] to send image data to.
     * @param bufSize The size of the buffer (in bytes) to use.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of [ ][LCJPEGConstants.CS_GRAYSCALE], [LCJPEGConstants.CS_RGB], [ ][LCJPEGConstants.CS_YCbRr], [LCJPEGConstants.CS_CMYK], or [ ][LCJPEGConstants.CS_YCCK].
     * @param quality Image quality: 0-100.
     */
    @Throws(LCImageLibException::class)
    constructor(receiver: LCImageDataReceiver, bufSize: Int, width: Int,
                height: Int, colorsPerPixel: Int, colorSpace: Int,
                quality: Int) {
        m_exportWidth = width
        m_exportHeight = height
        m_resolution = ResolutionOption.DEFAULT_VALUE
        m_resolutionUnit = ResolutionUnitOption.DEFAULT_VALUE
        beginWrite(
                receiver, bufSize, width, height, colorsPerPixel, colorSpace,
                quality
        )
    }

    /**
     * Dispose of an `LCJPEGWriter`.
     */
    external fun dispose()

    /**
     * Puts an image, compressing it into a JPEG.
     *
     * @param image The image to compress into a JPEG.
     * @param thread The [ProgressThread] that is putting the JPEG.
     */
    @Throws(LCImageLibException::class)
    @JvmOverloads
    fun putImage(image: RenderedImage, thread: ProgressThread? = null) {
        try {
            val bands = image.sampleModel.numBands
            if (bands == 4 /* CMYK */) {
                //
                // Write a mimicked APPE segment so 3rd-party applications that
                // read CYMK JPEG images will think the creator is Photoshop
                // and thus know to invert the image data.
                //
                writeAdobeSegment(ADOBE_CTT_UNKNOWN)
            }
            writeImage(image, thread)
        } finally {
            dispose()
        }
    }

    /**
     * Puts the given [ImageMetadata] into the JPEG file.  Only EXIF and IPTC metadata are
     * put.  This *must* be called only once and prior to [.putImage].
     *
     * @param imageMetadata The [ImageMetadata] to put.
     */
    @Throws(LCImageLibException::class)
    fun putMetadata(imageMetadata: ImageMetadata) {
        val metadata = imageMetadata.prepForExport(
                JPEGImageType.INSTANCE, m_exportWidth, m_exportHeight,
                m_resolution, m_resolutionUnit, false
        )

        //
        // The binary form of EXIF metadata, if any, has to go before XMP
        // metadata, otherwise Windows Explorer won't see the EXIF metadata.
        //
        val exifDir = metadata.getDirectoryFor(EXIFDirectory::class.java)
        if (exifDir != null) {
            val exifSegBuf = EXIFEncoder.encode(metadata, true).array()
            writeSegment(JPEG_APP1_MARKER.toInt(), exifSegBuf)
        }

        val xmpDoc = metadata.toXMP(false, true)
        val xmpSegBuf = XMLUtil.encodeDocument(xmpDoc, true)
        writeSegment(JPEG_APP1_MARKER.toInt(), xmpSegBuf)

        val iptcDir = metadata.getDirectoryFor(IPTCDirectory::class.java)
        if (iptcDir != null) {
            val iptcSegBuf = (iptcDir as IPTCDirectory).encode(true)
            if (iptcSegBuf != null) {
                writeSegment(JPEG_APPD_MARKER.toInt(), iptcSegBuf)
            }
        }
    }

    /**
     * Sets the ICC profile of the JPEG image.  This *must* be called only once and prior to
     * [.putImage].
     *
     * @param iccProfile The [ICC_Profile] to set.
     */
    @Throws(LCImageLibException::class)
    fun setICCProfile(iccProfile: ICC_Profile) {
        val iccProfileData = iccProfile.data
        val chunkSize = JPEG_MAX_SEGMENT_SIZE - ICC_PROFILE_HEADER_SIZE

        // We must calculate the total size of all the segments including a header per segment.
        var totalSize = iccProfileData.size + ((iccProfileData.size - 1) / chunkSize + 1) * ICC_PROFILE_HEADER_SIZE

        // Given the total size, we can calculate the number of segments needed.
        val numSegments = (totalSize - 1) / JPEG_MAX_SEGMENT_SIZE + 1

        // Now split the profile data across the number of segments with a header per segment.
        for (i in 0 until numSegments) {
            val segSize = Math.min(JPEG_MAX_SEGMENT_SIZE, totalSize)
            val buf = ByteBuffer.allocate(segSize).apply {
                put("ICC_PROFILE".toByteArray(Charsets.US_ASCII))
                put(0.toByte())
                put((i + 1).toByte())
                put(numSegments.toByte())
                put(iccProfileData, i * chunkSize, segSize - ICC_PROFILE_HEADER_SIZE)
            }.array()
            writeSegment(JPEG_APP2_MARKER.toInt(), buf)
            totalSize -= JPEG_MAX_SEGMENT_SIZE
        }
    }

    /**
     * Compresses and writes a raw set of scanlines to the JPEG image.
     *
     * @param buf The buffer from which to compress the image data.
     * @param offset The offset into the buffer where the image data will begin being read.
     * @param numLines The number of scanlines to compress.
     * @return Returns the number of scanlines written.
     */
    @Synchronized
    @Throws(LCImageLibException::class)
    external fun writeScanLines(buf: ByteArray, offset: Int, numLines: Int, lineStride: Int): Int

    /**
     * Write an APP segment to the JPEG file.
     *
     * @param marker The APP segment marker.
     * @param buf The buffer comprising the raw binary contents for the segment.
     */
    @Throws(LCImageLibException::class)
    external fun writeSegment(marker: Int, buf: ByteArray)

    /**
     * Finalize this class by calling [.dispose].
     */
    @Throws(Throwable::class)
    protected fun finalize() = dispose()

    /**
     * Begin using the [LCImageDataProvider] to get JPEG image data.
     *
     * @param receiver The [LCImageDataReceiver] to send image data to.
     * @param bufSize The size of the buffer (in bytes) to use.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of [ ][LCJPEGConstants.CS_GRAYSCALE], [LCJPEGConstants.CS_RGB], [ ][LCJPEGConstants.CS_YCbRr], [LCJPEGConstants.CS_CMYK], or [ ][LCJPEGConstants.CS_YCCK].
     * @param quality Image quality: 0-100.
     */
    @Throws(LCImageLibException::class)
    private external fun beginWrite(receiver: LCImageDataReceiver, bufSize: Int,
                                    width: Int, height: Int, colorsPerPixel: Int,
                                    colorSpace: Int, quality: Int)

    /**
     * Opens a JPEG file for writing.
     *
     * @param fileName The name of the JPEG file to write to.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of [ ][LCJPEGConstants.CS_GRAYSCALE], [LCJPEGConstants.CS_RGB], [ ][LCJPEGConstants.CS_YCbRr], [LCJPEGConstants.CS_CMYK], or [ ][LCJPEGConstants.CS_YCCK].
     * @param quality Image quality: 0-100.
     */
    @Throws(IOException::class, LCImageLibException::class)
    private fun openForWriting(fileName: String, width: Int, height: Int,
                               colorsPerPixel: Int, colorSpace: Int, quality: Int) {
        val fileNameUtf8 = (fileName + '\u0000').toByteArray(charset("UTF-8"))
        openForWriting(fileNameUtf8, width, height, colorsPerPixel, colorSpace, quality)
    }

    @Throws(IOException::class, LCImageLibException::class)
    private external fun openForWriting(fileNameUtf8: ByteArray, width: Int, height: Int,
                                        colorsPerPixel: Int, colorSpace: Int, quality: Int)

    /**
     * Writes an Adobe (APPE) segment.  The bytes of an Adobe segment are:
     * <blockquote>
     * <table border="0" cellpadding="0">
     * <tr valign="top">
     * <td>0-4&nbsp;</td>
     * <td>String: `Adobe`</td>
    </tr> *
     * <tr valign="top">
     * <td>5-6&nbsp;</td>
     * <td>DCTEncode/DCTDecode version number: 0x0065</td>
    </tr> *
     * <tr valign="top">
     * <td>7-8&nbsp;</td>
     * <td>
     * flags0 0x8000 bit:
     * <blockquote>
     * <table border="0" cellpadding="0">
     * <tr><td>0 =&nbsp;</td><td>downsampling</td></tr>
     * <tr><td>1 =&nbsp;</td><td>blend</td></tr>
    </table> *
    </blockquote> *
    </td> *
    </tr> *
     * <tr valign="top">
     * <td>9-10&nbsp;</td>
     * <td>flags1</td>
    </tr> *
     * <tr valign="top">
     * <td>11&nbsp;</td>
     * <td>
     * Color transformation code:
     * <blockquote>
     * <table border="0" cellpadding="0">
     * <tr><td>0 =&nbsp;</td><td>unknown</td></tr>
     * <tr><td>1 =&nbsp;</td><td>YcbCr</td></tr>
     * <tr><td>2 =&nbsp;</td><td>YCCK</td></tr>
    </table> *
    </blockquote> *
    </td> *
    </tr> *
    </table> *
    </blockquote> *
     * Notes:
     *
     *  *
     * For a color transform code of 0 (unknown), 3-channel images are assumed to be RGB and
     * 4-channel images are assumed to be CMYK.
     *
     *  *
     * Although the Adobe technical note says the version number is 0x65, all Adobe-generated files
     * in existence have version 0x64.
     *
     *
     *
     * @param colorTransformationCode One of `ADOBE_CTT_UNKNOWN`,
     * `ADOBE_CTT_YCBCR`, or `ADOBE_CTT_YCCK`.
     * @see "Adobe Technical Note .5116: Supporting the DCT Filters in PostScript Level 2, Adobe
     * Systems, Inc., November 24, 1992, p. 23."
     */
    @Throws(LCImageLibException::class)
    private fun writeAdobeSegment(colorTransformationCode: Byte) {
        val buf = ByteBuffer.allocate(ADOBE_APPE_SEGMENT_SIZE).apply {
            put("Adobe".toByteArray(Charsets.US_ASCII))
            putShort(0x0064.toShort())  // version number
            putShort(0.toShort())       // flags0
            putShort(0.toShort())       // flags1
            put(colorTransformationCode)
        }.array()
        writeSegment(JPEG_APPE_MARKER.toInt(), buf)
    }

    /**
     * Writes an image, compressing it into a JPEG.
     *
     * @param image The image to compress into a JPEG.
     * @param thread The [ProgressThread] to use, if any.
     */
    @Throws(LCImageLibException::class)
    private fun writeImage(image: RenderedImage, thread: ProgressThread?) {
        val imageWidth = image.width
        val imageHeight = image.height
        val stripRect = Rectangle()

        val indicator = thread?.progressIndicator
        indicator?.setMaximum(imageHeight)

        val bands = image.sampleModel.numBands

        val stripHeight = 8
        val rasterBuffer = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, imageWidth, stripHeight, bands * imageWidth, bands,
                LCImageLibUtil.bandOffset(bands), Point(0, 0))

        var y = 0
        while (y < imageHeight) {
            if (thread?.isCanceled == true) {
                return
            }

            val currentStripHeight = Math.min(stripHeight, imageHeight - y)
            stripRect.setBounds(0, y, imageWidth, currentStripHeight)

            val raster = rasterBuffer.createTranslatedChild(0, y) as WritableRaster

            // Prefetch tiles, uses all CPUs
            if (image is PlanarImage) {
                image.getTiles(image.getTileIndices(raster.bounds))
            }
            image.copyData(raster)

            val csm = raster.sampleModel as ComponentSampleModel
            val offset = LCImageLibUtil.min(*csm.bandOffsets)

            val db = raster.dataBuffer as DataBufferByte

            if (bands == 4 /* CMYK */) {
                //
                // A long-standing Photoshop bug is that CMYK images are stored
                // inverted.  To be compatible with Photoshop, we have to
                // invert CMYK images too.
                //
                LCImageLibUtil.invert(db)
            }

            val lineStride = csm.scanlineStride
            val written = writeScanLines(db.data, offset, currentStripHeight,
                    lineStride)
            if (written != currentStripHeight) {
                throw LCImageLibException("something is wrong: $written != $currentStripHeight")
            }
            indicator?.incrementBy(currentStripHeight)
            y += stripHeight
        }
        indicator?.setIndeterminate(true)
    }

    companion object {

        init {
            System.loadLibrary("LCJPEG")
        }

        /**
         * Gets the colorspace constant used by libJPEG from the number of color components of an
         * image.
         *
         * @param numComponents The number of color components.
         * @return Returns said colorspace constant.
         */
        fun getColorSpaceFromNumComponents(numComponents: Int): Int {
            return when (numComponents) {
                1 -> CS_GRAYSCALE
                3 -> CS_RGB
                4 -> CS_CMYK
                else -> CS_UNKNOWN
            }
        }
    }
}

/* vim:set et sw=4 ts=4: */
