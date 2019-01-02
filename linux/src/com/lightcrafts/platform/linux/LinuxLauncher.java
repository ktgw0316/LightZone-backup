/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform.linux;

import com.lightcrafts.platform.Launcher;

import javax.swing.SwingUtilities;

public final class LinuxLauncher extends Launcher {

    public static void main(String[] args) {
        final Launcher launcher = new LinuxLauncher();
        SwingUtilities.invokeLater(() -> launcher.init(args));
    }

    @Override
    protected void checkCpu() {
        if (! TestSSE2.hasSSE2()) {
            TestSSE2.showDialog();
            System.exit(0);
        }
    }

}
