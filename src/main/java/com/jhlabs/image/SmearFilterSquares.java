package com.jhlabs.image;

import java.util.Random;
import java.util.concurrent.Future;

import pixelitor.ThreadPool;

public class SmearFilterSquares extends SmearFilter{
	
	private Random randomGenerator;

	public SmearFilterSquares(String Filename) {
		// TODO Auto-generated constructor stub
		super(Filename);
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
       makeSquare(width, outPixels, rgb, minSx, maxSx, minSy, maxSy);
   }
   
   private void makeSquare(int width, int[] outPixels, int rgb, int minSx, int maxSx, int minSy, int maxSy) {
       for (int x = minSx; x < maxSx; x++) {
           for (int y = minSy; y < maxSy; y++) {
               int offset = y * width + x;
               int rgb2 = outPixels[offset];
               outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
           }
       }
   }

}
