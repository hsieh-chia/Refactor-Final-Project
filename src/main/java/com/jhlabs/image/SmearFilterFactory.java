package com.jhlabs.image;

public class SmearFilterFactory {
	
	public SmearFilter getFilter(int type, String NAME) {
		SmearFilter smearfilter = switch(type) {
	    	case SmearFilter.LINES -> new SmearFilterLines(NAME);
	    	case SmearFilter.CROSSES -> new SmearFilterCrosses(NAME);
	    	case SmearFilter.CIRCLES -> new SmearFilterShapesCircles(NAME);
	    	case SmearFilter.SQUARES -> new SmearFilterSquares(NAME);
	    	case SmearFilter.DIAMONDS -> new SmearFilterShapesDiamonds(NAME);
	    	default -> new SmearFilterLines(NAME);
		};
		return smearfilter;
	}
}