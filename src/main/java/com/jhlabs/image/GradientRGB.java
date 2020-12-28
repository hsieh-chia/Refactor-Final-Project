package com.jhlabs.image;

public class GradientRGB extends GradientHSB{

	public GradientRGB(int rgb_1, int rgb_2, float temp_t) {
		super(rgb_1, rgb_2, temp_t);
		// TODO Auto-generated constructor stub
	}

	@Override
	int getmap() {
		// TODO Auto-generated method stub
		return ImageMath.mixColors(t, rgb1, rgb2);
	}

}
