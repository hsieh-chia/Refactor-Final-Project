package com.jhlabs.image;

import java.util.Random;
import java.util.concurrent.Future;
import pixelitor.ThreadPool;

abstract public class SmearFilterShapes extends SmearFilter{
	
	private Random randomGenerator = new Random();
	
	public SmearFilterShapes (String filterName) {
		// TODO Auto-generated constructor stub
		super(filterName);
	}
	
	protected void render(int width, int height, int[] inPixels, int[] outPixels) {
        int radius = getDistance() + 1;
        int radius2 = radius * radius;

        int numShapes = (int) (2 * getDensity() * width * height / radius);

        pt = createProgressTracker(numShapes);
        Future<?>[] futures = new Future[numShapes];

        for (int i = 0; i < numShapes; i++) {
            Runnable r = () -> renderOneShape(width, height, inPixels, outPixels, radius, radius2);
            futures[i] = ThreadPool.submit(r);
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void renderOneShape(int width, int height, int[] inPixels, int[] outPixels, int radius, int radius2) {
        int sx = (randomGenerator.nextInt() & 0x7fffffff) % width;
        int sy = (randomGenerator.nextInt() & 0x7fffffff) % height;
        int rgb = inPixels[sy * width + sx];
        int minSx = sx - radius;
        int maxSx = sx + radius + 1;
        int minSy = sy - radius;
        int maxSy = sy + radius + 1;

        if (minSx < 0) {
            minSx = 0;
        }
        if (minSy < 0) {
            minSy = 0;
        }
        if (maxSx > width) {
            maxSx = width;
        }
        if (maxSy > height) {
            maxSy = height;
        }
        
        makeshape(width, outPixels, radius2, sx, sy, rgb, minSx, maxSx, minSy, maxSy);

//        switch (shape) {
//            case CIRCLES -> makeCircle(width, outPixels, radius2, sx, sy, rgb, minSx, maxSx, minSy, maxSy);
//            case SQUARES -> makeSquare(width, outPixels, rgb, minSx, maxSx, minSy, maxSy);
//            case DIAMONDS -> makeDiamond(width, outPixels, radius, sx, sy, rgb, minSx, maxSx, minSy, maxSy);
//        }
    }
    abstract protected void makeshape(int width, int[] outPixels, int radius2, int sx, int sy, int rgb, int minSx, int maxSx, int minSy, int maxSy);

}
