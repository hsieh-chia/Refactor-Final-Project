/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import javax.swing.*;
import java.net.URL;

/**
 * Icon-related static utility methods
 */
public final class Icons {
    private static final Icon westArrowIcon = load("west_arrow.gif");
    private static final Icon diceIcon = load("dice.png");
    private static final Icon dice2Icon = load("dice2.png");
    private static final Icon northArrowIcon = load("north_arrow.gif");
    private static final Icon southArrowIcon = load("south_arrow.gif");
    private static final Icon textLayerIcon = load("text_layer_icon.png");
    private static final Icon adjLayerIcon = load("adj_layer_icon.png");
    private static final Icon undoIcon = load("undo.png");
    private static final Icon redoIcon = load("redo.png");
    private static final Icon searchIcon = load("search.png");

    private Icons() {
        // should not be instantiated
    }

    public static Icon getWestArrowIcon() {
        return westArrowIcon;
    }

    public static Icon getDiceIcon() {
        return diceIcon;
    }

    public static Icon getTwoDicesIcon() {
        return dice2Icon;
    }

    public static Icon load(String iconFileName) {
        assert iconFileName != null;

        URL imgURL = ImageUtils.resourcePathToURL(iconFileName);
        return new ImageIcon(imgURL);
    }

    public static Icon getNorthArrowIcon() {
        return northArrowIcon;
    }

    public static Icon getSouthArrowIcon() {
        return southArrowIcon;
    }

    public static Icon getTextLayerIcon() {
        return textLayerIcon;
    }

    public static Icon getAdjLayerIcon() {
        return adjLayerIcon;
    }

    public static Icon getUndoIcon() {
        return undoIcon;
    }

    public static Icon getRedoIcon() {
        return redoIcon;
    }

    public static Icon getSearchIcon() {
        return searchIcon;
    }
}