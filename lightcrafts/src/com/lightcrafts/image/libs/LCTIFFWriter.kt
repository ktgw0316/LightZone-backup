/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs

import com.lightcrafts.image.export.ResolutionOption
import com.lightcrafts.image.export.ResolutionUnitOption
import com.lightcrafts.image.metadata.*
import com.lightcrafts.image.metadata.EXIFConstants.EXIF_FIELD_SIZE
import com.lightcrafts.image.metadata.EXIFTags.EXIF_GPS_IFD_POINTER
import com.lightcrafts.image.metadata.EXIFTags.EXIF_IFD_POINTER
import com.lightcrafts.image.metadata.TIFFTags.*
import com.lightcrafts.image.types.TIFFConstants
import com.lightcrafts.image.types.TIFFConstants.*
import com.lightcrafts.image.types.TIFFImageType
import com.lightcrafts.utils.UserCanceledException
import com.lightcrafts.utils.Version
import com.lightcrafts.utils.file.OrderableRandomAccessFile
import com.lightcrafts.utils.thread.ProgressThread
import com.lightcrafts.utils.xml.XMLUtil
import java.awt.Point
import java.awt.color.ICC_Profile
import java.awt.image.*
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteOrder
import javax.media.jai.PlanarImage

/**
 * An `LCTIFFWriter` is a Java wrapper around the LibTIFF library.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see [LibTIFF](http://www.remotesensing.org/libtiff/)
 */
class LCTIFFWriter
/**
 * Construct an `LCTIFFWriter` and open a TIFF file.
 *
 * @param fileName The name of the TIFF file to open.
 * @param appendFileName The name of the TIFF file to append as the second page in a 2-page TIFF
 * file.
 * @param width The width of the image in pixels.
 * @param height The height of the image in pixels.
 * @param resolution The resolution (in pixels per unit).
 * @param resolutionUnit The resolution unit; must be either [ ][TIFFConstants.TIFF_RESOLUTION_UNIT_CM] or [TIFFConstants.TIFF_RESOLUTION_UNIT_INCH].
 */
