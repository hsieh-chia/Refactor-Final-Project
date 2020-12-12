package com.jhlabs.image;

public class SmearFilterShapesCircles extends SmearFilterShapes {

	public SmearFilterShapesCircles(String Filename) {
		// TODO Auto-generated constructor stub
		super(Filename);
	}
	protected void makeshape(int width, int[] outPixels, int radius2, int sx, int sy, int rgb, int minSx, int maxSx, int minSy, int maxSy) {
        for (int x = minSx; x < maxSx; x++) {
            for (int y = minSy; y < maxSy; y++) {
                int dsx = x - sx;
                int dsy = y - sy;
                boolean isInside = dsx * dsx + dsy * dsy <= radius2;
                if (isInside) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
                }
            }
        }
    }
}
