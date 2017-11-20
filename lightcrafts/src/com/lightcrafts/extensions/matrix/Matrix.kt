/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.matrix

import Jama.Matrix

fun Matrix(A: Array<FloatArray>): Matrix {

    val m = A.size
    val n = A[0].size

    A.find { it.size != n }
            ?.run { throw IllegalArgumentException("All rows must have the same length.") }

    val mat = Matrix(m, n)

    for (i in 0 until m) {
        for (j in 0 until n) {
            mat.set(i, j, A[i][j].toDouble())
        }
    }
    return mat
}

fun Matrix.getArrayFloat(): Array<FloatArray> {

    val floatArray = Array(rowDimension) { FloatArray(columnDimension) }

    for (i in 0 until rowDimension) {
        for (j in 0 until columnDimension) {
            floatArray[i][j] = array[i][j].toFloat()
        }
    }
    return floatArray
}
