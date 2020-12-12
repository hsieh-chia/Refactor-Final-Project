package com.jhlabs.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

import pixelitor.ThreadPool;

public class SmearFilterLines extends SmearFilter{
	
	private Random randomGenerator;
	
	public SmearFilterLines(String filterName) {
		// TODO Auto-generated constructor stub
		super(filterName);
	}
	
	protected void render(int width, int height, int[] inPixels, int[] outPixels) {
        float sin = (float) Math.sin(getAngle());
        float cos = (float) Math.cos(getAngle());

        int numShapes = (int) (2 * getDensity() * width * height / 2);

        int stride = numShapes / 100 + 1;
        int estimatedWorkUnits = numShapes / stride;
        pt = createProgressTracker(estimatedWorkUnits);

        List<Future<?>> futures = new ArrayList<>(estimatedWorkUnits + 1);
        for (int i = 0; i < numShapes; i = i + stride) {
            Runnable r = () -> {
                for (int j = 0; j < stride; j++) {
                    renderOneLine(width, height, inPixels, outPixels, sin, cos);
                }
            };
            futures.add(ThreadPool.submit(r));
        }
        ThreadPool.waitFor(futures, pt);
    }
	
	private void renderOneLine(int width, int height, int[] inPixels, int[] outPixels, float sin, float cos) {
        int sx = (randomGenerator.nextInt() & 0x7fffffff) % width;
        int sy = (randomGenerator.nextInt() & 0x7fffffff) % height;
        int rgb = inPixels[sy * width + sx];
        int length = (randomGenerator.nextInt() & 0x7fffffff) % getDistance();
        int dx = (int) (length * cos);
        int dy = (int) (length * sin);

        int x0 = sx - dx;
        int y0 = sy - dy;
        int x1 = sx + dx;
        int y1 = sy + dy;
        int x, y, d, incrE, incrNE, ddx, ddy;

        if (x1 < x0) {
            ddx = -1;
        } else {
            ddx = 1;
        }
        if (y1 < y0) {
            ddy = -1;
        } else {
            ddy = 1;
        }
        dx = x1 - x0;
        dy = y1 - y0;
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        x = x0;
        y = y0;

        if (x < width && x >= 0 && y < height && y >= 0) {
            int offset = y * width + x;
            int rgb2 = outPixels[offset];
            outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
        }
        if (dx > dy) {
            d = 2 * dy - dx;
            incrE = 2 * dy;
            incrNE = 2 * (dy - dx);

            while (x != x1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    y += ddy;
                }
                x += ddx;
                if (x < width && x >= 0 && y < height && y >= 0) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
                }
            }
        } else {
            d = 2 * dx - dy;
            incrE = 2 * dx;
            incrNE = 2 * (dx - dy);

            while (y != y1) {
                if (d <= 0) {
                    d += incrE;
                } else {
                    d += incrNE;
                    x += ddx;
                }
                y += ddy;
                if (x < width && x >= 0 && y < height && y >= 0) {
                    int offset = y * width + x;
                    int rgb2 = outPixels[offset];
                    outPixels[offset] = ImageMath.mixColors(getMix(), rgb2, rgb);
                }
            }
        }
    }

}
