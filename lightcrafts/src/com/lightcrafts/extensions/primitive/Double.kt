/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.primitive

fun Double.clamp(lo: Double, hi: Double): Double = let{Math.max(lo, Math.min(hi, it))}
