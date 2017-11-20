/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.extensions.primitive

fun Short.toUnsignedInt(): Int = toInt() and 0xffff
