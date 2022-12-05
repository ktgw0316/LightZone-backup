/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * @(#)SplashWindow.java  2.2  2005-04-03
 *
 * Copyright (c) 2003-2005 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is in the public domain.
 */

package com.lightcrafts.splash;

import com.lightcrafts.app.ComboFrame;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * A Splash window.
 *  <p>
 * Usage: MyApplication is your application class. Create a Splasher class which
 * opens the splash window, invokes the main method of your Application class,
 * and disposes the splash window afterwards.
 * Please note that we want to keep the Splasher class and the SplashWindow class
 * as small as possible. The less code and the less classes must be loaded into
 * the JVM to open the splash screen, the faster it will appear.
 * <pre>
 * class Splasher {
 *    public static void main(String[] args) {
 *         SplashWindow.splash(Startup.class.getResource("splash.gif"));
 *         MyApplication.main(args);
 *         SplashWindow.disposeSplash();
 *    }
 * }
 * </pre>
 *
 * @author  Werner Randelshofer
 * @version 2.1 2005-04-03 Revised.
 */
public final class SplashWindow extends Frame {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Closes the splash window.
     */
    public static void disposeSplash() {
        if ( INSTANCE != null ) {
            INSTANCE.dispose();
            INSTANCE = null;
        }
    }

    /**
     * Invokes the main method of the provided class name.
     * @param args the command line arguments
     */
    public static void invokeMain( String className, String[] args ) {
        try {
            Class.forName( className )
                .getMethod( "main", new Class[]{ String[].class } )
                .invoke( null, new Object[]{ args } );
        }
        catch ( Exception e ) {
            final InternalError error =
                new InternalError( "Failed to invoke main method" );
            error.initCause( e );
            throw error;
        }
    }

    /**
     * Paints the image on the window.
     */
    public void paint( Graphics g ) {
        g.drawImage( m_image, 0, 0, this );

        // Notify method splash that the window
        // has been painted.
        // Note: To improve performance we do not enter
        // the synchronized block unless we have to.
        if ( !m_paintCalled ) {
            m_paintCalled = true;
            synchronized ( this ) {
                notifyAll();
            }
        }
    }

    /**
     * Queues a repaint on the singleton SplashWindow.  Useful for dynamic
     * splash images.
     */
    public static void repaintInstance() {
        // We may already have been disposed by a mouse click:
        if (INSTANCE != null) {
            INSTANCE.repaint();
        }
    }

    /**
     * Open's a splash window using the specified image.
     * @param image The splash image.
     */
    public static void splash( Image image ) {
        if ( INSTANCE == null && image != null ) {

            // Create the splash image
            INSTANCE = new SplashWindow( image );

            // Show the window.
            INSTANCE.setVisible(true);

            // Note: To make sure the user gets a chance to see the
            // splash window we wait until its paint method has been
            // called at least once by the AWT event dispatcher thread.
            // If more than one processor is available, we don't wait,
            // and maximize CPU throughput instead.
            if ( ! EventQueue.isDispatchThread()
                && Runtime.getRuntime().availableProcessors() == 1 )
            {
                synchronized ( INSTANCE ) {
                    while ( !INSTANCE.m_paintCalled ) {
                        try {
                            INSTANCE.wait();
                        }
                        catch ( InterruptedException e ) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    /**
     * Open's a splash window using the specified image.
     * @param imageURL The url of the splash image.
     */
    public static void splash( URL imageURL ) {
        if ( imageURL != null )
            splash( Toolkit.getDefaultToolkit().createImage( imageURL ) );
    }

    /**
     * Updates the display area of the window.
     */
    public void update( Graphics g ) {
        // Note: Since the paint method is going to draw an
        // image that covers the complete area of the component we
        // do not fill the component with its background color
        // here. This avoids flickering.
        paint( g );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The current instance of the splash window.
     * (Singleton design pattern).
     */
    private static SplashWindow INSTANCE;

    /**
     * The splash image which is displayed on the splash window.
     */
    private Image m_image;

    /**
     * This attribute indicates whether the method
     * paint(Graphics) has been called at least once since the
     * construction of this window.<br>
     * This attribute is used to notify method splash(Image)
     * that the window has been drawn at least once
     * by the AWT event dispatcher thread.<br>
     * This attribute acts like a latch. Once set to true,
     * it will never be changed back to false again.
     *
     * @see #paint
     * @see #splash
     */
    private boolean m_paintCalled;

    /**
     * Creates a new instance.
     * @param image the splash image.
     */
    private SplashWindow( Image image ) {
        m_image = image;

	// Java 1.6 will just use a cofee cup otherwise...
	setIconImage(ComboFrame.IconImage);

        setUndecorated(true);

        // Load the image
        final MediaTracker mt = new MediaTracker( this );
        mt.addImage( image, 0 );
        try {
            mt.waitForID( 0 );
        }
        catch ( InterruptedException ie ) {
            // ignore
        }

        // Center the window on the screen
        final int imgWidth = image.getWidth(this);
        final int imgHeight = image.getHeight(this);
        setSize(imgWidth, imgHeight);
        // Note: Do not use this since the window spans monitors
        // on multi-monitor environment.
        /*
        final Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
            (screenDim.width - imgWidth) / 2,
            (screenDim.height - imgHeight) / 2
        );
        */
        final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDisplayMode();
        setLocation(
            (dm.getWidth() - imgWidth) / 2,
            (dm.getHeight() - imgHeight) / 2
        );

        // Users shall be able to close the splash window by
        // clicking on its display area. This mouse listener
        // listens for mouse clicks and disposes the splash window.
        final MouseAdapter disposeOnClick = new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                // Note: To avoid that method splash hangs, we
                // must set paintCalled to true and call notifyAll.
                // This is necessary because the mouse click may
                // occur before the contents of the window
                // has been painted.
                synchronized ( SplashWindow.this ) {
                    SplashWindow.this.m_paintCalled = true;
                    SplashWindow.this.notifyAll();
                }
                disposeSplash();
            }
        };
        addMouseListener( disposeOnClick );
    }
}
/* vim:set et sw=4 ts=4: */
