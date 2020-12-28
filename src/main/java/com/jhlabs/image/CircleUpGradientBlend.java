package com.jhlabs.image;

import java.awt.Color;

public class CircleUpGradientBlend implements GradientBlend{
	@Override
	public int GradientBlendOperation(int t){
		int temp = t - 1;
        return (float) Math.sqrt(1 - temp * temp);
	}
}