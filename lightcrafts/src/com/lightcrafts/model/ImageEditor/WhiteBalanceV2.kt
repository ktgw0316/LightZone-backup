/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor

import Jama.Matrix
import com.lightcrafts.extensions.matrix.Matrix
import com.lightcrafts.extensions.matrix.getArrayFloat
import com.lightcrafts.extensions.primitive.clamp

import com.lightcrafts.image.types.RawImageInfo
import com.lightcrafts.jai.utils.Transform
import com.lightcrafts.model.OperationType
import com.lightcrafts.model.SliderConfig
import com.lightcrafts.model.ColorDropperOperation
import com.lightcrafts.utils.ColorScience
import com.lightcrafts.utils.splines

import javax.media.jai.JAI
import javax.media.jai.LookupTableJAI
import javax.media.jai.PlanarImage

import java.awt.geom.Point2D
import java.awt.image.renderable.ParameterBlock
import java.awt.image.RenderedImage
import java.text.DecimalFormat
import java.util.TreeMap

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: May 31, 2005
 * Time: 7:08:49 PM
 * To change this template use File | Settings | File Templates.
 */
internal class WhiteBalanceV2(rendering: Rendering, type: OperationType) : BlendedOperation(rendering, type), ColorDropperOperation {
    private val TINT = "Tint"
    private var tint = 0f
    private var p: Point2D? = null

    private var source = 5000f
    private var REF_T = 5000f
    private var caMethod = ColorScience.CAMethod.Bradford

    init {
        colorInputOnly = true

        caMethod = ColorScience.CAMethod.Mixed

        addSliderKey(SOURCE)
        addSliderKey(TINT)

        setSliderConfig(SOURCE, SliderConfig(1000.0, 40000.0, source.toDouble(), 10.0, true, DecimalFormat("0")))
        setSliderConfig(TINT, SliderConfig(-20.0, 20.0, tint.toDouble(), 0.1, false, DecimalFormat("0.0")))
    }

    override fun neutralDefault(): Boolean = false

    override fun setSliderValue(key: String, _value: Double) {
        val value = roundValue(key, _value)

        if (key == SOURCE && source.toDouble() != value) {
            source = value.toFloat()
        } else if (key == TINT && tint.toDouble() != value) {
            tint = value.toFloat()
        } else {
            return
        }

        super.setSliderValue(key, value)
    }

    override fun setColor(p: Point2D): Map<String, Float> {
        this.p = p
        settingsChanged()
        this.p = null

        val result = TreeMap<String, Float>()
        result.put(SOURCE, source)
        result.put(TINT, tint)
        return result
    }

    private inner class WhiteBalanceTransform internal constructor(source: PlanarImage) :
            BlendedOperation.BlendedTransform(source)
    {
        override fun setFront(): PlanarImage {
            var lightness = 0.18f

            if (p != null) {
                val pixel = pointToPixel(p)

                if (pixel != null) {
                    val n = neutralize(pixel, caMethod, source, REF_T)
                    lightness = pixel[1] / 255.0f
                    source = n[0]
                    tint = Math.min(Math.max(n[1], -20f), 20f)
                }
            }

            return whiteBalance(back, source, REF_T, tint, lightness, caMethod)
        }
    }

    override fun updateOp(op: Transform) {
        op.update()
    }

    override fun createBlendedOp(source: PlanarImage): BlendedOperation.BlendedTransform =
            WhiteBalanceTransform(source)

