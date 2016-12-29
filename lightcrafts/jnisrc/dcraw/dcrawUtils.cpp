/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

#include <jni.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_jai_opimage_RGBDemosaicOpImage.h"
#endif

#include "LC_JNIUtils.h"

#define DEMOSAIC_METHOD(method) \
        name4(Java_,com_lightcrafts_jai_opimage_RGBDemosaicOpImage,_,method)

#include<limits.h>
#include<omp.h>

static inline int clipUShort(unsigned short sample)
{
    return sample < 0      ? 0
         : sample > 0xffff ? 0xffff
                           : (unsigned short) sample;
}

#if _OPENMP >= 201307
#pragma omp declare simd
#endif
static inline int _abs(int x) {
    return x < 0 ? -x : x;
}

static inline bool isOdd(int x)
{
    return x & 1;
}

static inline bool isSameMathParity(int a, int b)
{
    return isOdd(a) == isOdd(b);
}

void interpolateGreen(
        unsigned short* srcData, unsigned short* destData, const int width, const int height,
        const int srcLineStride, const int destLineStride,
        const int srcOffset, const int rOffset, const int gOffset, const int bOffset,
        const int gx, const int gy, const int ry)
{
    // copy RAW data to RGB layer and remove hot pixels

#pragma omp for schedule (dynamic)
    for (int y = 0; y < height; ++y) {
        const int cOffset = isSameMathParity(y, ry) ? rOffset : bOffset;
        const int x0 = isSameMathParity(y, gy) ? gx+1 : gx;
        for (int x = 0; x < width; ++x) {
            const bool colorPixel = isSameMathParity(x, x0);
            const int offset = colorPixel ? cOffset : gOffset;

            int value = srcData[y * srcLineStride + x + srcOffset];
            if (x >= 2 && x < width-2 && y >= 2 && y < height-2) {
                int v[12];
                int n;
                if (!colorPixel) {
                    n = 8;
                    v[0] = srcData[(y-1) * srcLineStride + x-1 + srcOffset];
                    v[1] = srcData[(y-1) * srcLineStride + x+1 + srcOffset];
                    v[2] = srcData[(y+1) * srcLineStride + x-1 + srcOffset];
                    v[3] = srcData[(y+1) * srcLineStride + x+1 + srcOffset];

                    v[4] = 2 * srcData[(y-1) * srcLineStride + x + srcOffset];
                    v[5] = 2 * srcData[(y+1) * srcLineStride + x + srcOffset];
                    v[6] = 2 * srcData[y * srcLineStride + x-1 + srcOffset];
                    v[7] = 2 * srcData[y * srcLineStride + x+1 + srcOffset];
                } else {
                    n = 12;
                    v[0] = srcData[(y-2) * srcLineStride + x + srcOffset];
                    v[1] = srcData[(y+2) * srcLineStride + x + srcOffset];
                    v[2] = srcData[y * srcLineStride + x-2 + srcOffset];
                    v[3] = srcData[y * srcLineStride + x+2 + srcOffset];

                    v[4] = 2 * srcData[(y-1) * srcLineStride + x-1 + srcOffset];
                    v[5] = 2 * srcData[(y-1) * srcLineStride + x+1 + srcOffset];
                    v[6] = 2 * srcData[(y+1) * srcLineStride + x-1 + srcOffset];
                    v[7] = 2 * srcData[(y+1) * srcLineStride + x+1 + srcOffset];

                    v[8] = 2 * srcData[(y-1) * srcLineStride + x + srcOffset];
                    v[9] = 2 * srcData[(y+1) * srcLineStride + x + srcOffset];
                    v[10] = 2 * srcData[y * srcLineStride + x-1 + srcOffset];
                    v[11] = 2 * srcData[y * srcLineStride + x+1 + srcOffset];
                };
                bool replace = true;
                for (int i = 0; i < n; ++i)
                    if (value < 2 * v[i]) {
                        replace = false;
                        break;
                    }
                if (replace)
                    value = (v[0] + v[1] + v[2] + v[3]) / 4;
            }
            destData[3 * (y * destLineStride + x) + offset] = value;
        }
    }

    // green channel interpolation

#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; ++y) {
        const int cOffset = isSameMathParity(y, ry) ? rOffset : bOffset;
        const int x0 = isSameMathParity(y, gy) ? gx+1 : gx;

        int hl = destData[3 * (y * destLineStride + (x0-1)) + gOffset];
        int cxy = destData[3 * (y * destLineStride + x0) + cOffset];
        int chl = destData[3 * (y * destLineStride + (x0-2)) + cOffset];

        const int x_min = isOdd(x0) ? 3 : 2; 
        for (int x = x_min; x < width-2; x += 2) {
            const int hr = destData[3 * (y * destLineStride + (x+1)) + gOffset];
            const int vu = destData[3 * ((y-1) * destLineStride + x) + gOffset];
            const int vd = destData[3 * ((y+1) * destLineStride + x) + gOffset];
            const int dh = _abs(hl - hr);
            const int dv = _abs(vu - vd);

            const int chr = destData[3 * (y * destLineStride + (x+2)) + cOffset];
            const int cvu = destData[3 * ((y-2) * destLineStride + x) + cOffset];
            const int cvd = destData[3 * ((y+2) * destLineStride + x) + cOffset];
            const int cdh = _abs(chl + chr - 2 * cxy);
            const int cdv = _abs(cvu + cvd - 2 * cxy);

            // we're doing edge directed bilinear interpolation on the green channel,
            // which is a low pass operation (averaging), so we add some signal from the
            // high frequencies of the observed color channel

            int sample;
            if (dv + cdv - (dh + cdh) > 0) {
                sample = (hl + hr) / 2;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (chl + chr)/2) / 4;
            } else if (dh + cdh - (dv + cdv) > 0) {
                sample = (vu + vd) / 2;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (cvu + cvd)/2) / 4;
            } else {
                sample = (vu + hl + vd + hr) / 4;
                if (sample < 4 * cxy && cxy < 4 * sample)
                    sample += (cxy - (chl + chr + cvu + cvd)/4) / 8;
            }

            destData[3 * (y * destLineStride + x) + gOffset] = clipUShort(sample);

            hl = hr;
            chl = cxy;
            cxy = chr;
        }
    }

    // get the constant component out of the reconstructed green pixels and add to it
    // the "high frequency" part of the corresponding observed color channel

