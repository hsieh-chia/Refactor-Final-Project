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

import net.jafama.FastMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.Future;

/**
 * Renders a color wheel
 */
public class ColorWheel extends ParametrizedFilter {
    public static final String NAME = "Color Wheel";

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final AngleParam hueShiftParam = new AngleParam("Rotate (Degrees)", 0);
    private final RangeParam brightnessParam = new RangeParam("Brightness (%)", 0, 75, 100);
    private final RangeParam satParam = new RangeParam("Saturation (%)", 0, 90, 100);

    public ColorWheel() {
        super(ShowOriginal.NO);

        setParams(center, hueShiftParam, brightnessParam, satParam);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] destData = ImageUtils.getPixelsAsArray(dest);

        int width = dest.getWidth();
        int height = dest.getHeight();

        int cx = (int) (width * center.getRelativeX());
        int cy = (int) (height * center.getRelativeY());

        float hueShift = (float) hueShiftParam.getValueInRadians();
        float saturation = satParam.getPercentageValF();
        float brightness = brightnessParam.getPercentageValF();

        var pt = new StatusBarProgressTracker(NAME, height);

        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calculateLine(
                    destData, width, finalY, cx, cy, hueShift, saturation, brightness);
            futures[y] = ThreadPool.submit(lineTask);
        }
        ThreadPool.waitFor(futures, pt);
        pt.finished();

        return dest;
    }

    private static void calculateLine(int[] destData, int width, int y,
                                      int cx, int cy, float hueShift,
                                      float saturation, float brightness) {
        for (int x = 0; x < width; x++) {
            int yDiff = cy - y;
            int xDiff = x - cx;
            float angle = (float) FastMath.atan2(yDiff, xDiff) + hueShift;
            float hue = (float) (angle / (2 * Math.PI));

            destData[x + y * width] = Color.HSBtoRGB(hue, saturation, brightness);
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
