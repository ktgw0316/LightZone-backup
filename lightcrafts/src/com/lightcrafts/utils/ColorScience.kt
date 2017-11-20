/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.utils

import Jama.Matrix
import com.lightcrafts.extensions.matrix.Matrix
import com.lightcrafts.extensions.matrix.getArrayFloat
import com.lightcrafts.extensions.primitive.clamp
import com.lightcrafts.image.color.BlackBody
import com.lightcrafts.jai.JAIContext
import java.awt.color.ICC_ProfileRGB
import java.awt.image.DataBuffer

object ColorScience {
    private val rgbXYZ: Array<FloatArray>
    private val wtptXYZ: FloatArray
    private val whitePointTemperature: Float

    val Wr: Float
    val Wg: Float
    val Wb: Float
    val W: FloatArray

    private val Cxy: Array<FloatArray>

    private val XYZToRGBMat: Array<FloatArray>
    val RGBToXYZMat: Array<FloatArray>

    // Bradford cone response matrix, seems to deliver more consistent results
    private var Bradford = arrayOf(
            floatArrayOf(0.8951f, -0.7502f, 0.0389f),
            floatArrayOf(0.2664f, 1.7135f, -0.0685f),
            floatArrayOf(-0.1614f, 0.0367f, 1.0296f))

    private var VonKries = arrayOf(
            floatArrayOf(0.40024f, -0.22630f, 0.00000f),
            floatArrayOf(0.70760f, 1.16532f, 0.00000f),
            floatArrayOf(-0.08081f, 0.04570f, 0.91822f))

    // CAT02 matrix, sometimes gives more "neutral" results (no cyan cast)
    private var CAT02 = arrayOf(
            floatArrayOf(0.7328f, -0.7036f, 0.0030f),
            floatArrayOf(0.4296f, 1.6975f, 0.0136f),
            floatArrayOf(-0.1624f, 0.0061f, 0.9834f))

    private var Sharp = arrayOf(
            floatArrayOf(1.2694f, -0.8364f, 0.0297f),
            floatArrayOf(-0.0988f, 1.8006f, -0.0315f),
            floatArrayOf(-0.1706f, 0.0357f, 1.0018f))

    private var CMCCAT2000 = arrayOf(
            floatArrayOf(0.7982f, -0.5918f, 0.0008f),
            floatArrayOf(0.3389f, 1.5512f, 0.0239f),
            floatArrayOf(-0.1371f, 0.0406f, 0.9753f))

