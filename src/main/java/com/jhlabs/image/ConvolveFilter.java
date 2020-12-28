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

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Kernel;

/**
 * A filter which applies a convolution kernel to an image.
 *
 * @author Jerry Huxtable
 */
public class ConvolveFilter extends AbstractBufferedImageOp {
    /**
     * Treat pixels off the edge as zero.
     */
    public static final int ZERO_EDGES = 0;

    /**
     * Clamp pixels off the edge to the nearest edge.
     */
    public static final int CLAMP_EDGES = 1;

    /**
     * Wrap pixels off the edge to the opposite edge.
     */
    public static final int WRAP_EDGES = 2;

    /**
     * The convolution kernel.
     */
    protected Kernel kernel = null;

    /**
     * Whether to convolve alpha.
     */
    protected boolean alpha = true;

    /**
     * Whether to promultiply the alpha before convolving.
     */
    protected boolean premultiplyAlpha = true;

    /**
     * What do do at the image edges.
     */
    private int edgeAction = CLAMP_EDGES;

    /**
     * Construct a filter with a null kernel. This is only useful if you're going to change the kernel later on.
     */
    public ConvolveFilter(String filterName) {
        this(new float[9], filterName);
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param matrix an array of 9 floats containing the kernel
     */
    public ConvolveFilter(float[] matrix, String filterName) {
        this(new Kernel(3, 3, matrix), filterName);
    }

    /**
     * Construct a filter with the given kernel.
     *
     * @param rows   the number of rows in the kernel
     * @param cols   the number of columns in the kernel
     * @param matrix an array of rows*cols floats containing the kernel
     */
    public ConvolveFilter(int rows, int cols, float[] matrix, String filterName) {
        this(new Kernel(cols, rows, matrix), filterName);
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param kernel the convolution kernel
     */
    public ConvolveFilter(Kernel kernel, String filterName) {
        super(filterName);
        this.kernel = kernel;
    }

    /**
     * Set the convolution kernel.
     *
     * @param kernel the kernel
     * @see #getKernel
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * Get the convolution kernel.
     *
     * @return the kernel
     * @see #setKernel
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Set the action to perfomr for pixels off the image edges.
     *
     * @param edgeAction the action
     * @see #getEdgeAction
     */
    public void setEdgeAction(int edgeAction) {
        this.edgeAction = edgeAction;
    }

    /**
     * Get the action to perfomr for pixels off the image edges.
     *
     * @return the action
     * @see #setEdgeAction
     */
    public int getEdgeAction() {
        return edgeAction;
    }

    /**
     * Set whether to convolve the alpha channel.
     *
     * @param useAlpha true to convolve the alpha
     * @see #getUseAlpha
     */
    public void setUseAlpha(boolean useAlpha) {
        alpha = useAlpha;
    }

    /**
     * Get whether to convolve the alpha channel.
     *
     * @return true to convolve the alpha
     * @see #setUseAlpha
     */
    public boolean getUseAlpha() {
        return alpha;
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

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inputPixels = new int[width * height];
        int[] outputPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inputPixels);

        if (premultiplyAlpha) {
            ImageMath.premultiply(inputPixels, 0, inputPixels.length);
        }
        convolve(kernel, inputPixels, outputPixels, width, height, alpha, edgeAction);
        if (premultiplyAlpha) {
            ImageMath.unpremultiply(outputPixels, 0, outputPixels.length);
        }

        setRGB(dst, 0, 0, width, height, outputPixels);
        return dst;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstColorModel) {
        if (dstColorModel == null) {
            dstColorModel = src.getColorModel();
        }
        return new BufferedImage(dstColorModel, dstColorModel.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstColorModel
                .isAlphaPremultiplied(), null);
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override
    public Point2D getPoint2D(Point2D srcPoint, Point2D dstPoint) {
        if (dstPoint == null) {
            dstPoint = new Point2D.Double();
        }
        dstPoint.setLocation(srcPoint.getX(), srcPoint.getY());
        return dstPoint;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    /**
     * Convolve a block of pixels.
     *
     * @param kernel     the kernel
     * @param inputPixels   the input pixels
     * @param outputPixels  the output pixels
     * @param width      the width
     * @param height     the height
     * @param edgeAction what to do at the edges
     */
    public void convolve(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, int edgeAction) {
        convolve(kernel, inputPixels, outputPixels, width, height, true, edgeAction);
    }

    /**
     * Convolve a block of pixels.
     *
     * @param kernel     the kernel
     * @param inputPixels   the input pixels
     * @param outputPixels  the output pixels
     * @param width      the width
     * @param height     the height
     * @param alpha      include alpha channel
     * @param edgeAction what to do at the edges
     */
    public void convolve(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
    	IConvolve convolveProcess;
        if (kernel.getHeight() == 1) {
            convolveProcess = new convolveH();
        } else if (kernel.getWidth() == 1) {
            convolveProcess = new convolveV();
        } else {
            convolveProcess = new convolveHV();
        }
        convolveProcess.convolveProcessing(kernel, inputPixels, outputPixels, width, height, alpha, edgeAction);

    }

    /**
     * Convolve with a 2D kernel.
     *
     * @param kernel     the kernel
     * @param inputPixels   the input pixels
     * @param outputPixels  the output pixels
     * @param width      the width
     * @param height     the height
     * @param alpha      include alpha channel
     * @param edgeAction what to do at the edges
     */
    public void convolveHV(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int cols = kernel.getWidth();
        int halfRows = rows / 2;
        int halfCols = cols / 2;

        pt = createProgressTracker(height);

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
        finishProgressTracker();
    }

    /**
     * Convolve with a kernel consisting of one row.
     *
     * @param kernel     the kernel
     * @param inputPixels   the input pixels
     * @param outputPixels  the output pixels
     * @param width      the width
     * @param height     the height
     * @param alpha      include alpha channel
     * @param edgeAction what to do at the edges
     */
    public static void convolveH(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int halfCols = cols / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                int moffset = halfCols;
                for (int col = -halfCols; col <= halfCols; col++) {
                    float f = matrix[moffset + col];

                    if (f != 0) {
                        int eachRoundX = x + col;
                        if (eachRoundX < 0) {
                            if (edgeAction == CLAMP_EDGES) {
                                eachRoundX = 0;
                            } else if (edgeAction == WRAP_EDGES) {
                                eachRoundX = (x + width) % width;
                            }
                        } else if (eachRoundX >= width) {
                            if (edgeAction == CLAMP_EDGES) {
                                eachRoundX = width - 1;
                            } else if (edgeAction == WRAP_EDGES) {
                                eachRoundX = (x + width) % width;
                            }
                        }
                        int rgb = inputPixels[ioffset + eachRoundX];
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

    /**
     * Convolve with a kernel consisting of one column.
     *
     * @param kernel     the kernel
     * @param inputPixels   the input pixels
     * @param outputPixels  the output pixels
     * @param width      the width
     * @param height     the height
     * @param alpha      include alpha channel
     * @param edgeAction what to do at the edges
     */
    public static void convolveV(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
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

    @Override
    public String toString() {
        return "Blur/Convolve...";
    }
}
