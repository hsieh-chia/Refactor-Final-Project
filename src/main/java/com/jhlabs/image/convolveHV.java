package com.jhlabs.image;

import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Kernel;


public class convolveHV implements IConvolve{
	
    @Override
    public void convolveProcessing(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int cols = kernel.getWidth();
        int halfRows = rows / 2;
        int halfCols = cols / 2;

        final ProgressTracker pt = new StatusBarProgressTracker("ConvolveFilter", height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;

                for (int row = -halfRows; row <= halfRows; row++) {
                    int eachRoundY = y + row;
                    int ioffset;
                    if (0 <= eachRoundY && eachRoundY < height) {
                        ioffset = eachRoundY * width;
                    } else if (edgeAction == CLAMP_EDGES) {
                        ioffset = y * width;
                    } else if (edgeAction == WRAP_EDGES) {
                        ioffset = ((eachRoundY + height) % height) * width;
                    } else {
                        continue;
                    }
                    int moffset = cols * (row + halfRows) + halfCols;
                    for (int col = -halfCols; col <= halfCols; col++) {
                        float f = matrix[moffset + col];

                        if (f != 0) {
                            int eachRoundX = x + col;
                            if (!(0 <= eachRoundX && eachRoundX < width)) {
                                if (edgeAction == CLAMP_EDGES) {
                                    eachRoundX = x;
                                } else if (edgeAction == WRAP_EDGES) {
                                    eachRoundX = (x + width) % width;
                                } else {
                                    continue;
                                }
                            }
                            int rgb = inputPixels[ioffset + eachRoundX];
                            a += f * ((rgb >> 24) & 0xff);
                            r += f * ((rgb >> 16) & 0xff);
                            g += f * ((rgb >> 8) & 0xff);
                            b += f * (rgb & 0xff);
                        }
                    }
                }
                int eachRoundAlpha = alpha ? PixelUtils.clamp((int) (a + 0.5)) : 0xff;
                int eachRoundRed = PixelUtils.clamp((int) (r + 0.5));
                int eachRoundGreen = PixelUtils.clamp((int) (g + 0.5));
                int eachRoundBlue = PixelUtils.clamp((int) (b + 0.5));
                outputPixels[index++] = (eachRoundAlpha << 24) | (eachRoundRed << 16) | (eachRoundGreen << 8) | eachRoundBlue;
            }
            pt.unitDone();
        }
        pt.finished();
    }
}