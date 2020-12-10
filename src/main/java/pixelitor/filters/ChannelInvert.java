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

package pixelitor.filters;

import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.gui.GUIText;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Inverts only some of the RGB or HSB channels
 */
public class ChannelInvert extends ParametrizedFilter {
    public static final String NAME = "Channel Invert";

    private static final int NOTHING = 0;
    private static final int RED_ONLY = 1;
    private static final int GREEN_ONLY = 2;
    private static final int BLUE_ONLY = 3;

    private static final int RED_GREEN = 4;
    private static final int RED_BLUE = 5;
    private static final int GREEN_BLUE = 6;

    private static final int RED_GREEN_BLUE = 7;

    private static final int HUE_ONLY = 8;
    private static final int SATURATION_ONLY = 9;
    private static final int BRI_ONLY = 10;

    private static final int HUE_SAT = 11;
    private static final int HUE_BRI = 12;
    private static final int SAT_BRI = 13;
    private static final int HUE_SAT_BRI = 14;

    private final Item[] invertChoices = {
        new Item("Nothing", NOTHING),

        new Item(GUIText.HUE, HUE_ONLY),
        new Item(GUIText.SATURATION, SATURATION_ONLY),
        new Item(GUIText.BRIGHTNESS, BRI_ONLY),

        new Item("Hue and Saturation", HUE_SAT),
        new Item("Hue and Brightness", HUE_BRI),
        new Item("Saturation and Brightness", SAT_BRI),

        new Item("Hue, Saturation and Brightness", HUE_SAT_BRI),

        new Item("Red", RED_ONLY),
        new Item("Green", GREEN_ONLY),
        new Item("Blue", BLUE_ONLY),

        new Item("Red and Green", RED_GREEN),       //yellow
        new Item("Red and Blue", RED_BLUE),         //purple
        new Item("Green and Blue", GREEN_BLUE),     

        new Item("Red, Green and Blue", RED_GREEN_BLUE),        //black & white
    };

    private final IntChoiceParam invertTypeSelector = new IntChoiceParam("Invert Channel", invertChoices);

    public ChannelInvert() {
        super(ShowOriginal.YES);

        setParams(invertTypeSelector);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int invertType = invertTypeSelector.getValue();
        if (invertType == NOTHING) {
            return src;
        }
        if (invertType >= HUE_ONLY) {
            return invertHSB(invertType, src, dest);
        }

        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        for (int i = 0; i < destData.length; i++) {
            int srcPixel = srcData[i];
            int alpha = srcPixel & 0xFF000000;
            if (alpha == 0) {
                destData[i] = srcPixel;
            } else {
                destData[i] = switch (invertType) {
                    case RED_ONLY -> srcPixel ^ 0x00FF0000;
                    case GREEN_ONLY -> srcPixel ^ 0x0000FF00;
                    case BLUE_ONLY -> srcPixel ^ 0x000000FF;
                    case RED_GREEN -> srcPixel ^ 0x00FFFF00;
                    case RED_BLUE -> srcPixel ^ 0x00FF00FF;
                    case GREEN_BLUE -> srcPixel ^ 0x0000FFFF;
                    case RED_GREEN_BLUE -> srcPixel ^ 0x00FFFFFF;
                    default -> throw new IllegalStateException("Unexpected type: " + invertType);
                };
            }
        }

        return dest;
    }

    private static BufferedImage invertHSB(int invertType, BufferedImage src, BufferedImage dest) {
        int[] srcData = ImageUtils.getPixelsAsArray(src);
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        float[] hsb = {0.0f, 0.0f, 0.0f};
        // HSB(HSV) 通過色相/飽和度/亮度三要素來表達顏色.
        // H(Hue):表示顏色的型別(例如紅色,綠色或者黃色).取值範圍為0—360.其中每一個值代表一種顏色.
        // S(Saturation):顏色的飽和度.從0到1.有時候也稱為純度.(0表示灰度圖,1表示純的顏色)
        // B(Brightness or Value):顏色的明亮程度.從0到1.(0表示黑色,1表示特定飽和度的顏色)

        for (int i = 0; i < destData.length; i++) {
            int srcPixel = srcData[i];
            int a = srcPixel & 0xFF000000;
            if (a == 0) {
                destData[i] = srcPixel;
                continue;
            }
            int r = (srcPixel >>> 16) & 0xFF;
            int g = (srcPixel >>> 8) & 0xFF;
            int b = srcPixel & 0xFF;
            hsb = Color.RGBtoHSB(r, g, b, hsb);
            int newRGB = switch (invertType) {
                case HUE_ONLY -> Color.HSBtoRGB(0.5f + hsb[0], hsb[1], hsb[2]);
                case BRI_ONLY -> Color.HSBtoRGB(hsb[0], hsb[1], 1.0f - hsb[2]);
                case SATURATION_ONLY -> Color.HSBtoRGB(hsb[0], 1.0f - hsb[1], hsb[2]);
                case HUE_BRI -> Color.HSBtoRGB(0.5f + hsb[0], hsb[1], 1.0f - hsb[2]);
                case HUE_SAT -> Color.HSBtoRGB(0.5f + hsb[0], 1.0f - hsb[1], hsb[2]);
                case SAT_BRI -> Color.HSBtoRGB(hsb[0], 1.0f - hsb[1], 1.0f - hsb[2]);
                case HUE_SAT_BRI -> Color.HSBtoRGB(0.5f + hsb[0], 1.0f - hsb[1], 1.0f - hsb[2]);
                default -> 0;
            };

            //  alpha is 255 here
            newRGB &= 0x00FFFFFF;  // set alpha to 0
            destData[i] = a | newRGB; // add the real alpha
        }

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}