#pragma omp for schedule (dynamic)
    for (int y = 2; y < height-2; ++y) {
        const int cOffset = isSameMathParity(y, ry) ? rOffset : bOffset;
        const int x0 = isSameMathParity(y, gy) ? gx+1 : gx;

        int xy = destData[3 * (y * destLineStride + x0) + gOffset];
        int hl = destData[3 * (y * destLineStride + x0-2) + gOffset];
        int ul = destData[3 * ((y-2) * destLineStride + x0-2) + gOffset];
        int bl = destData[3 * ((y+2) * destLineStride + x0-2) + gOffset];

        int cxy = destData[3 * (y * destLineStride + x0) + cOffset];
        int chl = destData[3 * (y * destLineStride + x0-2) + cOffset];
        int cul = destData[3 * ((y-2) * destLineStride + x0-2) + cOffset];
        int cbl = destData[3 * ((y+2) * destLineStride + x0-2) + cOffset];

        int x = 2;

        int vu = destData[3 * ((y-2) * destLineStride + x) + gOffset];
        int vd = destData[3 * ((y+2) * destLineStride + x) + gOffset];

        int cvu = destData[3 * ((y-2) * destLineStride + x) + cOffset];
        int cvd = destData[3 * ((y+2) * destLineStride + x) + cOffset];

        for (; x < width-2; x += 2) {
            const int hr = destData[3 * (y * destLineStride + x+2) + gOffset];
            const int ur = destData[3 * ((y-2) * destLineStride + x+2) + gOffset];
            const int br = destData[3 * ((y+2) * destLineStride + x+2) + gOffset];

            const int chr = destData[3 * (y * destLineStride + x+2) + cOffset];
            const int cur = destData[3 * ((y-2) * destLineStride + x+2) + cOffset];
            const int cbr = destData[3 * ((y+2) * destLineStride + x+2) + cOffset];

            // Only work on the pixels that have a strong enough correlation between channels

            if (xy < 4 * cxy && cxy < 4 * xy) {
                const int vec1[4] = {hl, vu, ul, ur};
                const int vec2[4] = {hr, vd, br, bl};
                const int cvec1[4] = {chl, cvu, cul, cur};
                const int cvec2[4] = {chr, cvd, cbr, cbl};

                int diffs[4], cdiffs[4], gradients[4];
#if _OPENMP >= 201307
#pragma omp simd
#endif
                for (int i = 0; i < 4; ++i) {
                    diffs[i]  = xy  - ( vec1[i] +  vec2[i]) / 2;
                    cdiffs[i] = cxy - (cvec1[i] + cvec2[i]) / 2;
                }
#if _OPENMP >= 201307
#pragma omp simd
#endif
                for (int i = 0; i < 4; ++i) {
                    gradients[i] = _abs(diffs[i] + cdiffs[i]);
                }

                //  -1: flat, 0: horizontal, 1: vertical, 2: north-east, 3: north-west
                int min_dir = -1;
                int min_grad = xy / 4;
#if _OPENMP >= 201307
#pragma omp simd reduction(min:min_grad)
#endif
                for (int i = 0; i < 4; ++i) {
                    if (gradients[i] < min_grad) {
                        min_grad = gradients[i];
                        min_dir = i;
                    }
                }

                // Only work on parts of the image that have enough "detail"

                if (min_dir != -1) { // min_grad > xy / 4
                    const int sample = (xy + (xy - diffs[min_dir]) + cdiffs[min_dir]) / 2;
                    destData[3 * (y * destLineStride + x) + gOffset] = clipUShort(sample);
                }
            }

            hl = xy;
            xy = hr;
            ul = vu;
            vu = ur;
            bl = vd;
            vd = br;
            chl = cxy;
            cxy = chr;
            cul = cvu;
            cvu = cur;
            cbl = cvd;
            cvd = cbr;
        }
    }
}

