/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.primitive

import kotlin.math.max
import kotlin.math.min

fun Float.clamp(lo: Float, hi: Float): Float = max(lo, min(hi, this))
