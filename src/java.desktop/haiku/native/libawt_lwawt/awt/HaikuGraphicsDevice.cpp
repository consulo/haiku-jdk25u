/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#include <Screen.h>
#include <Font.h>
#include <InterfaceDefs.h>
#include <algorithm>

#include "Utilities.h"

extern "C" {

/*
 * Class:     sun_hawt_HaikuGraphicsDevice
 * Method:    nativeGetScaleFactor
 * Signature: (I)D
 *
 * Haiku has no direct screen-scale API. Per the Haiku-recommended
 * heuristic (https://discuss.haiku-os.org/t/getting-the-screen-dpi/13121/5),
 * derive scale from the user's plain-font size relative to the 12pt baseline.
 */
JNIEXPORT jdouble JNICALL
Java_sun_hawt_HaikuGraphicsDevice_nativeGetScaleFactor(JNIEnv *env,
    jclass clazz, jint displayID)
{
    // Haiku has no single "UI scale" setting. Per the community-blessed
    // heuristic (see haiku-development mailing list "HiDPI strategies,
    // current and future", 2021-08-30), derive scale from the
    // system-font size relative to the 12pt baseline.
    //
    // Take the largest of plain / bold / fixed: HiDPI Haiku setups
    // generally bump all three together, but if a user only enlarges
    // the bold/decorator font we still want to scale to match.
    //
    // NOTE: these globals only reflect the user's current Appearance
    // preferences AFTER a Haiku reboot — beta4 release notes explicitly
    // state "it is not possible for changes to these settings to take
    // effect without a reboot."
    //
    // TODO: when waddlesplash's proposed BFont::PixelDensity() or
    // BScreen::DefaultFont() ever lands, switch to that API for proper
    // per-display scaling on multi-monitor setups. Today displayID is
    // ignored because all displays share the system font.
    const float baseline = 12.0f;
    float plainSize = (be_plain_font != NULL) ? be_plain_font->Size() : 0.0f;
    float boldSize  = (be_bold_font  != NULL) ? be_bold_font->Size()  : 0.0f;
    float fixedSize = (be_fixed_font != NULL) ? be_fixed_font->Size() : 0.0f;

    float biggest = baseline;
    if (plainSize > biggest) biggest = plainSize;
    if (boldSize  > biggest) biggest = boldSize;
    if (fixedSize > biggest) biggest = fixedSize;

    float scale = biggest / baseline;
    if (scale < 1.0f) scale = 1.0f;

    fprintf(stderr,
        "[hawt] nativeGetScaleFactor: display=%d plain=%.1f bold=%.1f "
        "fixed=%.1f baseline=%.1f -> biggest=%.1f ratio=%.3f scale=%.3f\n",
        (int) displayID, plainSize, boldSize, fixedSize, baseline,
        biggest, biggest / baseline, scale);

    return (jdouble) scale;
}

/*
 * Class:     sun_hawt_HaikuGraphicsDevice
 * Method:    nativeGetYResolution
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuGraphicsDevice_nativeGetScreenResolution(JNIEnv *env,
	jclass clazz, jint displayID, jdoubleArray resolution)
{
	screen_id id;
	id.id = displayID;
	BScreen screen(id);
	if (!screen.IsValid())
		return;

	// This doesn't work with the VirtualBox screen, but
	// I'm guessing it's OK elsewhere.
	monitor_info info;
	if (screen.GetMonitorInfo(&info) != B_OK)
		return;

	// 2.54 cm == 1 inch
	double width = info.width / 2.54;
	double height = info.height / 2.54;

	if (width != 0) {
		jdouble xRes = (screen.Frame().Width() + 1) / width;
		env->SetDoubleArrayRegion(resolution, 0, 1, &xRes);
	}
	if (height != 0) {
		jdouble yRes = (screen.Frame().Width() + 1) / height;
		env->SetDoubleArrayRegion(resolution, 1, 1, &yRes);
	}
}

}
