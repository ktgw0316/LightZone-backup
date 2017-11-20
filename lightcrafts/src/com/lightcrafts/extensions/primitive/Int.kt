/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.primitive

fun Int.clamp(lo: Int, hi: Int): Int = let{Math.max(lo, Math.min(hi, it))}

fun Int.clampUnsignedShort(): Short = clamp(0, 0xffff).toShort()

