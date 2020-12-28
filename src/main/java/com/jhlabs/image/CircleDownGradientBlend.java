package com.jhlabs.image;

import java.awt.Color;

public class CircleDownGradientBlend implements GradientBlend{
	@Override
	public int GradientBlendOperation(int t){
		return 1 - (float) Math.sqrt(1 - t * t);
	}
}