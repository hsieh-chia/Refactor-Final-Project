package com.jhlabs.image;

public class CircleUpGradientBlend implements GradientBlend{
	@Override
	public float GradientBlendOperation(float t){
		float temp = t - 1;
        return (float) Math.sqrt(1 - temp * temp);
	}
}