void interpolateRedOrBlue(
        unsigned short* data, const int width, const int height,
        const int lineStride,
        const int gOffset, const int cOffset,
        const int cx0, const int cy0)
{
#pragma omp for schedule (dynamic)
    for (int y = 1; y < height-1; ++y) {
        for (int x = 1; x < width-1; ++x) {
            if (!isSameMathParity(x+cx0, cx0) || !isSameMathParity(y+cy0, cy0)) {
                int sample;
                const int cg = data[3 * (x + cx0 + (y + cy0) * lineStride) + gOffset];

                if (!isSameMathParity(x+cx0, cx0) && !isSameMathParity(y+cy0, cy0)) {
                    // Pixel at the other color location
                    const int gne = data[3 * (x + cx0 - 1 + (y + cy0 + 1) * lineStride) + gOffset];
                    const int gnw = data[3 * (x + cx0 + 1 + (y + cy0 + 1) * lineStride) + gOffset];
                    const int gsw = data[3 * (x + cx0 + 1 + (y + cy0 - 1) * lineStride) + gOffset];
                    const int gse = data[3 * (x + cx0 - 1 + (y + cy0 - 1) * lineStride) + gOffset];

                    const int cne = gne - data[3 * (x + cx0 - 1 + (y + cy0 + 1) * lineStride) + cOffset];
                    const int cnw = gnw - data[3 * (x + cx0 + 1 + (y + cy0 + 1) * lineStride) + cOffset];
                    const int csw = gsw - data[3 * (x + cx0 + 1 + (y + cy0 - 1) * lineStride) + cOffset];
                    const int cse = gse - data[3 * (x + cx0 - 1 + (y + cy0 - 1) * lineStride) + cOffset];

                    sample = cg - (cne + csw + cnw + cse) / 4;
                } else if (isSameMathParity(x+cx0, cx0) && !isSameMathParity(y+cy0, cy0)) {
                    // Pixel at green location - vertical
                    const int gu = data[3 * (x + cx0 + (y + cy0 - 1) * lineStride) + gOffset];
                    const int gd = data[3 * (x + cx0 + (y + cy0 + 1) * lineStride) + gOffset];

                    const int cu = gu - data[3 * (x + cx0 + (y + cy0 - 1) * lineStride) + cOffset];
                    const int cd = gd - data[3 * (x + cx0 + (y + cy0 + 1) * lineStride) + cOffset];

                    sample = cg - (cu + cd) / 2;
                } else {
                    // Pixel at green location - horizontal
                    const int gl = data[3 * (x + cx0 - 1 + (y + cy0) * lineStride) + gOffset];
                    const int gr = data[3 * (x + cx0 + 1 + (y + cy0) * lineStride) + gOffset];

                    const int cl = gl - data[3 * (x + cx0 - 1 + (y + cy0) * lineStride) + cOffset];
                    const int cr = gr - data[3 * (x + cx0 + 1 + (y + cy0) * lineStride) + cOffset];

                    sample = cg - (cl + cr) / 2;
                }

                data[3 * (x + cx0 + (y + cy0) * lineStride) + cOffset] = clipUShort(sample);
            }
        }
    }
}

JNIEXPORT void JNICALL DEMOSAIC_METHOD(demosaic)
( JNIEnv *env, jclass cls,
  jshortArray jsrcData, jshortArray jdestData, jint width, jint height,
  jint srcLineStride, jint destLineStride,
  jint srcOffset, jint rOffset, jint gOffset, jint bOffset,
  jint rx, jint ry, jint gx, jint gy, jint bx, jint by)
{
    unsigned short *srcData  = (unsigned short *) env->GetPrimitiveArrayCritical(jsrcData, 0);
    unsigned short *destData = (unsigned short *) env->GetPrimitiveArrayCritical(jdestData, 0);

#pragma omp parallel shared (srcData, destData)
    {
        interpolateGreen(srcData, destData, width, height, srcLineStride, destLineStride,
                srcOffset, rOffset, gOffset, bOffset, gx, gy, ry);
        interpolateRedOrBlue(destData, width, height, destLineStride, gOffset, rOffset, rx, ry);
        interpolateRedOrBlue(destData, width, height, destLineStride, gOffset, bOffset, bx, by);
    }

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdestData, destData, 0);
}

/* vim:set et sw=4 ts=4: */
