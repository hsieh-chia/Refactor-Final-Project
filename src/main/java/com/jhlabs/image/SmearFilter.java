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
import java.util.Random;


public abstract class SmearFilter extends WholeImageFilter {
    public static final int CROSSES = 0;
    public static final int LINES = 1;
    public static final int CIRCLES = 2;
    public static final int SQUARES = 3;
    public static final int DIAMONDS = 4;

    //    private Colormap colormap = new LinearColormap();
    private float angle = 0;
    private float density = 0.5f;
    private float scatter = 0.0f;
    private int distance = 8;
    private Random randomGenerator;
    //    private long seed = 567;
    private int shape = LINES;
    private float mix = 0.5f;

    public SmearFilter(String filterName) {
        super(filterName);
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public int getShape() {
        return shape;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getDensity() {
        return density;
    }

    public void setScatter(float scatter) {
        this.scatter = scatter;
    }

    public float getScatter() {
        return scatter;
    }

    /**
     * Specifies the angle of the texture.
     *
     * @param angle the angle of the texture.
     * @angle
     * @see #getAngle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     * Returns the angle of the texture.
     *
     * @return the angle of the texture.
     * @see #setAngle
     */
    public float getAngle() {
        return angle;
    }

    public void setMix(float mix) {
        this.mix = mix;
    }

    public float getMix() {
        return mix;
    }

    @Override
    protected int[] filterPixels(int width, int height, int[] inPixels, Rectangle transformedSpace) {
        int[] outPixels = new int[width * height];

        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                outPixels[i] = inPixels[i];
                i++;
            }
        }
        
        render(width, height, inPixels, outPixels);
        finishProgressTracker();

        return outPixels;
    }
    
    abstract protected void render(int width, int height, int[] inPixels, int[] outPixels);
    


    @Override
    public String toString() {
        return "Effects/Smear...";
    }

    public void setRandomGenerator(Random randomGenerator) {
        this.randomGenerator = randomGenerator;
    }
}
