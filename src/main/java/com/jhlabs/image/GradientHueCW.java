package com.jhlabs.image;

public class GradientHueCW extends GradientHSB{
	
	public GradientHueCW(int rgb_1, int rgb_2, float temp_t) {
		super(rgb_1, rgb_2, temp_t);
		// TODO Auto-generated constructor stub
	}

	protected int getmap() {
		if (hsb2[0] <= hsb1[0]) {
            hsb2[0] += 1.0f;
        }
		return combineHSB();
	}
}
