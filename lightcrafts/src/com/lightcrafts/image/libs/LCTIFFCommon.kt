/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs

/**
 * An `LCTIFFCommon` factors out common code for [LCTIFFReader] and [ ].
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see [LibTIFF](http://www.remotesensing.org/libtiff/)
 */
open class LCTIFFCommon {

    /**
     * This is where the native code stores a pointer to the `TIFF` native data
     * structure.  Do not touch this from Java.
     */
    internal var m_nativePtr: Long = 0

    /**
     * Dispose of an `LCTIFFWriter`.  Calling this more than once is guaranteed to be
     * harmless.
     */
    fun dispose() = TIFFClose()

    /**
     * Finalize this class by calling [.dispose].
     */
    @Throws(Throwable::class)
    protected fun finalize() = dispose()

    /**
     * Closes the TIFF file.
     */
    private external fun TIFFClose()

    companion object {

        init {
            System.loadLibrary("LCTIFF")
            init()
        }

        /**
         * Initializes the native library.
         */
        private external fun init()

        /**
         * This is called from the native code to throw an [LCImageLibException].  Doing it this
         * way is less work than having the native code call the constructor directly.
         *
         * @param msg The error message.
         */
        @Suppress("unused")
        @Throws(LCImageLibException::class)
        @JvmStatic
        private fun throwException(msg: String): Unit = throw LCImageLibException(msg)
    }
}
/* vim:set et sw=4 ts=4: */
