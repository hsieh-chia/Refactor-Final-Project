package com.jhlabs.image;

import java.awt.Color;

public class SplineGradientBlend implements GradientBlend{
	@Override
	public int GradientBlendOperation(int t){
		return ImageMath.smoothStep(0.15f, 0.85f, t);
	}
}