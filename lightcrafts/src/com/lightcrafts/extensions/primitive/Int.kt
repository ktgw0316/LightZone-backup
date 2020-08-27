/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.primitive

import kotlin.math.max
import kotlin.math.min

fun Int.clamp(lo: Int, hi: Int): Int = max(lo, min(hi, this))

fun Int.clampUnsignedShort(): Short = clamp(0, 0xffff).toShort()

