/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Resolves the keyboard modifiers the Haiku LWAWT toolkit uses for menu-item
 * accelerators and Swing mnemonic activation.
 *
 * <p>Two system properties control the policy:
 *
 * <dl>
 *   <dt>{@code -Dhaiku.menu.modifier}</dt>
 *   <dd>Modifier reported by {@link java.awt.Toolkit#getMenuShortcutKeyMaskEx()}.
 *       Swing apps install accelerators against this value
 *       (e.g. {@code KeyStroke.getKeyStroke('V', mask)} for paste).
 *       Default {@code "alt"} — the Haiku-native Command modifier, which
 *       maps to the physical left Alt key. Native Haiku menus draw
 *       these as ⌘C / ⌘V style glyphs.</dd>
 *
 *   <dt>{@code -Dhaiku.mnemonic.modifier}</dt>
 *   <dd>Modifier reported by {@link sun.awt.SunToolkit#getFocusAcceleratorKeyMask()},
 *       which Swing uses to register and dispatch mnemonic KeyStrokes
 *       (modifier+letter fires the {@code JMenuItem}/{@code JButton}
 *       with that mnemonic). Also drives the keycode whose press/release
 *       toggles the mnemonic-underline flash via
 *       {@link sun.swing.AltProcessor}. Default {@code "ctrl"} so that
 *       mnemonics never share a KeyStroke with the default Alt-based
 *       menu shortcuts.</dd>
 * </dl>
 *
 * <p>Accepted values for both properties: {@code alt}, {@code ctrl}
 * (alias {@code control}), {@code shift}, {@code meta}
 * (aliases {@code command}, {@code cmd}). Unknown values fall back to the
 * property's default.
 *
 * <p>This class is initialized once during {@link HaikuToolkit} static
 * setup. After that, {@link #installAltProcessorOverride()} pushes the
 * resolved mnemonic keycode into {@link sun.swing.AltProcessor} so the
 * visual mnemonic-flash and menu-bar entry follow the configured key
 * instead of the historical {@code VK_ALT}.
 */
final class HaikuShortcuts {

    /**
     * Symbolic identifiers for the modifiers we support exposing through
     * the Haiku toolkit. Encapsulates the four representations Swing
     * cares about (legacy mask, modern mask, raw keycode) so calling
     * code never reaches for {@code InputEvent} / {@code KeyEvent}
     * constants directly.
     */
    enum Modifier {
        ALT  (InputEvent.ALT_MASK,   InputEvent.ALT_DOWN_MASK,   KeyEvent.VK_ALT),
        CTRL (InputEvent.CTRL_MASK,  InputEvent.CTRL_DOWN_MASK,  KeyEvent.VK_CONTROL),
        SHIFT(InputEvent.SHIFT_MASK, InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_SHIFT),
        META (InputEvent.META_MASK,  InputEvent.META_DOWN_MASK,  KeyEvent.VK_META);

        @SuppressWarnings("deprecation")
        final int mask;
        final int maskEx;
        final int keyCode;

        @SuppressWarnings("deprecation")
        Modifier(int mask, int maskEx, int keyCode) {
            this.mask = mask;
            this.maskEx = maskEx;
            this.keyCode = keyCode;
        }

        /**
         * Parse a property value into a {@link Modifier}, falling back
         * to {@code fallback} for {@code null}, blank, or unrecognized
         * values.
         */
        static Modifier parse(String value, Modifier fallback) {
            if (value == null) return fallback;
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "alt"                          -> ALT;
                case "ctrl", "control"              -> CTRL;
                case "shift"                        -> SHIFT;
                case "meta", "command", "cmd"       -> META;
                default                             -> fallback;
            };
        }
    }

    /** Modifier used for menu-item accelerators. */
    static final Modifier MENU_MODIFIER = Modifier.parse(
            System.getProperty("haiku.menu.modifier"), Modifier.ALT);

    /** Modifier used for mnemonic activation and the AltProcessor flash key. */
    static final Modifier MNEMONIC_MODIFIER = Modifier.parse(
            System.getProperty("haiku.mnemonic.modifier"), Modifier.CTRL);

    /**
     * Push the configured mnemonic keycode into {@link sun.swing.AltProcessor}
     * so the underline flash and menu-bar entry follow the same physical
     * key Swing now uses for mnemonic activation.
     */
    static void installAltProcessorOverride() {
        sun.swing.AltProcessor.setMnemonicKeyCode(MNEMONIC_MODIFIER.keyCode);
    }

    private HaikuShortcuts() {}
}
