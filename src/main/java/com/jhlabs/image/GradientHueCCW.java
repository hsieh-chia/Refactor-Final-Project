package com.jhlabs.image;

public class GradientHueCCW extends GradientHSB{

	public GradientHueCCW(int rgb_1, int rgb_2, float temp_t) {
		super(rgb_1, rgb_2, temp_t);
		// TODO Auto-generated constructor stub
	}

	@Override
	int getmap() {
		// TODO Auto-generated method stub
		if (hsb1[0] <= hsb2[1]) {
            hsb1[0] += 1.0f;
        }
		return combineHSB();
	}

}
