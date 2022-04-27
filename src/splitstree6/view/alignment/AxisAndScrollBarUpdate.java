/*
 *  AxisAndScrollBarUpdate.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree6.view.alignment;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import splitstree6.data.CharactersBlock;

import java.util.BitSet;

import static splitstree6.view.alignment.DrawAlignment.*;

/**
 * update axis and horizontal scroll bar
 * Daniel Huson, 4.2022
 */
public class AxisAndScrollBarUpdate {

	public static void update(NumberAxis axis, ScrollBar scrollBar, double canvasWidth, double boxWidth, int nChar, ObjectProperty<BitSet> selectedSites) {
		if (nChar < 1) {
			scrollBar.setVisible(false);
			axis.setVisible(false);
		} else {
			scrollBar.setVisible(true);
			axis.setVisible(true);

			var numberOnCanvas = (canvasWidth - 10) / boxWidth;
			scrollBar.setMin(1);
			scrollBar.setMax(nChar - numberOnCanvas + 1);
			scrollBar.setVisibleAmount(numberOnCanvas);

			axis.setLowerBound(Math.max(1, Math.floor(scrollBar.getValue())));
			axis.setUpperBound(Math.round(scrollBar.getValue() + numberOnCanvas));

			axis.setOnMouseClicked(event -> {
				var pointInScene = new Point2D(event.getSceneX(), event.getSceneY());
				double xPosInAxis = axis.sceneToLocal(new Point2D(pointInScene.getX(), 0)).getX();
				var site = (int) Math.round(axis.getValueForDisplay(xPosInAxis).doubleValue());
				if (site >= 1 && site <= nChar) {
					var bits = new BitSet();
					bits.or(selectedSites.get());
					if (event.isShiftDown()) {
						if (bits.cardinality() == 0 || bits.cardinality() == 1 && bits.get(site)) {
							bits.set(site, !bits.get(site));
						} else {
							var left = site;
							while (left >= 1 && !bits.get(left))
								left--;
							var right = site;
							while (right <= nChar && !bits.get(right))
								right++;
							if (left >= 1) {
								for (var s = left; s <= site; s++)
									bits.set(s);
							}
							if (right <= nChar) {
								for (var s = site; s <= right; s++)
									bits.set(s);
							}
						}
					} else if (event.isShortcutDown()) {
						bits.set(site, !bits.get(site));
					} else {
						bits.clear();
						bits.set(site);
					}
					if (!bits.equals(selectedSites.get()))
						selectedSites.set(bits);

				}
			});
			if (numberOnCanvas < 100) {
				axis.setTickUnit(1);
				axis.setMinorTickVisible(false);
			} else if (numberOnCanvas < 500) {
				axis.setTickUnit(10);
				axis.setMinorTickVisible(true);
			} else if (numberOnCanvas < 5000) {
				axis.setTickUnit(100);
				axis.setMinorTickVisible(true);
			} else {
				axis.setTickUnit(1000);
				axis.setMinorTickVisible(true);
			}
		}
	}


	public static void updateSelection(Pane selectionPane, NumberAxis axis, CharactersBlock inputCharacters, BitSet activeSites, BitSet selectedSites) {
		selectionPane.setVisible(axis.isVisible());
		selectionPane.getChildren().clear();

		var axisStartOffset = 2;
		var boxWidth = (axis.getWidth()) / (axis.getUpperBound() - axis.getLowerBound());

		var left = Math.max(1, (int) axis.getLowerBound() - 1);
		var right = Math.min(inputCharacters.getNchar(), axis.getUpperBound() - 1);

		for (var site = left; site <= right; site++) {
			var inactive = !activeSites.get(site);
			var selected = selectedSites.get(site);
			if (selected || inactive) {
				var x = (site - axis.getLowerBound()) * boxWidth + axisStartOffset;
				var rectangle = new Rectangle(x, selectionPane.getHeight() - 4, Math.max(0.5, boxWidth), 8);
				rectangle.setStrokeWidth(1);
				rectangle.setStroke(selected ? SELECTION_STROKE : INACTIVE_STROKE);
				rectangle.setFill(inactive ? INACTIVE_FILL : SELECTION_FILL);
				selectionPane.getChildren().add(rectangle);
			}
		}
	}
}
