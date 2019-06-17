/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.GPSDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import lombok.val;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public class GPSMetadataEntry extends MetadataEntry {

    @Override
    public String getLabel(ImageMetadata meta) {
        return "GPS"; // TODO: LOCALE.get("GPSLabel");
    }

    @Override
    public Object getValue(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return "";
        }
        val lat = dir.getGPSLatitude();
        val lon = dir.getGPSLongitude();
        if (lat == null || lon == null) {
            return "";
        }
        val icon = getMapTileImageIcon(lat, lon, 17);
        if (icon != null) {
            return icon;
        }
        return getDMS(dir);
    }

    private String getDMS(GPSDirectory dir) {
        val latDMS = dir.getGPSLatitudeDMS();
        val lonDMS = dir.getGPSLongitudeDMS();
        return (latDMS.isEmpty() || lonDMS.isEmpty())
                ? ""
                : latDMS + ", " + lonDMS;
    }

    @Override
    boolean isEditable(ImageInfo info) {
        return false;
    }

    @Override
    boolean isValidValue(ImageMetadata meta, String value) {
        return true;
    }

    @Override
    void setValue(ImageMetadata meta, String value) {
        // readonly
    }

    @Override
    URI getURI(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return null;
        }
        Double latitude  = dir.getGPSLatitude();
        Double longitude = dir.getGPSLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }

        // TODO: OpenStreetMap
        // c.f. https://developers.google.com/maps/documentation/urls/guide
        try {
            return new URI("https://www.google.com/maps/search/?api=1&query="
                    + latitude + "," + longitude);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    // cf. https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
    private static String getOpenStreetMapTileNumber(double lat, double lon, int zoom) {
        int xtile = (int)Math.floor((lon + 180) / 360 * (1<<zoom));
        int ytile = (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat))
                + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = (1 << zoom) - 1;
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = (1 << zoom) - 1;
        return("" + zoom + "/" + xtile + "/" + ytile);
    }

    private static ImageIcon getMapTileImageIcon(double lat, double lon, int zoom) {
        try {
            val url = new URL("https://tile.openstreetmap.fr/hot/"
                    + getOpenStreetMapTileNumber(lat, lon, zoom) + ".png");
            val con = url.openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(1000);
            try (val in = con.getInputStream()) {
                val img = ImageIO.read(in);
                return new ImageIcon(img);
            }
        } catch (IOException ignored) {
            return null;
        }
    }
}
