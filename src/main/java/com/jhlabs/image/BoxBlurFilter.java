/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;

/**
 * A filter which performs a box blur on an image. The horizontal and vertical blurs can be specified separately
 * and a number of iterations can be given which allows an approximation to Gaussian blur.
 */
public class BoxBlurFilter extends AbstractBufferedImageOp {
    private float hRadius;
    private float vRadius;
    private int iterations = 1;
    private boolean premultiplyAlpha = true;

    /**
     * Construct a default BoxBlurFilter.
     */
    public BoxBlurFilter(String filterName) {
        super(filterName);
    }

    /**
     * Construct a BoxBlurFilter.
     *
     * @param hRadius    the horizontal radius of blur
     * @param vRadius    the vertical radius of blur
     * @param iterations the number of time to iterate the blur
     */
    public BoxBlurFilter(float hRadius, float vRadius, int iterations, String filterName) {
        super(filterName);

        this.hRadius = hRadius;
        this.vRadius = vRadius;
        this.iterations = iterations;
    }

    /**
     * Set whether to premultiply the alpha channel.
     *
     * @param premultiplyAlpha true to premultiply the alpha
     * @see #getPremultiplyAlpha
     */
    public void setPremultiplyAlpha(boolean premultiplyAlpha) {
        this.premultiplyAlpha = premultiplyAlpha;
    }

    /**
     * Get whether to premultiply the alpha channel.
     *
     * @return true to premultiply the alpha
     * @see #setPremultiplyAlpha
     */
    public boolean getPremultiplyAlpha() {
        return premultiplyAlpha;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        // the fractional blur is not included - it should
        // be executed only for tweening
        pt = createProgressTracker(iterations * (width + height));

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        if (premultiplyAlpha) {
            ImageMath.premultiply(inPixels, 0, inPixels.length);
        }

        for (int i = 0; i < iterations; i++) {
            blur(inPixels, outPixels, width, height, hRadius, pt);
            blur(outPixels, inPixels, height, width, vRadius, pt);
        }

        boolean blurFractional = false;
        double hFraction = hRadius - Math.floor(hRadius);
        double vFraction = vRadius - Math.floor(vRadius);
        if (hFraction > 0.001 || vFraction > 0.001) {
            blurFractional = true;
        }

        if (blurFractional) {
            blurFractional(inPixels, outPixels, width, height, hRadius);
            blurFractional(outPixels, inPixels, height, width, vRadius);
        }

        if (premultiplyAlpha) {
            ImageMath.unpremultiply(inPixels, 0, inPixels.length);
        }
        setRGB(dst, 0, 0, width, height, inPixels);

        finishProgressTracker();

        return dst;
    }