    private var XYZScaling = arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f))

    private val RGBtoZYX = Matrix(RGBtoZYX()).transpose()
    private val XYZtoRGB = RGBtoZYX.inverse()

    fun XYZ2xy(XYZ: FloatArray): FloatArray =
            floatArrayOf(XYZ[0] / (XYZ[0] + XYZ[1] + XYZ[2]), XYZ[1] / (XYZ[0] + XYZ[1] + XYZ[2]))

    private fun xy2XYZ(xy: FloatArray): FloatArray = floatArrayOf(xy[0] / xy[1], 1f, (1f - xy[0] - xy[1]) / xy[1])

    fun XYZ2xyY(XYZ: FloatArray): FloatArray =
            floatArrayOf(XYZ[0] / (XYZ[0] + XYZ[1] + XYZ[2]), XYZ[1] / (XYZ[0] + XYZ[1] + XYZ[2]), XYZ[1])

    fun xyY2XYZ(xyY: FloatArray): FloatArray =
            floatArrayOf(xyY[2] * xyY[0] / xyY[1], xyY[2], xyY[2] * (1f - xyY[0] - xyY[1]) / xyY[1])

    init {
        val pp = ICC_ProfileParameters(JAIContext.linearProfile as ICC_ProfileRGB)

        rgbXYZ = pp.rgbXYZ
        wtptXYZ = pp.wtptXYZ
        Cxy = pp.Cxy
        whitePointTemperature = pp.whitePointTemperature
        W = pp.W

        Wr = pp.W[0]
        Wg = pp.W[1]
        Wb = pp.W[2]

        RGBToXYZMat = pp.RGBToXYZMat
        XYZToRGBMat = pp.XYZToRGBMat
    }

    // TODO: we have to finish cleaning up this.

    class ICC_ProfileParameters(profile: ICC_ProfileRGB) {
        val rgbXYZ = Array(3) { FloatArray(3) }
        val wtptXYZ: FloatArray
        val Cxy: Array<FloatArray>
        val whitePointTemperature: Float
        val W: FloatArray
        val RGBToXYZMat: Array<FloatArray>
        val XYZToRGBMat: Array<FloatArray>

        init {
            // Extract the rgbXYZ from the current linear color profile
            val rgbXYZt = profile.matrix
            for (i in 0..2) {
                for (j in 0..2) {
                    rgbXYZ[i][j] = rgbXYZt[j][i]
                }
            }

            // Same for the white point
            wtptXYZ = profile.mediaWhitePoint

            // compute the xy coordinates of the workspace primaries form XYZ
            val Cr = rgbXYZ[0][0] + rgbXYZ[0][1] + rgbXYZ[0][2]
            val Cg = rgbXYZ[1][0] + rgbXYZ[1][1] + rgbXYZ[1][2]
            val Cb = rgbXYZ[2][0] + rgbXYZ[2][1] + rgbXYZ[2][2]

            Cxy = arrayOf(
                    floatArrayOf(rgbXYZ[0][0] / Cr, rgbXYZ[0][1] / Cr),
                    floatArrayOf(rgbXYZ[1][0] / Cg, rgbXYZ[1][1] / Cg),
                    floatArrayOf(rgbXYZ[2][0] / Cb, rgbXYZ[2][1] / Cb))

            val Zr = floatArrayOf(Cxy[0][0], Cxy[0][1], 1f - Cxy[0][0] - Cxy[0][1])
            val Zg = floatArrayOf(Cxy[1][0], Cxy[1][1], 1f - Cxy[1][0] - Cxy[1][1])
            val Zb = floatArrayOf(Cxy[2][0], Cxy[2][1], 1f - Cxy[2][0] - Cxy[2][1])

            // wtptXYZ -> wtptxy
            val wtptxy = XYZ2xy(wtptXYZ)
            whitePointTemperature = CCTX(wtptxy[0])
            W = W(whitePointTemperature)

            RGBToXYZMat = Matrix(arrayOf(mul(W[0] / Zr[1], Zr), mul(W[1] / Zg[1], Zg), mul(W[2] / Zb[1], Zb)))
                    .transpose().getArrayFloat()

            XYZToRGBMat = Matrix(RGBToXYZMat).inverse().getArrayFloat()
        }
    }

    // Return x,y coordinates of the CIE D illuminant specified by t, good between 1000K and 40000K
    internal fun D(t: Float): FloatArray = BlackBody.xy(t)

    fun CCTX(x: Float): Float = BlackBody.t(x)

    fun RGBtoZYX(): Array<FloatArray> {
        val mdata = Array(3) { DoubleArray(3) }
        for (i in 0..2) {
            for (j in 0..2) {
                mdata[i][j] = rgbXYZ[i][j].toDouble()
            }
        }
        val XYZRGB = Matrix(mdata)

        val S = Matrix(arrayOf(doubleArrayOf(wtptXYZ[0].toDouble(), wtptXYZ[1].toDouble(), wtptXYZ[2].toDouble())))
                .times(XYZRGB.inverse())
                .getArrayFloat()

        return arrayOf(
                floatArrayOf(S[0][0] * rgbXYZ[0][0], S[0][0] * rgbXYZ[0][1], S[0][0] * rgbXYZ[0][2]),
                floatArrayOf(S[0][1] * rgbXYZ[1][0], S[0][1] * rgbXYZ[1][1], S[0][1] * rgbXYZ[1][2]),
                floatArrayOf(S[0][2] * rgbXYZ[2][0], S[0][2] * rgbXYZ[2][1], S[0][2] * rgbXYZ[2][2]))
    }

    internal fun mul(c: Float, v: FloatArray): FloatArray {
        val r = FloatArray(v.size)
        for (i in v.indices) {
            r[i] = c * v[i]
        }
        return r
    }

    fun W(T: Float): FloatArray {
        val DT = D(T)

        // Compute z-coordinates
        val R = floatArrayOf(Cxy[0][0], Cxy[0][1], 1f - Cxy[0][0] - Cxy[0][1])
        val G = floatArrayOf(Cxy[1][0], Cxy[1][1], 1f - Cxy[1][0] - Cxy[1][1])
        val B = floatArrayOf(Cxy[2][0], Cxy[2][1], 1f - Cxy[2][0] - Cxy[2][1])
        val W = floatArrayOf(DT[0], DT[1], 1f - DT[0] - DT[1])

        // Compute luminance weights for the primaries using the white point
        val RGB = vec(R, G, B)
        val WGB = vec(W, G, B)
        val RWB = vec(R, W, B)
        val RGW = vec(R, G, W)

        val rgbDet = RGB.det().toFloat()

        return floatArrayOf(
                R[1] * WGB.det().toFloat() / (W[1] * rgbDet),
                G[1] * RWB.det().toFloat() / (W[1] * rgbDet),
                B[1] * RGW.det().toFloat() / (W[1] * rgbDet))
    }

    /*
        8.2 - HSI, HSL, HSV, and related color spaces

        The representation of the colors in the RGB and CMY(K) color spaces are designed
        for specific devices. But for a human observer, they are not useful definitions.
        For user interfaces a more intuitive color space, designed for the way we actually
        think about color is to be preferred. Such a color space is HSI; Hue, Saturation and
        Intensity, which can be thought of as a RGB cube tipped up onto one corner. The line
        from RGB=min to RGB=max becomes verticle and is the intensity axis. The position of a
        point on the circumference of a circle around this axis is the hue and the saturation
        is the radius from the central intensity axis to the color.

                 Green
                  /\
                /    \    ^
              /V=1 x   \   \ Hue (angle, so that Hue(Red)=0,
       Blue -------------- Red       Hue(Green)=120, and Hue(blue)=240 deg)
            \      |     /
             \     |-> Saturation (distance from the central axis)
              \    |   /
               \   |  /
                \  | /
                 \ |/
               V=0 x (Intensity=0 at the top of the apex and =1 at the base of the cone)

        The big disadvantage of this model is the conversion which is mainly because the hue is
        expressed as an angle. The transforms are given below:

            Hue = (Alpha-arctan((Red-intensity)*(3^0.5)/(Green-Blue)))/(2*PI)
            with { Alpha=PI/2 if Green>Blue
                 { Aplha=3*PI/2 if Green<Blue
                 { Hue=1 if Green=Blue
            Saturation = (Red^2+Green^2+Blue^2-Red*Green-Red*Blue-Blue*Green)^0.5
            Intensity = (Red+Green+Blue)/3

        Note that you have to compute Intensity *before* Hue. If not, you must assume that:

            Hue = (Alpha-arctan((2*Red-Green-Blue)/((Green-Blue)*(3^0.5))))/(2*PI).

        I assume that H, S, L, R, G, and B are within the range of [0;1]. Another point of view of
        this cone is to project the coordinates onto the base. The 2D projection is:

            Red:   (1;0)
            Green: (cos(120 deg);sin(120 deg)) = (-0.5; 0.866)
            Blue:  (cos(240 deg);sin(240 deg)) = (-0.5;-0.866)

        Now you need intermediate coordinates:

            x = Red-0.5*(Green+Blue)
            y = 0.866*(Green-Blue)

        Finally, you have:

            Hue = arctan2(x,y)/(2*PI) ; Just one formula, always in the correct quadrant
            Saturation = (x^2+y^2)^0.5
            Intensity = (Red+Green+Blue)/3

        The intermediate coordinates YST { I, x, y } are a cool substitute for HSI in most calculations

            RGBtoYST = new float[][]{{Wr, Wg, Wb, 0},
                                      {1, -.5, -.5, 0},
                                      {0, .5 * Math.sqrt(3), -.5 * Math.sqrt(3), 0},
                                      {0, 0, 0, 1}};

            YSTtoRGB = new float[][] {{1, (Wg+Wb), (Wb - Wg) / Math.sqrt(3), 0},
                                       {1, (-Wr), (2*Wb + Wr) / Math.sqrt(3), 0},
                                       {1, (-Wr), -(2*Wg + Wr) / Math.sqrt(3), 0}};

        The code below uses a normalized version of the intermediate coordinates YST to make sure it fits in the
        dynamic range of a positive integer representation
    */

    abstract class LinearTransform {

        internal abstract val transform: Array<DoubleArray>

        fun fromRGB(dataType: Int): Array<DoubleArray?> {
            val t = scaleTransform(transform, dataType)
            return strip(t)
        }

        fun toRGB(dataType: Int): Array<DoubleArray?> {
            val t = scaleTransform(transform, dataType)
            return strip(Matrix(t).inverse().array)
        }

        companion object {
            internal fun scaleTransform(t: Array<DoubleArray>, dataType: Int): Array<DoubleArray> {
                when (dataType) {
                    DataBuffer.TYPE_BYTE -> {
                        t[1][3] = 0xFF / 2.0
                        t[2][3] = 0xFF / 2.0
                    }
                    DataBuffer.TYPE_SHORT -> {
                        t[1][3] = java.lang.Short.MAX_VALUE / 2.0
                        t[2][3] = java.lang.Short.MAX_VALUE / 2.0
                    }
                    DataBuffer.TYPE_USHORT -> {
                        t[1][3] = 0xFFFF / 2.0
                        t[2][3] = 0xFFFF / 2.0
                    }
                    DataBuffer.TYPE_INT -> {
                        t[1][3] = Integer.MAX_VALUE / 2.0
                        t[2][3] = Integer.MAX_VALUE / 2.0
                    }
                    DataBuffer.TYPE_FLOAT, DataBuffer.TYPE_DOUBLE -> {
                        t[1][3] = 0.5
                        t[2][3] = 0.5
                    }
                }
                return t
            }
        }
    }

    class XYZ : LinearTransform() {
        override val transform: Array<DoubleArray>
            get() = arrayOf(
                    doubleArrayOf(Wr.toDouble(), Wg.toDouble(), Wb.toDouble(), 0.0),
                    doubleArrayOf(0.5, -0.25, -0.25, 0.5),
                    doubleArrayOf(0.0, 0.5, -0.5, 0.5),
                    doubleArrayOf(0.0, 0.0, 0.0, 1.0))
    }

    class YST : LinearTransform() {
        override val transform: Array<DoubleArray>
            get() = arrayOf(
                    doubleArrayOf(Wr.toDouble(), Wg.toDouble(), Wb.toDouble(), 0.0),
                    doubleArrayOf(0.5, -0.25, -0.25, 0.5),
                    doubleArrayOf(0.0, 0.5, -0.5, 0.5),
                    doubleArrayOf(0.0, 0.0, 0.0, 1.0))
    }

    class LLab : LinearTransform() {
        override val transform: Array<DoubleArray>
            get() = arrayOf(
                    doubleArrayOf(Wr.toDouble(), Wg.toDouble(), Wb.toDouble(), 0.0),
                    doubleArrayOf(0.5, -0.5, 0.0, 0.5),
                    doubleArrayOf(0.0, 0.5, -0.5, 0.5),
                    doubleArrayOf(0.0, 0.0, 0.0, 1.0))
    }

    class YCC : LinearTransform() {
        override val transform: Array<DoubleArray>
            get() = arrayOf(
                    doubleArrayOf(Wr.toDouble(), Wg.toDouble(), Wb.toDouble(), 0.0),
                    doubleArrayOf(-Wr / (2 - 2 * Wb) + .5, -Wg / (2 - 2 * Wb) + .5, (1 - Wb) / (2 - 2 * Wb) + .5, 0.0),
                    doubleArrayOf((1 - Wr) / (2 - 2 * Wr) + .5, -Wg / (2 - 2 * Wr) + .5, -Wb / (2 - 2 * Wr) + .5, 0.0),
                    doubleArrayOf(0.0, 0.0, 0.0, 1.0))
    }

    private fun vec(A: FloatArray, B: FloatArray, C: FloatArray): Matrix {
        val ABC = Matrix(3, 3)
        for (i in 0..2) {
            ABC.set(i, 0, A[i].toDouble())
        }
        for (i in 0..2) {
            ABC.set(i, 1, B[i].toDouble())
        }
        for (i in 0..2) {
            ABC.set(i, 2, C[i].toDouble())
        }
        return ABC
    }

    fun strip(x: Array<DoubleArray>): Array<DoubleArray?> {
        val r = arrayOfNulls<DoubleArray>(3)
        r[0] = x[0]
        r[1] = x[1]
        r[2] = x[2]
        return r
    }

    enum class CAMethod {
        Bradford, VonKries, CAT02, Sharp, CMCCAT2000, XYZScaling, Mixed
    }

    internal fun mixer(t: Float): Float = (Math.atan(((t - 5000) / 100).toDouble()) / Math.PI + 0.5).toFloat()

    internal fun matrix(t: Float): Array<FloatArray> {
        val matrix = Array(3) { FloatArray(3) }
        val m = mixer(t)

        for (i in 0..2) {
            for (j in 0..2) {
                matrix[i][j] = XYZScaling[i][j] * (1 - m) + Bradford[i][j] * m
            }
        }

        return matrix
    }

    @JvmOverloads
    fun chromaticAdaptation(source: Float, target: Float, caMethod: CAMethod = CAMethod.Bradford): Array<FloatArray> {
        val method: Array<FloatArray> = when (caMethod) {
            ColorScience.CAMethod.Bradford -> Bradford
            ColorScience.CAMethod.VonKries -> VonKries
            ColorScience.CAMethod.CAT02 -> CAT02
            ColorScience.CAMethod.Sharp -> Sharp
            ColorScience.CAMethod.CMCCAT2000 -> CMCCAT2000
            ColorScience.CAMethod.Mixed -> matrix(target)
            ColorScience.CAMethod.XYZScaling -> XYZScaling
        }

        val B = Matrix(method)

        // source illuminant tristimulus in cone response domain
        var sXYZ = xy2XYZ(D(source))
        sXYZ = Matrix(arrayOf(floatArrayOf(sXYZ[0], sXYZ[1], sXYZ[2]))).times(B).getArrayFloat()[0]

        // target illuminant tristimulus in cone response domain
        var tXYZ = xy2XYZ(D(target))
        tXYZ = Matrix(arrayOf(floatArrayOf(tXYZ[0], tXYZ[1], tXYZ[2]))).times(B).getArrayFloat()[0]

        // scaling matrix for the colors
        val diag = arrayOf(
                floatArrayOf(sXYZ[0] / tXYZ[0], 0f, 0f),
                floatArrayOf(0f, sXYZ[1] / tXYZ[1], 0f),
                floatArrayOf(0f, 0f, sXYZ[2] / tXYZ[2]))

        // total tansform
        return B.times(Matrix(diag)).times(B.inverse()).getArrayFloat()
    }

    fun saturation(r: Double, g: Double, b: Double): Double {
        val min = Math.min(r, Math.min(g, b))
        val max = Math.max(r, Math.max(g, b))
        return if (max != 0.0) 1 - min / max else 0.0
    }

    fun neutralTemperature(rgb: FloatArray, refT: Float, caMethod: CAMethod): FloatArray {
        var sat = Double.MAX_VALUE
        var minT = 0
        var wbr = 0.0
        var wbg = 0.0
        var wbb = 0.0

        val color = Matrix(arrayOf(
                doubleArrayOf(rgb[0].toDouble()),
                doubleArrayOf(rgb[1].toDouble()),
                doubleArrayOf(rgb[2].toDouble())))

        var t = 1000
        while (t < 40000) {
            val B = Matrix(chromaticAdaptation(t.toFloat(), refT, caMethod))
            val combo = XYZtoRGB.times(B.times(RGBtoZYX))

            val adapdedColor = combo.times(color)

            val r = adapdedColor.get(0, 0).clamp(0.0, 1.0)
            val g = adapdedColor.get(1, 0).clamp(0.0, 1.0)
            val b = adapdedColor.get(2, 0).clamp(0.0, 1.0)

            val tSat = saturation(r, g, b)

            if (tSat < sat) {
                sat = tSat
                minT = t
                wbr = r
                wbg = g
                wbb = b
            }
            t += (0.001 * t).toInt()
        }

        val tint =
                if (wbr != 0.0 || wbg != 0.0 || wbb != 0.0)
                    (-(wbg - (wbr + wbb) / 2)).toFloat()
                else
                    0f

        return floatArrayOf(minT.toFloat(), tint)
    }

    fun findTemperature(rgb: FloatArray, refT: Float, caMethod: CAMethod): Float {
        var minDiff = Double.MAX_VALUE
        var minT = 0

        val xyzRef = JAIContext.linearColorSpace.toCIEXYZ(rgb)
        val labRef = JAIContext.labColorSpace.fromCIEXYZ(xyzRef)

        var gray = Matrix(arrayOf(doubleArrayOf(0.18), doubleArrayOf(0.18), doubleArrayOf(0.18)))

        var t = 1000
        while (t < 40000) {
            val B = Matrix(chromaticAdaptation(t.toFloat(), refT, caMethod))
            val combo = XYZtoRGB.times(B.times(RGBtoZYX))

            gray = combo.times(gray)

            val r = gray.get(0, 0).clamp(0.0, 1.0)
            val g = gray.get(1, 0).clamp(0.0, 1.0)
            val b = gray.get(2, 0).clamp(0.0, 1.0)

            val xyzGray = JAIContext.linearColorSpace.toCIEXYZ(floatArrayOf(r.toFloat(), g.toFloat(), b.toFloat()))
            val labGray = JAIContext.labColorSpace.fromCIEXYZ(xyzGray)

            val diff = (1..2)
                    .map { labGray[it] - labRef[it] }
                    .sumByDouble { (it * it).toDouble() }
                    .let { Math.sqrt(it) }

            if (diff < minDiff) {
                minDiff = diff
                minT = t
            }
            t += (0.001 * t).toInt()
        }
        return minT.toFloat()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        for (i in 2000 until 25000 step 500) {
            println("m(" + i + ") : " + mixer(i.toFloat()))
        }

        val D65 = D(whitePointTemperature)

        println("D65: " + D65[0] + ", " + D65[1] + ", " + (1f - D65[0] - D65[1]))

        println("xr: " + Cxy[0][0] + ", yr: " + Cxy[0][1])
        println("xg: " + Cxy[1][0] + ", yg: " + Cxy[1][1])
        println("xb: " + Cxy[2][0] + ", yb: " + Cxy[2][1])
        println("W: $Wr, $Wg, $Wb")
        println("RGBToXYZ")

        for (i in 0..2) {
            for (j in 0..2) {
                print(" " + RGBToXYZMat[i][j])
            }
            println()
        }

        val rgb2xyz = RGBtoZYX()

        println("rgb2xyz")
        for (i in 0..2) {
            for (j in 0..2) {
                print(" " + rgb2xyz[i][j])
            }
            println()
        }

        println("XYZToRGB")
        for (i in 0..2) {
            for (j in 0..2) {
                print(" " + XYZToRGBMat[i][j])
            }
            println()
        }
    }
}
