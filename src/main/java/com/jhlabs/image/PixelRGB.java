package com.jhlabs.image;

public class PixelRGB {
	private int red;
	private int green;
	private int blue;
	private int a;
	
	public int getR() {
		return red;
	}
	
	public int getG() {
		return green;
	}
	
	public int getB() {
		return blue;
	}
	
	public int getA() {
		return a;
	}
	
	public PixelRGB(int rgb) {
		a = (rgb >> 24) & 0xff;
		red = (rgb >> 16) & 0xff;
        green = (rgb >> 8) & 0xff;
        blue = rgb & 0xff;
	}
	
}
