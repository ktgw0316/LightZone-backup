/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.jai.opimage

import com.lightcrafts.extensions.primitive.clampUnsignedShort
import com.lightcrafts.extensions.primitive.toUnsignedInt
import com.lightcrafts.jai.JAIContext
import com.lightcrafts.utils.LCMatrix
import java.awt.Rectangle
import java.awt.color.ColorSpace
import java.awt.color.ICC_Profile
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
class IntVibranceOpImage(source: RenderedImage, transform: Array<FloatArray>, config: Map<*, *>?) :
        PointOpImage(source, ImageLayout(source), config, true)
{
    private val transform = Array(3) { IntArray(3) }
    private val toLinearsRGB = Array(3) { IntArray(3) }
    private val saturationIncrease: Boolean

    init {
        permitInPlaceOperation()

        for (i in 0..2) {
            for (j in 0..2) {
                this.transform[i][j] = (sMath_scale * transform[i][j]).toInt()
            }
        }

        saturationIncrease = transform[0][0] > 1

        val linRGB = ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB) as ICC_ProfileRGB
        val XYZtoLinsRGB = LCMatrix(linRGB.matrix).inverse()
        val CIERGBtoXYZ = LCMatrix((JAIContext.linearProfile as ICC_ProfileRGB).matrix)
        val CIERGBtoLinsRGB = XYZtoLinsRGB.times(CIERGBtoXYZ).array

        for (i in 0..2) {
            for (j in 0..2) {
                toLinearsRGB[i][j] = (sMath_scale * CIERGBtoLinsRGB[i][j]).toInt()
            }
        }

    }

    override fun computeRect(sources: Array<Raster>?,
                             dest: WritableRaster?,
                             destRect: Rectangle?) {
        // Retrieve format tags.
        val formatTags = formatTags

        val src = RasterAccessor(sources!![0], destRect!!, formatTags[0], getSourceImage(0).colorModel)
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

        val sqrt3d2 = (sMath_scale * Math.sqrt(3.0) / 2).toInt() // 0.866...

        for (row in 0 until height) {
            for (col in 0 until width) {
                val srcPixOffset = srcPixelStride * col + row * srcLineStride
                val r = srcData[srcPixOffset + srcROffset].toUnsignedInt() / 2
                val g = srcData[srcPixOffset + srcGOffset].toUnsignedInt() / 2
                val b = srcData[srcPixOffset + srcBOffset].toUnsignedInt() / 2

                val lr = (toLinearsRGB[0][0] * r + toLinearsRGB[0][1] * g + toLinearsRGB[0][2] * b) / sMath_scale
                val lg = (toLinearsRGB[1][0] * r + toLinearsRGB[1][1] * g + toLinearsRGB[1][2] * b) / sMath_scale
                val lb = (toLinearsRGB[2][0] * r + toLinearsRGB[2][1] * g + toLinearsRGB[2][2] * b) / sMath_scale

                val x = lr - (lg + lb) / 2
                val y = sqrt3d2 * (lg - lb) / sMath_scale

                var hue = arctan2(x, y) + sMath_PI

                if (hue < 0) {
                    hue += 2 * sMath_PI
                }

                if (hue > 4 * sMath_PI / 3) {
                    hue -= 4 * sMath_PI / 3
                } else if (hue > 2 * sMath_PI / 3) {
                        hue -= 2 * sMath_PI / 3
                }

                var mask = sMath_scale / 2 + (sMath_scale - sMath_scale * Math.abs(sMath_PI / 6 - hue) / (sMath_PI / 3)) / 2

                if (saturationIncrease) {
                    val min = Math.min(r, Math.min(g, b))
                    val max = Math.max(r, Math.max(g, b))

                    val saturation = if (max != 0) sMath_scale - sMath_scale * min / max else 0
                    mask = mask * (sMath_scale - saturation * saturation / sMath_scale) / sMath_scale
                }

                var rr = (transform[0][0] * r + transform[0][1] * g + transform[0][2] * b) / sMath_scale
                var gg = (transform[1][0] * r + transform[1][1] * g + transform[1][2] * b) / sMath_scale
                var bb = (transform[2][0] * r + transform[2][1] * g + transform[2][2] * b) / sMath_scale

                rr = 2 * ((sMath_scale - mask) * r / sMath_scale + rr * mask / sMath_scale)
                gg = 2 * ((sMath_scale - mask) * g / sMath_scale + gg * mask / sMath_scale)
                bb = 2 * ((sMath_scale - mask) * b / sMath_scale + bb * mask / sMath_scale)

                val dstPixOffset = dstPixelStride * col + row * dstLineStride
                dstData[dstPixOffset + dstROffset] = rr.clampUnsignedShort()
                dstData[dstPixOffset + dstGOffset] = gg.clampUnsignedShort()
                dstData[dstPixOffset + dstBOffset] = bb.clampUnsignedShort()
            }
        }
    }

    companion object {

        private val sMath_scale = 0x8000
        private val sMath_PI = (sMath_scale * Math.PI).toInt()

        /**
         * fast integer arctan2 implementation.
         * see: http://www.dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
         */
        internal fun arctan2(y: Int, x: Int): Int {
            val coeff_1 = sMath_PI / 4
            val coeff_2 = 3 * coeff_1
            val abs_y = Math.abs(y) + 1      // kludge to prevent 0/0 condition
            val angle: Int

            if (x >= 0) {
                val r = sMath_scale * (x - abs_y) / (x + abs_y)
                angle = coeff_1 - coeff_1 * r / sMath_scale
            } else {
                val r = sMath_scale * (x + abs_y) / (abs_y - x)
                angle = coeff_2 - coeff_1 * r / sMath_scale
            }

            return if (y < 0) -angle else angle
        }
    }
}
