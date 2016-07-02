/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.image.types;

/**
 * A <code>MEFImageType</code> is-a {@link RawImageType} for MEF (Minolta RaW)
 * images.
 *
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public class MEFImageType extends RawImageType {

    /** The singleton instance of <code>MEFImageType</code>. */
    public static final MEFImageType INSTANCE = new MEFImageType();

    /** {@inheritDoc} */
    @Override
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "MEF";
    }

    /**
     * Construct an <code>MEFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private MEFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for MEF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
            "mef"
    };
}
