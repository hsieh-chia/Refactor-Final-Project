package com.jhlabs.image;

import java.awt.image.Kernel;

public class convolveV implements IConvolve{
    @Override
    public void convolveProcessing(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int halfRows = rows / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;

                for (int row = -halfRows; row <= halfRows; row++) {
                    int eachRoundY = y + row;
                    int ioffset;
                    if (eachRoundY < 0) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = 0;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = eachRoundY * width;
                        }
                    } else if (eachRoundY >= height) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = (height - 1) * width;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = eachRoundY * width;
                        }
                    } else {
                        ioffset = eachRoundY * width;
                    }

                    float f = matrix[row + halfRows];

                    if (f != 0) {
                        int rgb = inputPixels[ioffset + x];
                        a += f * ((rgb >> 24) & 0xff);
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }
                int eachRoundAlpha = alpha ? PixelUtils.clamp((int) (a + 0.5)) : 0xff;
                int eachRoundRed = PixelUtils.clamp((int) (r + 0.5));
                int eachRoundGreen = PixelUtils.clamp((int) (g + 0.5));
                int eachRoundBlue = PixelUtils.clamp((int) (b + 0.5));
                outputPixels[index++] = (eachRoundAlpha << 24) | (eachRoundRed << 16) | (eachRoundGreen << 8) | eachRoundBlue;
            }
        }
    }
}