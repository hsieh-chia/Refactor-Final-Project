package com.jhlabs.image;

import java.awt.Color;

public class LinearGradientBlend implements GradientBlend{
	@Override
	public int GradientBlendOperation(int t){
		return t;
	}
}