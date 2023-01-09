/*
 *  InteractionSetup.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.view.network;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jloda.fx.control.RichTextLabel;
import jloda.fx.label.EditLabelDialog;
import jloda.fx.selection.SelectionModel;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.DraggableUtils;
import jloda.fx.util.SelectionEffectBlue;
import jloda.graph.Node;
import jloda.phylo.PhyloGraph;
import splitstree6.data.parts.Taxon;
import splitstree6.layout.tree.LabeledNodeShape;

import java.util.Map;
import java.util.function.Function;

/**
 * network mouse interaction setup
 * Daniel Huson, 4.2022
 */
public class InteractionSetup {
	private static boolean nodeShapeOrLabelEntered;
	private static boolean edgeShapeEntered;
	private static double mouseDownX;
	private static double mouseDownY;

	private final Stage stage;
	private final UndoManager undoManager;
	private final SelectionModel<Taxon> taxonSelectionModel;

	private InvalidationListener taxonSelectionInvalidationListener;

	/**
	 * constructor
	 */
	public InteractionSetup(Stage stage, Pane pane, UndoManager undoManager, SelectionModel<Taxon> taxonSelectionModel) {
		this.stage = stage;
		this.undoManager = undoManager;
		this.taxonSelectionModel = taxonSelectionModel;

		pane.setOnMouseClicked(e -> {
			if (e.isStillSincePress() && !e.isShiftDown()) {
				Platform.runLater(taxonSelectionModel::clearSelection);
				e.consume();
			}
		});
	}

	/**
	 * setup network mouse interaction
	 */
	public void apply(Map<Integer, RichTextLabel> taxonLabelMap, Map<Node, LabeledNodeShape> nodeShapeMap, Function<Integer, Taxon> idTaxonMap, Function<Taxon, Integer> taxonIdMap) {
		for (var shape : nodeShapeMap.values()) {
			DraggableUtils.setupDragMouseTranslate(shape);
			shape.setOnMouseEntered(e -> {
				if (!e.isStillSincePress() && !nodeShapeOrLabelEntered) {
					nodeShapeOrLabelEntered = true;
					shape.setScaleX(1.2 * shape.getScaleX());
					shape.setScaleY(1.2 * shape.getScaleY());
					e.consume();
				}
			});
			shape.setOnMouseExited(e -> {
				if (nodeShapeOrLabelEntered) {
					shape.setScaleX(shape.getScaleX() / 1.2);
					shape.setScaleY(shape.getScaleY() / 1.2);
					nodeShapeOrLabelEntered = false;
					e.consume();
				}
			});
		}

		var graphOptional = nodeShapeMap.keySet().stream().filter(v -> v.getOwner() != null).map(v -> (PhyloGraph) v.getOwner()).findAny();

		if (graphOptional.isPresent()) {
			var graph = graphOptional.get();

			for (var id : taxonLabelMap.keySet()) {
				try {
					var label = taxonLabelMap.get(id);
					if (label != null) {
						var v = graph.getTaxon2Node(id);
						var shape = nodeShapeMap.get(v);
						var taxon = idTaxonMap.apply(id);
						if (taxon != null && shape != null) {
							shape.setOnContextMenuRequested(m -> showContextMenu(m, stage, undoManager, label));
							label.setOnContextMenuRequested(m -> showContextMenu(m, stage, undoManager, label));

							shape.setOnMouseEntered(e -> {
								if (!e.isStillSincePress() && !nodeShapeOrLabelEntered) {
									nodeShapeOrLabelEntered = true;
									shape.setScaleX(1.2 * shape.getScaleX());
									shape.setScaleY(1.2 * shape.getScaleY());
									label.setScaleX(1.1 * label.getScaleX());
									label.setScaleY(1.1 * label.getScaleY());
									e.consume();
								}
							});
							shape.setOnMouseExited(e -> {
								if (nodeShapeOrLabelEntered) {
									shape.setScaleX(shape.getScaleX() / 1.2);
									shape.setScaleY(shape.getScaleY() / 1.2);
									label.setScaleX(label.getScaleX() / 1.1);
									label.setScaleY(label.getScaleY() / 1.1);
									nodeShapeOrLabelEntered = false;
									e.consume();
								}
							});

							final EventHandler<MouseEvent> mouseClickedHandler = e -> {
								if (e.isStillSincePress()) {
									if (!e.isShiftDown())
										taxonSelectionModel.clearSelection();
									taxonSelectionModel.toggleSelection(taxon);
									e.consume();
								}
							};
							shape.setOnMouseClicked(mouseClickedHandler);
							label.setOnMouseClicked(mouseClickedHandler);

							label.setOnMouseEntered(shape.getOnMouseEntered());
							label.setOnMouseExited(shape.getOnMouseExited());

							label.setOnMousePressed(e -> {
								if (taxonSelectionModel.isSelected(taxon)) {
									mouseDownX = e.getScreenX();
									mouseDownY = e.getScreenY();
									e.consume();
								}
							});

							label.setOnMouseDragged(e -> {
								if (taxonSelectionModel.isSelected(taxon)) {
									for (var wTaxon : taxonSelectionModel.getSelectedItems()) {
										var wLabel = taxonLabelMap.get(taxonIdMap.apply(wTaxon));

										var dx = e.getScreenX() - mouseDownX;
										var dy = e.getScreenY() - mouseDownY;
										wLabel.setLayoutX(wLabel.getLayoutX() + dx);
										wLabel.setLayoutY(wLabel.getLayoutY() + dy);
									}
									mouseDownX = e.getScreenX();
									mouseDownY = e.getScreenY();
									e.consume();
								}
							});
						}
					}
				} catch (Exception ignored) {
				}
			}
			taxonSelectionInvalidationListener = e -> {
				for (var t : taxonLabelMap.keySet()) {
					var taxon = idTaxonMap.apply(t);
					if (taxon != null) {
						var label = taxonLabelMap.get(t);
						label.setEffect(taxonSelectionModel.isSelected(taxon) ? SelectionEffectBlue.getInstance() : null);
						var shape = nodeShapeMap.get(graph.getTaxon2Node(t));
						if (shape != null)
							shape.setEffect(taxonSelectionModel.isSelected(taxon) ? SelectionEffectBlue.getInstance() : null);
					}
				}
			};
			taxonSelectionModel.getSelectedItems().addListener(new WeakInvalidationListener(taxonSelectionInvalidationListener));
			taxonSelectionInvalidationListener.invalidated(null);
		}
	}

	private static void showContextMenu(ContextMenuEvent event, Stage stage, UndoManager undoManager, RichTextLabel label) {
		var editLabelMenuItem = new MenuItem("Edit Label...");
		editLabelMenuItem.setOnAction(e -> {
			var oldText = label.getText();
			var editLabelDialog = new EditLabelDialog(stage, label);
			var result = editLabelDialog.showAndWait();
			if (result.isPresent() && !result.get().equals(oldText)) {
				undoManager.doAndAdd("Edit Label", () -> label.setText(oldText), () -> label.setText(result.get()));
			}
		});
		var menu = new ContextMenu();
		menu.getItems().add(editLabelMenuItem);
		menu.show(label, event.getScreenX(), event.getScreenY());
	}
}