    /**
     * Blur and transpose a block of ARGB pixels.
     *
     * @param in     the input pixels
     * @param out    the output pixels
     * @param width  the width of the pixel array
     * @param height the height of the pixel array
     * @param radius the radius of blur
     * @param pt
     */
    public static void blur(int[] in, int[] out, int width, int height, float radius, ProgressTracker pt) {
        int widthMinus1 = width - 1;
        int r = (int) radius;
        int tableSize = 2 * r + 1;

        if (tableSize < 0) {
            throw new IllegalArgumentException(String.format("tableSize is negative, radius = %.2f", radius));
        }

        int[] divide = new int[256 * tableSize];

        for (int i = 0; i < 256 * tableSize; i++) {
            divide[i] = i / tableSize;
        }

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            int ta = 0, tr = 0, tg = 0, tb = 0;

            for (int i = -r; i <= r; i++) {
                int rgb = in[inIndex + ImageMath.clamp(i, 0, width - 1)];
                PixelRGB pixelrgb = new PixelRGB(rgb);
                ta += pixelrgb.getA();
                tr += pixelrgb.getR();
                tg += pixelrgb.getG();
                tb += pixelrgb.getB();
            }

            for (int x = 0; x < width; x++) {
                out[outIndex] = (divide[ta] << 24) | (divide[tr] << 16) | (divide[tg] << 8) | divide[tb];

                int i1 = x + r + 1;
                if (i1 > widthMinus1) {
                    i1 = widthMinus1;
                }
                int i2 = x - r;
                if (i2 < 0) {
                    i2 = 0;
                }
                int rgb1 = in[inIndex + i1];
                int rgb2 = in[inIndex + i2];

                ta += ((rgb1 >> 24) & 0xff) - ((rgb2 >> 24) & 0xff);
                tr += ((rgb1 & 0xff0000) - (rgb2 & 0xff0000)) >> 16;
                tg += ((rgb1 & 0xff00) - (rgb2 & 0xff00)) >> 8;
                tb += (rgb1 & 0xff) - (rgb2 & 0xff);
                outIndex += height;
            }
            inIndex += width;

            pt.unitDone();
        }
    }

    public static void blurFractional(int[] in, int[] out, int width, int height, float radius) {
        radius -= (int) radius;
        float f = 1.0f / (1 + 2 * radius);
        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;

            out[outIndex] = in[0];
            outIndex += height;
            for (int x = 1; x < width - 1; x++) {
                int i = inIndex + x;
                PixelRGB rgb1 = new PixelRGB(in[i - 1]);
                PixelRGB rgb2 = new PixelRGB(in[i]);
                PixelRGB rgb3 = new PixelRGB(in[i + 1]);


                int a1 = rgb1.getA();
                int r1 = rgb1.getR();
                int g1 = rgb1.getG();
                int b1 = rgb1.getB();
                
                int a2 = rgb2.getA();
                int r2 = rgb2.getR();
                int g2 = rgb2.getG();
                int b2 = rgb2.getB();
                
                int a3 = rgb3.getA();
                int r3 = rgb3.getR();
                int g3 = rgb3.getG();
                int b3 = rgb3.getB();
                
                a1 = a2 + (int) ((a1 + a3) * radius);
                r1 = r2 + (int) ((r1 + r3) * radius);
                g1 = g2 + (int) ((g1 + g3) * radius);
                b1 = b2 + (int) ((b1 + b3) * radius);
                a1 = (int) (a1 * f);
                r1 = (int) (r1 * f);
                g1 = (int) (g1 * f);
                b1 = (int) (b1 * f);
                out[outIndex] = (a1 << 24) | (r1 << 16) | (g1 << 8) | b1;
                outIndex += height;
            }

            // Laszlo: we get an array index out of bounds exception here
            // if either the width or the height of the image is 1 pixel.
            // In Pixelitor these cases are already filtered before we get here
            out[outIndex] = in[width - 1];
            inIndex += width;
        }
    }

    /**
     * Set the horizontal size of the blur.
     *
     * @param hRadius the radius of the blur in the horizontal direction
     * @min-value 0
     * @see #getHRadius
     */
    public void setHRadius(float hRadius) {
        this.hRadius = hRadius;
    }

    /**
     * Get the horizontal size of the blur.
     *
     * @return the radius of the blur in the horizontal direction
     * @see #setHRadius
     */
    public float getHRadius() {
        return hRadius;
    }

    /**
     * Set the vertical size of the blur.
     *
     * @param vRadius the radius of the blur in the vertical direction
     * @min-value 0
     * @see #getVRadius
     */
    public void setVRadius(float vRadius) {
        this.vRadius = vRadius;
    }

    /**
     * Get the vertical size of the blur.
     *
     * @return the radius of the blur in the vertical direction
     * @see #setVRadius
     */
    public float getVRadius() {
        return vRadius;
    }

    /**
     * Set both the horizontal and vertical sizes of the blur.
     *
     * @param radius the radius of the blur in both directions
     * @min-value 0
     * @see #getRadius
     */
    public void setRadius(float radius) {
        hRadius = vRadius = radius;
    }

    /**
     * Get the size of the blur.
     *
     * @return the radius of the blur in the horizontal direction
     * @see #setRadius
     */
    public float getRadius() {
        return hRadius;
    }

    /**
     * Set the number of iterations the blur is performed.
     *
     * @param iterations the number of iterations
     * @min-value 0
     * @see #getIterations
     */
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /**
     * Get the number of iterations the blur is performed.
     *
     * @return the number of iterations
     * @see #setIterations
     */
    public int getIterations() {
        return iterations;
    }

    @Override
    public String toString() {
        return "Blur/Box Blur...";
    }
}