@Throws(LCImageLibException::class, UnsupportedEncodingException::class)
@JvmOverloads constructor(
        /**
         * The name of the TIFF file.
         */
        private val m_fileName: String,

        /**
         * The name of the TIFF file to append, if any.
         */
        private val m_appendFileName: String?,

        /**
         * The width of the image as exported.
         */
        private val m_exportWidth: Int,

        /**
         * The height of the image as exported.
         */
        private val m_exportHeight: Int,

        /**
         * The resolution (in pixels per unit) of the image as exported.
         */
        private val m_resolution: Int = ResolutionOption.DEFAULT_VALUE,

        /**
         * The resolution unit of the image as exported.
         */
        private val m_resolutionUnit: Int = ResolutionUnitOption.DEFAULT_VALUE) : LCTIFFCommon() {

    /**
     * Flag used to remember whether the image has EXIF metadata.
     */
    private var m_hasExifMetadata: Boolean = false

    /**
     * Construct an `LCTIFFWriter` and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    constructor(fileName: String, width: Int, height: Int) : this(fileName, null, width, height) {
    }

    /**
     * Construct an `LCTIFFWriter` and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either [ ][TIFFConstants.TIFF_RESOLUTION_UNIT_CM] or [TIFFConstants.TIFF_RESOLUTION_UNIT_INCH].
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    constructor(fileName: String, width: Int, height: Int,
                resolution: Int, resolutionUnit: Int) : this(fileName, null, width, height, resolution, resolutionUnit) {
    }

    init {
        openForWriting(m_fileName)
        //
        // If openForWriting() fails, it will store 0 in the native pointer.
        //
        if (m_nativePtr == 0L) {
            throw LCImageLibException("Could not open $m_fileName")
        }
    }

    /**
     * Puts a TIFF image as tiles.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the putting.
     */
    @Throws(IOException::class, LCImageLibException::class)
    fun putImageTiled(image: RenderedImage, thread: ProgressThread) {
        try {
            writeImageTiled(image, thread)
            if (m_appendFileName != null) {
                append(m_appendFileName)
            }
            dispose()
            if (m_hasExifMetadata) {
                fixEXIFMetadata(m_fileName)
            }
        } finally {
            dispose()
        }
    }

    /**
     * Puts a TIFF image as strips.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the putting.
     */
    @Throws(IOException::class, LCImageLibException::class)
    fun putImageStriped(image: RenderedImage, thread: ProgressThread?) {
        try {
            writeImageStriped(image, thread)
            if (m_appendFileName != null) {
                append(m_appendFileName)
            }
            dispose()
            if (m_hasExifMetadata) {
                fixEXIFMetadata(m_fileName)
            }
        } finally {
            dispose()
        }
    }

    /**
     * Puts the given [ImageMetadata] into the TIFF file. This *must* be called only once
     * and prior to [.putImageStriped].
     *
     * @param imageMetadata The [ImageMetadata] to put.
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    fun putMetadata(imageMetadata: ImageMetadata) {
        val metadata = imageMetadata.prepForExport(
                TIFFImageType.INSTANCE, m_exportWidth, m_exportHeight,
                m_resolution, m_resolutionUnit, false
        )

        ////////// Put TIFF metadata //////////////////////////////////////////

        val tiffDir = metadata.getDirectoryFor(TIFFDirectory::class.java)
        if (tiffDir != null) {
            for ((tagID, value) in tiffDir) {
                when (tagID) {
                    TIFF_ARTIST, TIFF_COPYRIGHT, TIFF_DATE_TIME, TIFF_DOCUMENT_NAME,
                    TIFF_HOST_COMPUTER, TIFF_IMAGE_DESCRIPTION, TIFF_INK_NAMES, TIFF_MAKE,
                    TIFF_MODEL, TIFF_PAGE_NAME, TIFF_SOFTWARE, TIFF_TARGET_PRINTER
                    -> setStringField(tagID, value.stringValue)

                    TIFF_MS_RATING, TIFF_RESOLUTION_UNIT -> setIntField(tagID, value.intValue)
                    TIFF_X_RESOLUTION, TIFF_Y_RESOLUTION -> setFloatField(tagID, value.floatValue)
                }
            }
        }

        ////////// Put EXIF metadata //////////////////////////////////////////

        val exifDir = metadata.getDirectoryFor(EXIFDirectory::class.java)
        if (exifDir != null) {
            val exifBuf = EXIFEncoder.encode(metadata, false).array()
            //
            // Libtiff doesn't support writing EXIF metadata so we have to do
            // an annoying work-around.  We temporarily store the encoded EXIF
            // metadata as belonging to the PHOTOSHOP tag.  Later, after the
            // TIFF file has been completely written, we go back and patch the
            // file in-place by changing the tag ID to EXIF_IFD_POINTER and
            // adjusting the EXIF metadata offsets.
            //
            // The reason the PHOTOSHOP tag is used is because: (1) we don't
            // use it for anything else, (2) its field type is unsigned byte
            // so we can set its value to the encoded binary EXIF metadata, and
            // (3) its tag ID (0x8649) is fairly close to that of the real tag
            // ID of EXIF_IFD_POINTER (0x8769).  Point #3 is important because
            // the tag IDs in a TIFF file must be in ascending sorted order so
            // even after the tag ID is changed, the set of tags is still in
            // ascending sorted order.
            //
            setByteField(TIFF_PHOTOSHOP_IMAGE_RESOURCES, exifBuf)
            m_hasExifMetadata = true
        }

        ////////// Put IPTC metadata //////////////////////////////////////////

        val iptcDir = metadata.getDirectoryFor(IPTCDirectory::class.java)
        if (iptcDir != null) {
            //
            // Write both the binary and XMP forms of IPTC metadata: the binary
            // form to enable non-XMP-aware applications to read it and the
            // XMP form to write all the metadata, i.e., the additional IPTC
            // tags present in XMP.
            //
            val iptcBuf = (iptcDir as IPTCDirectory).encode(false)
            if (iptcBuf != null) {
                setByteField(TIFF_RICH_TIFF_IPTC, iptcBuf)
            }

            val xmpDoc = metadata.toXMP(false, true, IPTCDirectory::class.java)
            val xmpBuf = XMLUtil.encodeDocument(xmpDoc, false)
            setByteField(TIFF_XMP_PACKET, xmpBuf)
        }
    }

    /**
     * Sets the value of the given TIFF byte field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is a byte array ([TIFFConstants.TIFF_FIELD_TYPE_UBYTE].
     * @param value The value for the given tag.
     * @return Returns `true` only if the value was set.
     * @throws IllegalArgumentException if `tagID` isn't that of an byte metadata field
     * or is otherwise unsupported.
     * @see .setFloatField
     * @see .setIntField
     * @see .setStringField
     */
    @Throws(LCImageLibException::class)
    external fun setByteField(tagID: Int, value: ByteArray): Boolean

    /**
     * Sets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is an integer ([TIFFConstants.TIFF_FIELD_TYPE_USHORT] or [ ][TIFFConstants.TIFF_FIELD_TYPE_ULONG] and not a string.
     * @param value The value for the given tag.
     * @return Returns `true` only if the value was set.
     * @throws IllegalArgumentException if `tagID` isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see .setByteField
     * @see .setIntField
     * @see .setStringField
     */
    @Throws(LCImageLibException::class)
    private external fun setFloatField(tagID: Int, value: Float): Boolean

    /**
     * Sets the ICC profile of the TIFF image.  This *must* be called only once and prior to
     * [.putImageStriped] or [ ][.putImageTiled].
     *
     * @param iccProfile The [ICC_Profile] to set.
     */
    @Throws(LCImageLibException::class)
    fun setICCProfile(iccProfile: ICC_Profile) = setByteField(TIFF_ICC_PROFILE, iccProfile.data)

    /**
     * Sets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is an integer ([TIFFConstants.TIFF_FIELD_TYPE_USHORT] or [ ][TIFFConstants.TIFF_FIELD_TYPE_ULONG] and not a string.
     * @param value The value for the given tag.
     * @return Returns `true` only if the value was set.
     * @throws IllegalArgumentException if `tagID` isn't that of an integer metadata
     * field or is otherwise unsupported.
     * @see .setByteField
     * @see .setFloatField
     * @see .setStringField
     */
    @Throws(LCImageLibException::class)
    external fun setIntField(tagID: Int, value: Int): Boolean

    /**
     * Sets the value of the given TIFF string metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be that of a tag whose
     * value is a string ([TIFFConstants.TIFF_FIELD_TYPE_ASCII].
     * @param value The value for the given tag.
     * @return Returns `true` only if the value was set.
     * @throws IllegalArgumentException if `tagID` isn't that of an string metadata field
     * or is otherwise unsupported.
     * @see .setByteField
     * @see .setFloatField
     * @see .setIntField
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    private fun setStringField(tagID: Int, value: String): Boolean {
        val valueUtf8 = (value + '\u0000').toByteArray(charset("UTF-8"))
        return setStringField(tagID, valueUtf8)
    }

    @Throws(LCImageLibException::class)
    private external fun setStringField(tagID: Int, valueUtf8: ByteArray): Boolean

    /**
     * Append the TIFF image in the given file creating a multi-page TIFF file.
     *
     * @param fileName The name of the TIFF file to append.
     * @return Returns `true` only if the append succeeded.
     */
    @Throws(UnsupportedEncodingException::class)
    private fun append(fileName: String): Boolean {
        val fileNameUtf8 = (fileName + '\u0000').toByteArray(charset("UTF-8"))
        return append(fileNameUtf8)
    }

    private external fun append(fileNameUtf8: ByteArray): Boolean

    /**
     * Computes which tile a given point is in.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param sample TODO
     * @return Returns the tile index.
     */
    private external fun computeTile(x: Int, y: Int, z: Int, sample: Int): Int

    /**
     * Opens a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     */
    @Throws(LCImageLibException::class, UnsupportedEncodingException::class)
    private fun openForWriting(fileName: String) {
        val fileNameUtf8 = (fileName + '\u0000').toByteArray(charset("UTF-8"))
        openForWriting(fileNameUtf8)
    }

    @Throws(LCImageLibException::class)
    private external fun openForWriting(fileNameUtf8: ByteArray)

    /**
     * Writes a TIFF image as strips.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the writing.
     */
    @Throws(LCImageLibException::class)
    private fun writeImageStriped(image: RenderedImage,
                                  thread: ProgressThread?) {
        val dataType = image.sampleModel.dataType
        val bands = image.sampleModel.numBands
        val imageWidth = image.width
        val imageHeight = image.height

        setIntField(TIFF_BITS_PER_SAMPLE, if (dataType == DataBuffer.TYPE_BYTE) 8 else 16)
        setIntField(TIFF_IMAGE_WIDTH, imageWidth)
        setIntField(TIFF_IMAGE_LENGTH, imageHeight)
        setIntField(
                TIFF_PHOTOMETRIC_INTERPRETATION,
                if (bands == 4)
                    TIFF_PHOTOMETRIC_SEPARATED
                else if (bands == 3)
                    TIFF_PHOTOMETRIC_RGB
                else
                    TIFF_PHOTOMETRIC_BLACK_IS_ZERO)
        setIntField(TIFF_PLANAR_CONFIGURATION, TIFF_PLANAR_CONFIGURATION_CHUNKY)
        val stripHeight = 32
        setIntField(TIFF_ROWS_PER_STRIP, stripHeight)
        setIntField(TIFF_SAMPLES_PER_PIXEL, bands)

        val indicator = thread?.progressIndicator
        indicator?.setMaximum(imageHeight)

        // Allocate the output buffer only once
        val type = if (dataType == DataBuffer.TYPE_BYTE)
            DataBuffer.TYPE_BYTE
        else
            DataBuffer.TYPE_USHORT

        val outBuffer = Raster.createInterleavedRaster(
                type, imageWidth, stripHeight, bands * imageWidth, bands,
                LCImageLibUtil.bandOffset(bands), Point(0, 0))

        var y = 0
        var stripIndex = 0
        while (y < imageHeight) {
            if (thread?.isCanceled == true) {
                return
            }

            val currentStripHeight = Math.min(stripHeight, imageHeight - y)

            // Create a child raster of the out buffer for the current strip
            val raster = outBuffer.createWritableChild(0, 0, imageWidth, currentStripHeight, 0, y, null)

            // Prefetch tiles, uses all CPUs
            if (image is PlanarImage) {
                image.getTiles(image.getTileIndices(raster.bounds))
            }
            image.copyData(raster)

            val csm = raster.sampleModel as ComponentSampleModel
            val offset = LCImageLibUtil.min(*csm.bandOffsets)

            writeStrip(dataType, bands, imageWidth, stripIndex, currentStripHeight, raster, offset)
            indicator?.incrementBy(currentStripHeight)
            y += stripHeight
            stripIndex++
        }
        indicator?.setIndeterminate(true)
    }

    /**
     * Writes a TIFF image as tiles.
     *
     * @param image The image to put.
     * @param thread The thread that's doing the writing.
     */
    @Throws(LCImageLibException::class)
    private fun writeImageTiled(image: RenderedImage,
                                thread: ProgressThread?) {
        val dataType = image.sampleModel.dataType

        setIntField(TIFF_IMAGE_WIDTH, image.width)
        setIntField(TIFF_IMAGE_LENGTH, image.height)
        setIntField(TIFF_BITS_PER_SAMPLE, if (dataType == DataBuffer.TYPE_BYTE) 8 else 16)
        setIntField(TIFF_SAMPLES_PER_PIXEL, image.sampleModel.numBands)

        setIntField(TIFF_PLANAR_CONFIGURATION, TIFF_PLANAR_CONFIGURATION_CHUNKY)
        setIntField(TIFF_PHOTOMETRIC_INTERPRETATION, TIFF_PHOTOMETRIC_RGB)

        setIntField(TIFF_TILE_WIDTH, image.tileWidth)
        setIntField(TIFF_TILE_LENGTH, image.tileHeight)

        val indicator = thread?.progressIndicator
        indicator?.setMaximum(image.numXTiles * image.numYTiles)

        for (tileX in 0 until image.numXTiles) {
            for (tileY in 0 until image.numYTiles) {
                if (thread?.isCanceled == true) {
                    return
                }

                val tileIndex = computeTile(tileX * image.tileWidth, tileY * image.tileHeight, 0, 0)
                val tile = image.getTile(tileX, tileY)

                writeTile(dataType, tileIndex, tile)
                indicator?.incrementBy(1)
            }
        }
        indicator?.setIndeterminate(true)
    }

    @Throws(LCImageLibException::class)
    private fun writeStrip(dataType: Int, bands: Int, imageWidth: Int, stripIndex: Int,
                           currentStripHeight: Int, raster: WritableRaster, offset: Int) {
        val db = raster.dataBuffer
        val stripSize: Int
        val written: Int

        if (dataType == DataBuffer.TYPE_BYTE) {
            stripSize = bands * imageWidth * currentStripHeight
            written = writeStripByte(stripIndex, (db as DataBufferByte).data, offset.toLong(),
                    stripSize)
        } else {
            stripSize = 2 * bands * imageWidth * currentStripHeight
            written = writeStripShort(stripIndex, (db as DataBufferUShort).data, offset.toLong(),
                    stripSize)
        }

        if (written != stripSize) {
            throw LCImageLibException("something is wrong: $written != $stripSize")
        }
    }

    @Throws(LCImageLibException::class)
    private fun writeTile(dataType: Int, tileIndex: Int, tile: Raster) {
        val db = tile.dataBuffer
        val tileSize: Int
        val written: Int

        if (dataType == DataBuffer.TYPE_BYTE) {
            val buffer = (db as DataBufferByte).data
            tileSize = buffer.size
            written = writeTileByte(tileIndex, buffer, 0, tileSize)
        } else {
            val buffer = (db as DataBufferUShort).data
            tileSize = 2 * buffer.size
            written = writeTileShort(tileIndex, buffer, 0, tileSize)
        }

        if (written != tileSize) {
            throw LCImageLibException("something is wrong: $written != $tileSize")
        }
    }

    /**
     * Encodes and writes a strip to the TIFF image.
     *
     * @param stripIndex The index of the strip to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    @Throws(LCImageLibException::class)
    private external fun writeStripByte(stripIndex: Int, buf: ByteArray, offset: Long,
                                        stripSize: Int): Int

    /**
     * Encodes and writes a strip to the TIFF image.
     *
     * @param stripIndex The index of the strip to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    @Throws(LCImageLibException::class)
    private external fun writeStripShort(stripIndex: Int, buf: ShortArray, offset: Long,
                                         stripSize: Int): Int

    /**
     * Encodes and writes a tile to the TIFF image.
     *
     * @param tileIndex The index of the tile to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    @Throws(LCImageLibException::class)
    private external fun writeTileByte(tileIndex: Int, buf: ByteArray, offset: Long,
                                       tileSize: Int): Int

    /**
     * Encodes and writes a tile to the TIFF image.
     *
     * @param tileIndex The index of the tile to write.
     * @param buf The buffer into which to write the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes written or -1 if there was an error.
     */
    @Throws(LCImageLibException::class)
    private external fun writeTileShort(tileIndex: Int, buf: ShortArray, offset: Long,
                                        tileSize: Int): Int

    companion object {

        /**
         * Fix the EXIF metadata in a TIFF file.
         *
         * @param fileName The full path of the TIFF file.
         */
        @Throws(IOException::class)
        private fun fixEXIFMetadata(fileName: String) {
            //
            // This code is based on the code in TIFFMetadataReader but it's been
            // simplified and does much less error-checking because we just wrote
            // the TIFF file ourselves so we know it's valid.
            //
            OrderableRandomAccessFile(fileName, "rw").use { file ->
                if (file.readShort() == TIFF_LITTLE_ENDIAN) {
                    file.order(ByteOrder.LITTLE_ENDIAN)
                }
                var ifdOffset = file.run {
                    seek((TIFF_HEADER_SIZE - TIFF_INT_SIZE).toLong())
                    readInt()
                }
                while (ifdOffset > 0) {
                    val entryCount = file.run {
                        seek(ifdOffset.toLong())
                        readUnsignedShort()
                    }
                    for (entry in 0 until entryCount) {
                        val entryOffset = TIFFMetadataReader.calcIFDEntryOffset(ifdOffset, entry)
                        val tagID = file.run {
                            seek(entryOffset.toLong())
                            readUnsignedShort()
                        }
                        if (tagID == TIFF_PHOTOSHOP_IMAGE_RESOURCES) {
                            val subdirOffset = file.run {
                                seek(file.filePointer - TIFF_SHORT_SIZE)
                                writeShort(TIFF_EXIF_IFD_POINTER)
                                writeShort(TIFF_FIELD_TYPE_ULONG.toInt())
                                writeInt(1)
                                readInt()
                            }
                            fixEXIFDirectory(file, subdirOffset.toLong(), 0)
                            return
                        }
                    }
                    ifdOffset = file.readInt()
                }
            }
        }

        /**
         * Fix an EXIF directory in a TIFF file.  Specifically, this means to adjust all value offsets
         * so that they are relative to the beginning of the TIFF file (as is required by the TIFF
         * specification) rather than relative to the start of the EXIF directory (as is the case when
         * in a JPEG file).
         *
         * @param file The TIFF file containing an EXIF directory to fix.
         * @param dirOffset The offset to the start of the EXIF directory.
         * @param parentDirSize The size of the parent EXIF directory, if any.
         */
        @Throws(IOException::class)
        private fun fixEXIFDirectory(file: OrderableRandomAccessFile,
                                     dirOffset: Long, parentDirSize: Int) {
            file.seek(dirOffset)
            val entryCount = file.readUnsignedShort()
            for (entry in 0 until entryCount) {
                val entryOffset = TIFFMetadataReader.calcIFDEntryOffset(dirOffset.toInt(), entry)
                val tagID = file.run {
                    seek(entryOffset.toLong())
                    readUnsignedShort()
                }
                val fieldType = file.readUnsignedShort()
                val numValues = file.readInt()
                val byteCount = numValues * EXIF_FIELD_SIZE[fieldType]
                if (byteCount > TIFF_INLINE_VALUE_MAX_SIZE ||
                        tagID == EXIF_IFD_POINTER || tagID == EXIF_GPS_IFD_POINTER) {
                    val origValueOffset = file.readInt()
                    val valueOffset = origValueOffset + dirOffset.toInt() + parentDirSize
                    file.run {
                        seek(file.filePointer - TIFF_INT_SIZE)
                        writeInt(valueOffset)
                    }
                    when (tagID) {
                        EXIF_IFD_POINTER, EXIF_GPS_IFD_POINTER
                        -> fixEXIFDirectory(file, valueOffset.toLong(), -origValueOffset)
                    }
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val tiff = LCTIFFReader(args[0])
                val image = tiff.getImage(null) ?: return

                val writer = LCTIFFWriter(
                        System.getProperty("java.io.tmpdir") + File.separator + "out.tiff",
                        args[1], image.width, image.height)
                writer.setStringField(TIFF_SOFTWARE, Version.getApplicationName())
                writer.putImageStriped(image, null)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: LCImageLibException) {
                e.printStackTrace()
            } catch (e: UserCanceledException) {
                e.printStackTrace()
            }

        }
    }
}
/* vim:set et sw=4 ts=4: */
