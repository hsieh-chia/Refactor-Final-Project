/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters;

import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * "Mystic Rose" (complete graph) shape filter.
 */
public class MysticRose extends ShapeFilter {
    public static final String NAME = "Mystic Rose";

    private final RangeParam nrPoints = new RangeParam("Number of Points", 3, 10, 43);
    private final RangeParam radius = new RangeParam(GUIText.RADIUS, 1, 500, 1000);
    private final RangeParam rotate = new RangeParam("Rotate", 0, 0, 100);

    public MysticRose() {
        addParamsToFront(
            nrPoints,
            rotate,
            radius.withAdjustedRange(0.6)
        );

        helpURL = "https://en.wikipedia.org/wiki/Complete_graph";
    }

    @Override
    protected Path2D createShape(int width, int height) {
        Point2D[] points = calcPoints(width, height);

        Path2D shape = new Path2D.Double();
        for (int i = 0; i < points.length; i++) {
            for (int j = 0; j < points.length; j++) {
                if (i > j) { // draw only in one direction
                    shape.moveTo(points[i].getX(), points[i].getY());
                    shape.lineTo(points[j].getX(), points[j].getY());
                }
            }
        }
        return shape;
    }

    private Point2D[] calcPoints(int width, int height) {
        int numPoints = nrPoints.getValue();
        Point2D[] points = new Point2D[numPoints];
        double r = radius.getValueAsDouble();
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        double angle = 2 * Math.PI / numPoints;
        double startAngle = angle * rotate.getPercentageValD();
        for (int i = 0; i < points.length; i++) {
            double theta = startAngle + i * angle;
            points[i] = new Point2D.Double(
                cx + r * Math.cos(theta),
                cy + r * Math.sin(theta));
        }
        return points;
    }

    @Override
    protected float getGradientRadius(float cx, float cy) {
        return radius.getValueAsFloat();
    }
}