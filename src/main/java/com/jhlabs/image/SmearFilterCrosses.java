package com.jhlabs.image;

import java.util.Random;

public class SmearFilterCrosses extends SmearFilter{
	
	private Random randomGenerator;
	
	public SmearFilterCrosses(String filterName) {
        super(filterName);
    }
	
	protected void render(int width, int height, int[] inPixels, int[] outPixels) {
        int numShapes = (int) (2 * getDensity() * width * height / (getDistance() + 1));

        pt = createProgressTracker(numShapes);

        for (int i = 0; i < numShapes; i++) {
            int x = (randomGenerator.nextInt() & 0x7fffffff) % width;
            int y = (randomGenerator.nextInt() & 0x7fffffff) % height;
//            int length = randomGenerator.nextInt() % distance + 1;
            int length = randomGenerator.nextInt(getDistance()) + 1;
            int rgb = inPixels[y * width + x];
            for (int x1 = x - length; x1 < x + length + 1; x1++) {
                if (x1 >= 0 && x1 < width) {
                    int offset = y * width + x1;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
                }
            }
            for (int y1 = y - length; y1 < y + length + 1; y1++) {
                if (y1 >= 0 && y1 < height) {
                    int offset = y1 * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
                }
            }
            pt.unitDone();
        }
    }

}


