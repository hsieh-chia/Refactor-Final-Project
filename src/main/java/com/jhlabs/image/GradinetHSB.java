package com.jhlabs.image;

import java.awt.Color;

abstract class GradientHSB{
	
	protected int rgb1, rgb2;
	protected float[] hsb1;
	protected float[] hsb2;
	protected float t;
	
	public GradientHSB(int rgb_1, int rgb_2, float temp_t){
		rgb1 = rgb_1;
		rgb2 = rgb_2;
		t = temp_t;

		hsb1 = Color.RGBtoHSB((rgb1 >> 16) & 0xff, (rgb1 >> 8) & 0xff, rgb1 & 0xff, null);
		hsb2 = Color.RGBtoHSB((rgb2 >> 16) & 0xff, (rgb2 >> 8) & 0xff, rgb2 & 0xff, null);
	}
	
	abstract int getmap();
	
	public int combineHSB(){
		float h = ImageMath.lerp(t, hsb1[0], hsb2[0]) % ImageMath.TWO_PI;
        float s = ImageMath.lerp(t, hsb1[1], hsb2[1]);
        float b = ImageMath.lerp(t, hsb1[2], hsb2[2]);
        return  0xff000000 | Color.HSBtoRGB(h, s, b);//FIXME-alpha
	}
}