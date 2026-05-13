/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.hawt;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.util.ArrayList;

import sun.java2d.SunGraphicsEnvironment;

public class HaikuGraphicsDevice extends GraphicsDevice {

    private final int displayID;
    private final HaikuGraphicsConfig config;
    // Resolved lazily on the first getScaleFactor() call so the native
    // be_*_font globals have a chance to be populated by app_server
    // (HaikuToolkit.nativeInit creates the BApplication that does that).
    // 0.0 means "not yet resolved". Fractional values are supported
    // (1.5, 2.25, ...) — matches Windows Win32GraphicsDevice.
    private volatile double scale;

    /** Haiku's font-size baseline that corresponds to a 1x UI scale. */
    private static final double FONT_BASELINE = 12.0;

    private native double nativeGetScreenResolution(int displayID,
        double[] resolution);
    /**
     * Fills {@code sizes} with the current sizes of plain, bold and fixed
     * system fonts respectively. {@code sizes.length} must be 3. A 0.0
     * entry means the corresponding native global was unavailable.
     */
    private static native void nativeGetSystemFontSizes(double[] sizes);

    public HaikuGraphicsDevice(int displayID) {
        this.displayID = displayID;
        config = new HaikuGraphicsConfig(this);
    }

    public double getScaleFactor() {
        double s = scale;
        if (s == 0.0) {
            s = resolveScale();
            scale = s;
        }
        return s;
    }

    private double resolveScale() {
        if (!SunGraphicsEnvironment.isUIScaleEnabled()) {
            return 1.0;
        }
        double debugScale = SunGraphicsEnvironment.getDebugScale();
        if (debugScale >= 1) {
            return debugScale;
        }

        // Heuristic: take the largest of the system fonts (Plain / Bold /
        // Fixed) and divide by the 12pt baseline. HiDPI Haiku setups bump
        // the bold/decorator font even when Plain stays at 12, so the max
        // gives the most reliable signal. Fractional results (Plain=18 ->
        // 1.5) flow through to the rest of the pipeline unrounded.
        double[] sizes = new double[3];
        nativeGetSystemFontSizes(sizes);
        double biggest = FONT_BASELINE;
        for (double size : sizes) {
            if (size > biggest) biggest = size;
        }
        return biggest / FONT_BASELINE;
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[] { config };
    }

    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return config;
    }

    @Override
    public String getIDstring() {
        return "Display " + this.displayID;
    }

    @Override
    public int getType() {
        return TYPE_RASTER_SCREEN;
    }

    @Override
    public boolean isDisplayChangeSupported() {
        return false;
    }

    @Override
    public boolean isFullScreenSupported() {
        return false;
    }

    public int getDisplayID() {
        return displayID;
    }

    public double getXResolution() {
        double[] resolution = new double[2];
        nativeGetScreenResolution(displayID, resolution);
        return resolution[0] != 0.0 ? resolution[0] : 72.0;
    }

    public double getYResolution() {
        double[] resolution = new double[2];
        nativeGetScreenResolution(displayID, resolution);
        return resolution[1] != 0.0 ? resolution[1] : 72.0;
    }

    public double getScreenResolution() {
        double[] resolution = new double[2];
        nativeGetScreenResolution(displayID, resolution);
        double res = (resolution[0] + resolution[1]) / 2;
        return res != 0.0 ? res : 72.0;
    }
}
