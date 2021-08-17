/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.jai.opimage

import com.lightcrafts.extensions.matrix.Matrix
import com.lightcrafts.extensions.matrix.getArrayFloat
import com.lightcrafts.extensions.primitive.clampUnsignedShort
import com.lightcrafts.extensions.primitive.toUnsignedInt
import com.lightcrafts.image.color.HSB
import com.lightcrafts.jai.JAIContext
import java.awt.Rectangle
import java.awt.color.ICC_ProfileRGB
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.awt.image.RenderedImage
import java.awt.image.WritableRaster
import javax.media.jai.ImageLayout
import javax.media.jai.PointOpImage
import javax.media.jai.RasterAccessor

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
class HueRotateOpImage(source: RenderedImage, private val angle: Float, config: Map<*, *>?) :
        PointOpImage(source, ImageLayout(source), config, true)
{
    private val toSRGB: Array<FloatArray>
    private val toLinearRGB: Array<FloatArray>

    init {
        permitInPlaceOperation()

        val sRGB = JAIContext.sRGBColorProfile as ICC_ProfileRGB
        toSRGB = Matrix(sRGB.matrix)
                .inverse()
                .times(Matrix((JAIContext.linearProfile as ICC_ProfileRGB).matrix))
                .getArrayFloat()
        toLinearRGB = Matrix(sRGB.matrix)
                .inverse()
                .times(Matrix(JAIContext.linearProfile.matrix))
                .inverse()
                .getArrayFloat()
    }

    override fun computeRect(sources: Array<Raster>?,
                             dest: WritableRaster?,
                             destRect: Rectangle?) {
        // Retrieve format tags.
        val formatTags = formatTags

        val src = RasterAccessor(sources!![0], destRect!!, formatTags[0],
                getSourceImage(0).colorModel)
        val dst = RasterAccessor(dest!!, destRect, formatTags[1], getColorModel())

        when (dst.dataType) {
            DataBuffer.TYPE_USHORT -> ushortLoop(src, dst)
            else -> throw UnsupportedOperationException("Unsupported data type: " + dst.dataType)
        }
    }

    protected fun ushortLoop(src: RasterAccessor, dst: RasterAccessor) {
        val width = src.width
        val height = src.height

        val dstData = dst.getShortDataArray(0)
        val dstBandOffsets = dst.bandOffsets
        val dstLineStride = dst.scanlineStride
        val dstPixelStride = dst.pixelStride

        val srcData = src.getShortDataArray(0)
        val srcBandOffsets = src.bandOffsets
        val srcLineStride = src.scanlineStride
        val srcPixelStride = src.pixelStride

        val srcROffset = srcBandOffsets[0]
        val srcGOffset = srcBandOffsets[1]
        val srcBOffset = srcBandOffsets[2]

        val dstROffset = dstBandOffsets[0]
        val dstGOffset = dstBandOffsets[1]
        val dstBOffset = dstBandOffsets[2]

        val rgb = FloatArray(3)
        val hsi = FloatArray(3)

        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcPixOffset = srcPixelStride * col + row * srcLineStride
                val r = srcData[srcPixOffset + srcROffset].toUnsignedInt()
                val g = srcData[srcPixOffset + srcGOffset].toUnsignedInt()
                val b = srcData[srcPixOffset + srcBOffset].toUnsignedInt()

                rgb[0] = (toSRGB[0][0] * r + toSRGB[0][1] * g + toSRGB[0][2] * b) / 0xffff
                rgb[1] = (toSRGB[1][0] * r + toSRGB[1][1] * g + toSRGB[1][2] * b) / 0xffff
                rgb[2] = (toSRGB[2][0] * r + toSRGB[2][1] * g + toSRGB[2][2] * b) / 0xffff

                HSB.fromRGB(rgb, hsi)

                hsi[0] += angle

                if (hsi[0] < 0)
                    hsi[0] += 1f
                else if (hsi[0] >= 1)
                    hsi[0] -= 1f

                HSB.toRGB(hsi, rgb)

                val rr = (0xffff * (toLinearRGB[0][0] * rgb[0] + toLinearRGB[0][1] * rgb[1] + toLinearRGB[0][2] * rgb[2])).toInt()
                val gg = (0xffff * (toLinearRGB[1][0] * rgb[0] + toLinearRGB[1][1] * rgb[1] + toLinearRGB[1][2] * rgb[2])).toInt()
                val bb = (0xffff * (toLinearRGB[2][0] * rgb[0] + toLinearRGB[2][1] * rgb[1] + toLinearRGB[2][2] * rgb[2])).toInt()

                val dstPixOffset = dstPixelStride * col + row * dstLineStride
                dstData[dstPixOffset + dstROffset] = rr.clampUnsignedShort()
                dstData[dstPixOffset + dstGOffset] = gg.clampUnsignedShort()
                dstData[dstPixOffset + dstBOffset] = bb.clampUnsignedShort()
            }
        }
    }
}
