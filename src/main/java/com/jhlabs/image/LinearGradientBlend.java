package com.jhlabs.image;

public class LinearGradientBlend implements GradientBlend{
	@Override
	public float GradientBlendOperation(float t){
		return t;
	}
}