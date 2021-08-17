/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.platform.linux;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.Version;

import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.JHelp;
import javax.swing.*;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPlatform extends Platform {

    private final static String home = System.getProperty( "user.home" );

    // My understanding of the state of standard linux color profile
    // locations comes from:
    //
    //      http://bugs.freestandards.org/show_bug.cgi?id=77

    private final static File SystemProfileDir = new File(
        "/usr/share/color/icc"
    );

    private final static File UserProfileDir = new File(
        home, ".color/icc"
    );

    private static Collection<ColorProfileInfo> Profiles;

    @Override
    public File getDefaultImageDirectory() {
        ProcessBuilder pb = new ProcessBuilder("xdg-user-dir", "PICTURES");
        try {
            Process p = pb.start();
            final String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                line = br.readLine();
            }
            p.waitFor();
            p.destroy();
            if (p.exitValue() == 0 && line != null && ! line.equals(home)) {
                return new File(line);
            }
        }
        catch (IOException | InterruptedException ignored) {
        }

        return new File( home, Version.getApplicationName() );
    }

    @Override
    public File getLightZoneDocumentsDirectory() {
        final String appName = Version.getApplicationName();
        final String path = ".local/share/" + appName;
        return new File( home, path );
    }

    @Override
    public LookAndFeel getLookAndFeel() {
        return LightZoneSkin.getLightZoneLookAndFeel();
    }

    @Override
    public FileChooser getFileChooser() {
        return new LinuxFileChooser();
    }

    @Override
    public ICC_Profile getDisplayProfile() {
        Preferences prefs = Preferences.userRoot().node(
            "/com/lightcrafts/platform/linux"
        );
        String path = prefs.get("DisplayProfile", null);
        if (path != null) {
            try {
                return ICC_Profile.getInstance(path);
            }
            catch (Throwable e) {
                System.err.println("Malformed display profile at " + path);
                // return null;
            }
        }
        return null;
    }

    @Override
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getColorProfiles();
    }

    @Override
    public Collection<ColorProfileInfo> getExportProfiles() {
        return getColorProfiles();
    }

    private static synchronized Collection<ColorProfileInfo> getColorProfiles() {
        if (Profiles == null) {
            Profiles = new HashSet<>();
            Profiles.addAll(getColorProfiles(SystemProfileDir));
            Profiles.addAll(getColorProfiles(UserProfileDir));
        }
        return Profiles;
    }

    @Override
    public void makeModal(Dialog dialog) {
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
    }

    @Override
    public void showHelpTopic(String topic) {
        // TODO: use the "topic" argument to pick an initial page
        try {
            URL url = HelpSet.findHelpSet(null, "LightZone.hs");
            HelpSet help = new HelpSet(null, url);
            String title = help.getTitle();
            JHelp jhelp = new JHelp(help);
            help.setHomeID("index");
            try {
                jhelp.setCurrentID(topic);
            }
            catch (Throwable t) {
                jhelp.setCurrentID("index");
            }
            JFrame frame = new JFrame();
            frame.setTitle(title);
            frame.setContentPane(jhelp);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
        catch (HelpSetException e) {
            getPlatform().getAlertDialog().showAlert(
                null,
                "Couldn't initialize the LightZone help system.",
                e.getClass().getName() + ": " + e.getMessage(),
                AlertDialog.ERROR_ALERT,
                "OK");
        }
    }

    public static void main(String[] args)
        throws UnsupportedLookAndFeelException
    {
        Platform platform = Platform.getPlatform();
        platform.loadLibraries();
        System.out.println(platform.getPhysicalMemoryInMB());

        UIManager.setLookAndFeel(platform.getLookAndFeel());
        platform.showHelpTopic("New_Features");
    }
}
/* vim:set et sw=4 ts=4: */