    companion object {
        private val SOURCE = "Temperature"

        internal val typeV2: OperationType = OperationTypeImpl("White Point V2")
        internal val typeV3: OperationType = OperationTypeImpl("White Point V3")

        private fun W(original: Float, target: Float): FloatArray {
            val originalW = ColorScience.W(original)
            val targetW = ColorScience.W(target)
            return floatArrayOf(originalW[0] / targetW[0], originalW[1] / targetW[1], originalW[2] / targetW[2])
        }

        private val RGBtoZYX = Matrix(ColorScience.RGBtoZYX()).transpose()
        private val XYZtoRGB = RGBtoZYX.inverse()

        internal fun neutralize(pixel: IntArray, caMethod: ColorScience.CAMethod, source: Float, REF_T: Float):
                FloatArray {
            var r = pixel[0].toDouble()
            var g = pixel[1].toDouble()
            var b = pixel[2].toDouble()
            var sat = ColorScience.saturation(r, g, b)
            var minT = source.toInt()
            var wbr = 0.0
            var wbg = 0.0
            var wbb = 0.0

            var t = 1000
            while (t < 40000) {
                val B = Matrix(ColorScience.chromaticAdaptation(REF_T, t.toFloat(), caMethod))
                val combo = XYZtoRGB.times(B.times(RGBtoZYX))

                var color = Matrix(arrayOf(
                        doubleArrayOf(pixel[0].toDouble()),
                        doubleArrayOf(pixel[1].toDouble()),
                        doubleArrayOf(pixel[2].toDouble())))

                color = combo.times(color)

                r = color.get(0, 0)
                g = color.get(1, 0)
                b = color.get(2, 0)

                val tSat = ColorScience.saturation(r, g, b)

                if (tSat < sat) {
                    sat = tSat
                    minT = t
                    wbr = r / 256
                    wbg = g / 256
                    wbb = b / 256
                }
                t += (0.001 * t).toInt()
            }

            if (wbr != 0.0 || wbg != 0.0 || wbb != 0.0) {
                println("wb: $wbr, $wbg, $wbb, sat: $sat")
                return floatArrayOf(minT.toFloat(), (-(wbg - (wbr + wbb) / 2)).toFloat())
            } else {
                return floatArrayOf(REF_T, 0f)
            }
        }

        fun whiteBalance(image: RenderedImage, source: Float,
                         REF_T: Float, tint: Float, lightness: Float,
                         caMethod: ColorScience.CAMethod): PlanarImage =
                whiteBalance(image, source, REF_T, tint, lightness, 1f, null, caMethod)

        fun whiteBalanceMatrix(source: Float, REF_T: Float, mult: Float, cameraRGB: Array<FloatArray>?,
                               caMethod: ColorScience.CAMethod): Array<FloatArray> {
            val B = Matrix(ColorScience.chromaticAdaptation(REF_T, source, caMethod))
            var combo = XYZtoRGB.times(B.times(RGBtoZYX))

            val m = combo.times(Matrix(arrayOf(doubleArrayOf(1.0), doubleArrayOf(1.0), doubleArrayOf(1.0))))

            val max = m.get(1, 0) // Math.max(m.get(1, 0), Math.max(m.get(1, 0), m.get(2, 0)));
            if (max != 1.0) {
                combo = combo.times(Matrix(arrayOf(
                        doubleArrayOf(1 / max, 0.0, 0.0),
                        doubleArrayOf(0.0, 1 / max, 0.0),
                        doubleArrayOf(0.0, 0.0, 1 / max))))
            }

            if (cameraRGB != null) {
                combo = combo.times(Matrix(cameraRGB))
            }

            if (mult != 1f) {
                combo = combo.times(mult.toDouble())
            }

            return combo.getArrayFloat()
        }

        fun tintCast(image: PlanarImage, tint: Float, lightness: Float): PlanarImage {
            if (tint != 0f) {
                val tred = (-tint / 4).toDouble()
                val tgreen = (tint / 2).toDouble()
                val tblue = (-tint / 4).toDouble()

                val polygon = arrayOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(lightness.toDouble(), 0.0), doubleArrayOf(1.0, 0.0))

                polygon[1][1] = tred
                val redCurve = Array(256) { DoubleArray(2) }
                splines.bspline(2, polygon, redCurve)

                polygon[1][1] = tgreen
                val greenCurve = Array(256) { DoubleArray(2) }
                splines.bspline(2, polygon, greenCurve)

                polygon[1][1] = tblue
                val blueCurve = Array(256) { DoubleArray(2) }
                splines.bspline(2, polygon, blueCurve)

                val table = Array(3) { ShortArray(0x10000) }

                val interpolator = splines.Interpolator()

                for (i in 0..65535) {
                    table[0][i] = ((i + 0xff * interpolator.interpolate(i / 0xffff.toDouble(), redCurve))
                            .clamp(0.0, 0xffff.toDouble())
                            .toInt() and 0xffff).toShort()
                }
                interpolator.reset()
                for (i in 0..65535) {
                    table[1][i] = ((i + 0xff * interpolator.interpolate(i / 0xffff.toDouble(), greenCurve))
                            .clamp(0.0, 0xffff.toDouble())
                            .toInt() and 0xffff).toShort()
                }
                interpolator.reset()
                for (i in 0..65535) {
                    table[2][i] = ((i + 0xff * interpolator.interpolate(i / 0xffff.toDouble(), blueCurve))
                            .clamp(0.0, 0xffff.toDouble())
                            .toInt() and 0xffff).toShort()
                }

                val lookupTable = LookupTableJAI(table, true)

                val pb = ParameterBlock()
                pb.addSource(image)
                pb.add(lookupTable)
                return JAI.create("lookup", pb, null)
            } else {
                return image
            }
        }

        fun whiteBalance(image: RenderedImage, source: Float, REF_T: Float,
                         tint: Float, lightness: Float, mult: Float, cameraRGB: Array<FloatArray>?,
                         caMethod: ColorScience.CAMethod): PlanarImage {
            val b = whiteBalanceMatrix(source, REF_T, mult, cameraRGB, caMethod)
            val t = Array(3) { DoubleArray(4) } // for BC, last column si going to be zero

            for (i in 0..2) {
                for (j in 0..2) {
                    t[i][j] = b[i][j].toDouble()
                }
            }

            val cargb = JAI.create("BandCombine", image, t, null)

            return if (tint != 0f)
                tintCast(cargb, tint, lightness)
            else
                cargb
        }
    }
}
