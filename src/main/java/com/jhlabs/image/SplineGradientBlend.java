package com.jhlabs.image;

public class SplineGradientBlend implements GradientBlend{
	@Override
	public float GradientBlendOperation(float t){
		return ImageMath.smoothStep(0.15f, 0.85f, t);
	}
}