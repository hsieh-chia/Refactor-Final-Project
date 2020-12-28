package com.jhlabs.image;

public class CircleDownGradientBlend implements GradientBlend{
	@Override
	public float GradientBlendOperation(float t){
		return 1 - (float) Math.sqrt(1 - t * t);
	}
}