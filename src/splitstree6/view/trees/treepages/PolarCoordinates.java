/*
 *  PolarCoordinates.java Copyright (C) 2021 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package splitstree6.view.trees.treepages;

import javafx.geometry.Point2D;
import jloda.fx.util.GeometryUtilsFX;

/**
 * polar coordinates
 * Daniel Huson, 11.2021
 */
public record PolarCoordinates(double radius, double angle) {
	public Point2D toCartesian() {
		return new Point2D(radius * Math.cos(GeometryUtilsFX.deg2rad(angle)), radius * Math.sin(GeometryUtilsFX.deg2rad(angle)));
	}

	public static PolarCoordinates fromCartesian(Point2D point) {
		return new PolarCoordinates(point.magnitude(), point.angle(1, 0));
	}
}