package com.jhlabs.image;

public class GradientBlendFactory{
	private GradientBlend gradientblend;
	public static final int LINEAR = 0x10;
	public static final int SPLINE = 0x20;
	public static final int CIRCLE_UP = 0x30;
	public static final int CIRCLE_DOWN = 0x40;
	public static final int CONSTANT = 0x50;
	
	public float GradientBlendOperation(float t,int blend){
		switch (blend) {
	        case CONSTANT:
	            gradientblend = new ConstantGradientBlend();
	            break;
	        case LINEAR:
	        	gradientblend = new LinearGradientBlend();
	            break;
	        case SPLINE:
	            gradientblend = new SplineGradientBlend();
	            break;
	        case CIRCLE_UP:
	            gradientblend = new CircleUpGradientBlend();
	            break;
	        case CIRCLE_DOWN:
	            gradientblend = new CircleDownGradientBlend();
	            break;
	    }
	    return gradientblend.GradientBlendOperation(t);
	}
}
// GradientBlendFactory gradientblendfactory = new GradientBlendFactory();
// t=gradientblendfactory.GradientBlendOperation(t,blend);