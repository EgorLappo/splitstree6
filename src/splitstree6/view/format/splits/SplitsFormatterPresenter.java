/*
 * SplitsFormatterPresenter.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package splitstree6.view.format.splits;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import jloda.fx.selection.SelectionModel;
import jloda.fx.undo.UndoManager;
import jloda.fx.undo.UndoableRedoableCommandList;
import jloda.graph.Node;
import jloda.util.ProgramProperties;
import splitstree6.view.splits.layout.RotateSplit;
import splitstree6.view.splits.viewer.LoopView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * splits formatter presenter
 * Daniel Huson, 1.2022
 */
public class SplitsFormatterPresenter {
	private final InvalidationListener selectionListener;

	private boolean inUpdatingDefaults = false;

	public SplitsFormatterPresenter(UndoManager undoManager, SplitsFormatterController controller, SelectionModel<Integer> splitSelectionModel,
									Map<Node, Shape> nodeShapeMap, Map<Integer, ArrayList<Shape>> splitShapeMap, ObservableList<LoopView> loopViews) {

		var strokeWidth = new SimpleDoubleProperty(1.0);
		controller.getWidthCBox().getItems().addAll(0.1, 0.5, 1, 2, 3, 4, 5, 6, 8, 10, 20);
		controller.getWidthCBox().valueProperty().addListener((v, o, n) -> {
			if (n != null)
				strokeWidth.set(n.doubleValue());
		});

		strokeWidth.addListener((v, o, n) -> {
			if (!inUpdatingDefaults) {
				if (n != null) {
					if (n.doubleValue() < 0)
						strokeWidth.set(0);
					else {
						var undoList = new UndoableRedoableCommandList("set line width");

						for (var split : splitSelectionModel.getSelectedItems()) {
							if (splitShapeMap.containsKey(split)) {
								for (var shape : splitShapeMap.get(split)) {
									var oldValue = shape.getStrokeWidth();
									if (n.doubleValue() != oldValue) {
										undoList.add(shape.strokeWidthProperty(), oldValue, n);
									}
								}
							}
						}
						if (undoList.size() > 0)
							undoManager.doAndAdd(undoList);

						Platform.runLater(() -> controller.getWidthCBox().setValue(n));
					}
				}
			}
		});

		controller.getColorPicker().setOnAction(e -> {
			if (!inUpdatingDefaults) {
				var undoList = new UndoableRedoableCommandList("set line color");
				var color = controller.getColorPicker().getValue();
				for (var split : splitSelectionModel.getSelectedItems()) {
					if (splitShapeMap.containsKey(split)) {
						for (var shape : splitShapeMap.get(split)) {
							var oldColor = shape.getStroke();
							if (!color.equals(oldColor)) {
								var hasGraphEdgeStyleClass = shape.getStyleClass().contains("graph-edge");
								undoList.add(() -> {
									if (hasGraphEdgeStyleClass)
										shape.getStyleClass().add("graph-edge");
									shape.setStroke(oldColor);

								}, () -> {
									if (hasGraphEdgeStyleClass)
										shape.getStyleClass().remove("graph-edge");
									shape.setStroke(color);
								});
							}
						}
					}
				}
				if (undoList.size() > 0)
					undoManager.doAndAdd(undoList);
			}
		});

		selectionListener = e -> {
			inUpdatingDefaults = true;
			try {
				controller.getWidthCBox().setDisable(splitSelectionModel.size() == 0);
				controller.getColorPicker().setDisable(splitSelectionModel.size() == 0);

				var widths = new HashSet<Double>();
				var colors = new HashSet<Paint>();
				for (var split : splitSelectionModel.getSelectedItems()) {
					if (splitShapeMap.containsKey(split)) {
						for (var shape : splitShapeMap.get(split)) {
							if (shape.getUserData() instanceof Double width) // temporarily store width in user data when user is hovering over edge
								widths.add(width);
							else
								widths.add(shape.getStrokeWidth());
							colors.add(shape.getStroke());
						}
					}
				}
				var width = (widths.size() == 1 ? widths.iterator().next() : null);
				controller.getWidthCBox().setValue(width);
				strokeWidth.setValue(null);
				controller.getColorPicker().setValue(colors.size() == 1 ? (Color) colors.iterator().next() : null);
			} finally {
				inUpdatingDefaults = false;
			}
		};

		controller.getRotateLeftButton().setOnAction(e -> {
			var splits = new ArrayList<>(splitSelectionModel.getSelectedItems());
			undoManager.doAndAdd("rotate splits", () -> RotateSplit.apply(splits, -5, nodeShapeMap), () -> RotateSplit.apply(splits, 5, nodeShapeMap));
		});
		controller.getRotateLeftButton().disableProperty().bind(splitSelectionModel.sizeProperty().isEqualTo(0));

		controller.getRotateRightButton().setOnAction(e -> {
			var splits = new ArrayList<>(splitSelectionModel.getSelectedItems());
			undoManager.doAndAdd("rotate splits", () -> RotateSplit.apply(splits, 5, nodeShapeMap), () -> RotateSplit.apply(splits, -5, nodeShapeMap));
		});
		controller.getRotateRightButton().disableProperty().bind(splitSelectionModel.sizeProperty().isEqualTo(0));

		controller.getOutlineColorPicker().valueProperty().addListener((c, o, n) -> {
			undoManager.doAndAdd("set outline fill color", () -> loopViews.forEach(lv -> lv.setFill(o)), () -> loopViews.forEach(lv -> lv.setFill(n)));
			ProgramProperties.put("OutlineFillColor", n);
		});
		controller.getOutlineColorPicker().disableProperty().bind(Bindings.isEmpty(loopViews));
        loopViews.addListener((InvalidationListener) e -> controller.getOutlineColorPicker().setValue(ProgramProperties.get("OutlineFillColor", Color.SILVER)));

		//selectionModel.getSelectedItems().addListener(selectionListener);
		splitSelectionModel.getSelectedItems().addListener(new WeakInvalidationListener(selectionListener));
		selectionListener.invalidated(null);

	}
}