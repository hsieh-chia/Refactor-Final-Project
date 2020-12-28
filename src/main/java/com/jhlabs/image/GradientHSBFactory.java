package com.jhlabs.image;

public class GradientHSBFactory {
	/**
     * Interpolate in RGB space.
     */
    public static final int RGB = 0x00;

    /**
     * Interpolate hue clockwise.
     */
    public static final int HUE_CW = 0x01;

    /**
     * Interpolate hue counter clockwise.
     */
    public static final int HUE_CCW = 0x02;
    
    private int rgb1, rgb2;
    private float t;
    public GradientHSBFactory(int rgb_1, int rgb_2, float temp_t) {
    	rgb1 = rgb_1;
    	rgb2 = rgb_2;
    	t = temp_t;
    }
    
    public GradientHSB getHSBfunction(int type) {
    	GradientHSB hsb = switch(type) {
    		case GradientHSBFactory.RGB -> new GradientRGB(rgb1, rgb2, t);
    		case GradientHSBFactory.HUE_CW -> new GradientHueCW(rgb1, rgb2, t);
    		case GradientHSBFactory.HUE_CCW -> new GradientHueCCW(rgb1, rgb2, t);
    		default -> new GradientRGB(rgb1, rgb2, t);
    	};
    	
    	return hsb;
    }
	

}
