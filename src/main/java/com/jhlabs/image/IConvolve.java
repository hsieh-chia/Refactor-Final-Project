package com.jhlabs.image;

import java.awt.image.Kernel;

public interface IConvolve {
	public static final int CLAMP_EDGES = 1;
	public static final int WRAP_EDGES = 2;

    public void convolveProcessing(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction);
}