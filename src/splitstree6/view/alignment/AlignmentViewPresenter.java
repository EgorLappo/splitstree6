/*
 *  AlignmentViewPresenter.java Copyright (C) 2022 Daniel H. Huson
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

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import jloda.fx.util.BasicFX;
import jloda.fx.window.MainWindowManager;
import jloda.util.Single;
import splitstree6.data.CharactersBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.data.parts.Taxon;
import splitstree6.tabs.IDisplayTabPresenter;
import splitstree6.window.MainWindow;
import splitstree6.window.MainWindowController;
import splitstree6.workflow.DataNode;

import java.util.ArrayList;

public class AlignmentViewPresenter implements IDisplayTabPresenter {
	private final InvalidationListener invalidationListener;
	private final InvalidationListener updateListener;
	private final InvalidationListener selectionListener;

	private final AlignmentViewController controller;
	private final MainWindowController mainWindowController;


	public AlignmentViewPresenter(MainWindow mainWindow, AlignmentView alignmentView) {
		var workflow = mainWindow.getWorkflow();
		controller = alignmentView.getController();
		mainWindowController = mainWindow.getController();

		controller.getChooseColumnsMenu().setDisable(true); // todo: implement

		controller.getColorSchemeCBox().getItems().addAll(ColorScheme.values());
		controller.getColorSchemeCBox().valueProperty().bindBidirectional(alignmentView.optionColorSchemeProperty());

		controller.getTaxaListView().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


		var inUpdate = new Single<>(false);

		controller.getTaxaListView().getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super Taxon>) e -> {
			if (!inUpdate.get()) {
				try {
					inUpdate.set(true);

					var toAdd = new ArrayList<Taxon>();
					var toRemove = new ArrayList<Taxon>();

					while (e.next()) {
						if (e.getAddedSize() > 0)
							toAdd.addAll(e.getAddedSubList());
						if (e.getRemovedSize() > 0)
							toRemove.addAll(e.getRemoved());
					}
					Platform.runLater(() -> {
						mainWindow.getTaxonSelectionModel().clearSelection(toRemove);
						mainWindow.getTaxonSelectionModel().selectAll(toAdd);
					});
				} finally {
					inUpdate.set(false);
				}
			}
		});

		selectionListener = e -> {
			if (!inUpdate.get()) {
				try {
					inUpdate.set(true);
					controller.getTaxaListView().getSelectionModel().clearSelection();
					for (var t : mainWindow.getTaxonSelectionModel().getSelectedItems()) {
						controller.getTaxaListView().getSelectionModel().select(t);
					}
				} finally {
					inUpdate.set(false);
				}
			}
		};
		mainWindow.getTaxonSelectionModel().getSelectedItems().addListener(new WeakInvalidationListener(selectionListener));

		var workingTaxa = mainWindow.workingTaxaProperty();
		var workingCharactersNode = new SimpleObjectProperty<DataNode<?>>(this, "workingCharactersNode");
		var workingCharacters = new SimpleObjectProperty<CharactersBlock>(this, "workingCharacters");

		updateListener = e -> Platform.runLater(() -> {
			controller.getAxis().setPadding(new Insets(0, 0, 0, alignmentView.getOptionUnitWidth()));
			updateTaxaCellFactory(controller.getTaxaListView(), alignmentView.getOptionUnitHeight());
			updateAxisAndScrollBar(controller.getAxis(), controller.gethScrollBar(), controller.getCanvas().getWidth(),
					alignmentView.getOptionUnitWidth(), workingCharacters.get() != null ? workingCharacters.get().getNchar() : 0);
			updateCanvas(controller.getCanvas(), workingTaxa.get(), workingCharacters.get(), alignmentView.getOptionColorScheme(), alignmentView.getOptionUnitWidth(),
					alignmentView.getOptionUnitHeight(), controller.getvScrollBar(), controller.getAxis());
		});

		controller.getCanvas().widthProperty().addListener(updateListener);
		controller.getCanvas().heightProperty().addListener(updateListener);
		alignmentView.optionUnitWidthProperty().addListener(updateListener);
		alignmentView.optionUnitHeightProperty().addListener(updateListener);

		invalidationListener = e -> {
			if (workflow.getWorkingDataNode() != null && workflow.getWorkingDataNode().getDataBlock() instanceof CharactersBlock charactersBlock) {
				workingCharactersNode.set(workflow.getWorkingDataNode());
				workingCharactersNode.get().validProperty().addListener(updateListener);
				workingCharacters.set(charactersBlock);
			} else {
				workingCharactersNode.set(null);
				workingCharacters.set(null);
			}
		};
		mainWindow.getWorkflow().validProperty().addListener(new WeakInvalidationListener(invalidationListener));
		invalidationListener.invalidated(null);

		alignmentView.optionColorSchemeProperty().addListener(updateListener);
		alignmentView.optionColorSchemeProperty().addListener((v, o, n) -> alignmentView.getUndoManager().add("color scheme", alignmentView.optionColorSchemeProperty(), o, n));
		alignmentView.optionUnitWidthProperty().addListener((v, o, n) -> alignmentView.getUndoManager().add("column width", alignmentView.optionUnitWidthProperty(), o, n));
		alignmentView.optionUnitHeightProperty().addListener((v, o, n) -> alignmentView.getUndoManager().add("row height", alignmentView.optionUnitHeightProperty(), o, n));

		MainWindowManager.useDarkThemeProperty().addListener(new WeakInvalidationListener(updateListener));

		controller.gethScrollBar().valueProperty().addListener(updateListener);
		controller.gethScrollBar().valueProperty().addListener((v, o, n) -> {
			var diff = n.doubleValue() - o.doubleValue();
			controller.getAxis().setLowerBound(controller.getAxis().getLowerBound() + diff);
			controller.getAxis().setUpperBound(controller.getAxis().getUpperBound() + diff);
		});
		controller.getvScrollBar().valueProperty().addListener(updateListener);

		controller.getExpandHorizontallyButton().setOnAction(e -> alignmentView.setOptionUnitWidth(1.2 * alignmentView.getOptionUnitWidth()));
		controller.getExpandHorizontallyButton().disableProperty().bind(alignmentView.optionUnitWidthProperty().greaterThan(64));

		controller.getContractHorizontallyButton().setOnAction(e -> alignmentView.setOptionUnitWidth(1 / 1.2 * alignmentView.getOptionUnitWidth()));
		controller.getContractHorizontallyButton().disableProperty().bind(alignmentView.optionUnitWidthProperty().lessThan(0.01));

		controller.getExpandVerticallyButton().setOnAction(e -> alignmentView.setOptionUnitHeight(1.2 * alignmentView.getOptionUnitHeight()));
		controller.getExpandVerticallyButton().disableProperty().bind(alignmentView.optionUnitHeightProperty().greaterThan(64));

		controller.getContractVerticallyButton().setOnAction(e -> alignmentView.setOptionUnitHeight(1 / 1.2 * alignmentView.getOptionUnitHeight()));
		controller.getContractVerticallyButton().disableProperty().bind(alignmentView.optionUnitHeightProperty().lessThan(0.01));

		Platform.runLater(() -> invalidationListener.invalidated(null));
		Platform.runLater(() -> updateListener.invalidated(null));

		Platform.runLater(() -> {
			var taxonHBar = BasicFX.getScrollBar(controller.getTaxaListView(), Orientation.HORIZONTAL);
			if (taxonHBar != null) {
				controller.getLeftBottomPane().setMaxHeight(taxonHBar.isVisible() ? 0 : 16);
				taxonHBar.visibleProperty().addListener((v, o, n) -> controller.getLeftBottomPane().setPrefHeight(n ? 0 : 16));
			}

			var taxonVBar = BasicFX.getScrollBar(controller.getTaxaListView(), Orientation.VERTICAL);
			if (taxonVBar != null) {
				controller.getvScrollBar().visibleProperty().bind(taxonVBar.visibleProperty());
				controller.getvScrollBar().minProperty().bind(taxonVBar.minProperty());
				controller.getvScrollBar().maxProperty().bind(taxonVBar.maxProperty());
				controller.getvScrollBar().visibleAmountProperty().bind(taxonVBar.visibleAmountProperty());
				taxonVBar.valueProperty().bindBidirectional(controller.getvScrollBar().valueProperty());
			}
		});
	}

	private void updateTaxaCellFactory(ListView<Taxon> listView, double unitHeight) {
		listView.setFixedCellSize(unitHeight);

		listView.setCellFactory(cell -> new ListCell<>() {
			@Override
			protected void updateItem(Taxon item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
					setGraphic(null);
					setStyle(String.format("-fx-font-size: %.1f;", Math.min(18, 0.6 * unitHeight)));
					setText(item.getName());
					setAlignment(Pos.CENTER_LEFT);
				}
			}
		});
	}

	private void updateCanvas(Canvas canvas, TaxaBlock taxaBlock, CharactersBlock charactersBlock, ColorScheme colorScheme,
							  double boxWidth, double boxHeight, ScrollBar vScrollBar, NumberAxis axis) {
		var gc = canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

		if (taxaBlock != null && charactersBlock != null) {
			controller.getTaxaListView().getItems().clear();
			for (var t = 1; t <= taxaBlock.getNtax(); t++) {
				var taxon = taxaBlock.get(t);
				controller.getTaxaListView().getItems().add(taxon);
			}

			var fontSize = 0.9 * Math.min(boxWidth, boxHeight);
			gc.setFont(Font.font("monospaced", fontSize));
			var showColors = (colorScheme != ColorScheme.None);

			var textFill = !showColors && MainWindowManager.isUseDarkTheme() ? Color.WHITE : Color.BLACK;

			var offset = vScrollBar.isVisible() ? (vScrollBar.getValue() * (canvas.getHeight() - taxaBlock.getNtax() * boxHeight)) : 0;

			for (var t = 1; t <= taxaBlock.getNtax(); t++) {
				var y = t * boxHeight + offset;
				if (y < 0)
					continue;
				;
				if (y > canvas.getHeight() + boxHeight)
					break;

				var bot = (int) Math.max(1, Math.floor(axis.getLowerBound()));
				var top = Math.min(charactersBlock.getNchar(), Math.ceil(axis.getUpperBound()));
				var col = 0;
				for (var c = bot; c <= top; c++) {
					var ch = charactersBlock.get(t, c);
					var x = (col++) * boxWidth;
					if (showColors) {
						gc.setFill(colorScheme.apply(ch));
						gc.fillRect(x, y - boxHeight, boxWidth, boxHeight);
					}
					gc.setFill(textFill);
					gc.fillText(String.valueOf(ch), x + 0.25 * fontSize, y - 0.4 * fontSize);
				}
			}
		}
	}

	private void updateAxisAndScrollBar(NumberAxis axis, ScrollBar scrollBar, double canvasWidth, double boxWidth, int nChar) {
		if (nChar < 1) {
			scrollBar.setVisible(false);
			axis.setVisible(false);
		} else {
			scrollBar.setVisible(true);
			axis.setVisible(true);
			var numberOnCanvas = canvasWidth / boxWidth;
			scrollBar.setMin(1);
			scrollBar.setMax(nChar);
			scrollBar.setVisibleAmount(numberOnCanvas);
			axis.setLowerBound(Math.floor(scrollBar.getValue()));
			axis.setUpperBound(Math.round(scrollBar.getValue() + numberOnCanvas));
			if (numberOnCanvas < 100) {
				if (axis.getLowerBound() == 0)
					axis.setLowerBound(1);
				axis.setTickUnit(1);
				axis.setMinorTickVisible(false);
			} else if (numberOnCanvas < 500) {
				if (axis.getLowerBound() == 1)
					axis.setLowerBound(0);
				axis.setTickUnit(10);
				axis.setMinorTickVisible(true);
			} else if (numberOnCanvas < 5000) {
				if (axis.getLowerBound() == 1)
					axis.setLowerBound(0);
				axis.setTickUnit(100);
				axis.setMinorTickVisible(true);
			} else {
				axis.setTickUnit(1000);
				axis.setMinorTickVisible(true);
			}
		}
	}


	@Override
	public void setupMenuItems() {
		mainWindowController.getZoomInMenuItem().setOnAction(controller.getExpandVerticallyButton().getOnAction());
		mainWindowController.getZoomOutMenuItem().disableProperty().bind(controller.getContractVerticallyButton().disableProperty());
		mainWindowController.getZoomInHorizontalMenuItem().setOnAction(controller.getExpandHorizontallyButton().getOnAction());
		mainWindowController.getZoomOutHorizontalMenuItem().disableProperty().bind(controller.getContractHorizontallyButton().disableProperty());
	}
}
