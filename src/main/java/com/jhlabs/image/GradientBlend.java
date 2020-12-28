package com.jhlabs.image;

import java.awt.Color;

public interface GradientBlend {
	public static final int LINEAR = 0x10;
	public static final int SPLINE = 0x20;
	public static final int CIRCLE_UP = 0x30;
	public static final int CIRCLE_DOWN = 0x40;
	public static final int CONSTANT = 0x50;


    public int GradientBlendOperation(int t);
}