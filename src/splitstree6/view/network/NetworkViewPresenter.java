/*
 * NetworkViewPresenter.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree6.view.network;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ObservableMap;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.Group;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import jloda.fx.control.RichTextLabel;
import jloda.fx.find.FindToolBar;
import jloda.fx.util.ResourceManagerFX;
import jloda.fx.window.MainWindowManager;
import jloda.graph.Node;
import jloda.util.Single;
import jloda.util.StringUtils;
import splitstree6.data.NetworkBlock;
import splitstree6.layout.network.DiagramType;
import splitstree6.layout.tree.LayoutOrientation;
import splitstree6.tabs.IDisplayTabPresenter;
import splitstree6.view.findreplace.FindReplaceTaxa;
import splitstree6.view.utils.ComboBoxUtils;
import splitstree6.window.MainWindow;

import java.util.ArrayList;
import java.util.Objects;

public class NetworkViewPresenter implements IDisplayTabPresenter {
	private final LongProperty updateCounter = new SimpleLongProperty(0L);

	private final MainWindow mainWindow;
	private final NetworkView networkView;
	private final NetworkViewController controller;

	private final FindToolBar findToolBar;


	private final ObjectProperty<NetworkPane> networkPane = new SimpleObjectProperty<>();
	private final InvalidationListener updateListener;

	private final MouseInteraction mouseInteraction;

	public NetworkViewPresenter(MainWindow mainWindow, NetworkView networkView, ObjectProperty<Bounds> targetBounds, ObjectProperty<NetworkBlock> networkBlock, ObservableMap<Integer, RichTextLabel> taxonLabelMap,
								ObservableMap<Node, Group> nodeShapeMap,
								ObservableMap<jloda.graph.Edge, Group> edgeShapeMap) {
		this.mainWindow = mainWindow;
		this.networkView = networkView;
		this.controller = networkView.getController();

		mouseInteraction = new MouseInteraction(mainWindow.getStage(), networkView.getUndoManager(), mainWindow.getTaxonSelectionModel());
		networkPane.addListener((v, o, n) -> {
			controller.getScrollPane().setContent(n);
		});

		controller.getScrollPane().setLockAspectRatio(true);
		controller.getScrollPane().setRequireShiftOrControlToZoom(true);
		controller.getScrollPane().setUpdateScaleMethod(() -> networkView.setOptionZoomFactor(controller.getScrollPane().getZoomFactorY() * networkView.getOptionZoomFactor()));

		controller.getDiagramCBox().setButtonCell(ComboBoxUtils.createButtonCell(null, null));
		controller.getDiagramCBox().setCellFactory(ComboBoxUtils.createCellFactory(null, null));
		controller.getDiagramCBox().getItems().addAll(DiagramType.values());
		controller.getDiagramCBox().valueProperty().bindBidirectional(networkView.optionDiagramProperty());

		controller.getOrientationCBox().setButtonCell(ComboBoxUtils.createButtonCell(null, LayoutOrientation::createLabel));
		controller.getOrientationCBox().setCellFactory(ComboBoxUtils.createCellFactory(null, LayoutOrientation::createLabel));
		controller.getOrientationCBox().getItems().addAll(LayoutOrientation.values());
		controller.getOrientationCBox().valueProperty().bindBidirectional(networkView.optionOrientationProperty());

		var boxDimension = new SimpleObjectProperty<Dimension2D>();
		targetBounds.addListener((v, o, n) -> boxDimension.set(new Dimension2D(n.getWidth() - 40, n.getHeight() - 80)));

		var first = new Single<>(true);

		updateListener = e -> {
			if (first.get())
				first.set(false);
			// else  SplitNetworkEdits.clearEdits(splitsView.optionEditsProperty());

			var pane = new NetworkPane(mainWindow, mainWindow.getWorkflow().getWorkingTaxaBlock(), networkBlock.get(), mainWindow.getTaxonSelectionModel(),
					boxDimension.get().getWidth(), boxDimension.get().getHeight(), networkView.getOptionDiagram(), networkView.optionOrientationProperty(),
					networkView.optionZoomFactorProperty(), networkView.optionFontScaleFactorProperty(),
					taxonLabelMap, nodeShapeMap, edgeShapeMap);

			networkPane.set(pane);

			pane.setRunAfterUpdate(() -> {
				var taxa = mainWindow.getWorkflow().getWorkingTaxaBlock();
				mouseInteraction.setup(taxonLabelMap, nodeShapeMap, taxa::get, taxa::indexOf);
				updateCounter.set(updateCounter.get() + 1);
			});
			pane.drawNetwork();
		};

		networkBlock.addListener(updateListener);
		networkView.optionDiagramProperty().addListener(updateListener);

		controller.getZoomInButton().setOnAction(e -> networkView.setOptionZoomFactor(1.1 * networkView.getOptionZoomFactor()));
		controller.getZoomInButton().disableProperty().bind(networkView.emptyProperty().or(networkView.optionZoomFactorProperty().greaterThan(8.0 / 1.1)));
		controller.getZoomOutButton().setOnAction(e -> networkView.setOptionZoomFactor((1.0 / 1.1) * networkView.getOptionZoomFactor()));
		controller.getZoomOutButton().disableProperty().bind(networkView.emptyProperty());

		controller.getIncreaseFontButton().setOnAction(e -> networkView.setOptionFontScaleFactor(1.2 * networkView.getOptionFontScaleFactor()));
		controller.getIncreaseFontButton().disableProperty().bind(networkView.emptyProperty());
		controller.getDecreaseFontButton().setOnAction(e -> networkView.setOptionFontScaleFactor((1.0 / 1.2) * networkView.getOptionFontScaleFactor()));
		controller.getDecreaseFontButton().disableProperty().bind(networkView.emptyProperty());

		findToolBar = FindReplaceTaxa.create(mainWindow, networkView.getUndoManager());
		findToolBar.setShowFindToolBar(false);
		controller.getvBox().getChildren().add(findToolBar);
		controller.getFindToggleButton().setOnAction(e -> {
			if (!findToolBar.isShowFindToolBar()) {
				findToolBar.setShowFindToolBar(true);
				controller.getFindToggleButton().setSelected(true);
				controller.getFindToggleButton().setGraphic(ResourceManagerFX.getIconAsImageView("sun/Replace24.gif", 16));
			} else if (!findToolBar.isShowReplaceToolBar()) {
				findToolBar.setShowReplaceToolBar(true);
				controller.getFindToggleButton().setSelected(true);
			} else {
				findToolBar.setShowFindToolBar(false);
				findToolBar.setShowReplaceToolBar(false);
				controller.getFindToggleButton().setSelected(false);
				controller.getFindToggleButton().setGraphic(ResourceManagerFX.getIconAsImageView("sun/Find24.gif", 16));
			}
		});

		networkView.viewTabProperty().addListener((v, o, n) -> {
			if (n != null) {
				controller.getvBox().getChildren().add(0, n.getAlgorithmBreadCrumbsToolBar());
			}
		});
		networkView.emptyProperty().addListener(e -> networkView.getRoot().setDisable(networkView.emptyProperty().get()));

		var undoManager = networkView.getUndoManager();

		networkView.optionDiagramProperty().addListener((v, o, n) -> undoManager.add("diagram", networkView.optionDiagramProperty(), o, n));
		networkView.optionOrientationProperty().addListener((v, o, n) -> undoManager.add("orientation", networkView.optionOrientationProperty(), o, n));
		networkView.optionFontScaleFactorProperty().addListener((v, o, n) -> undoManager.add("font size", networkView.optionFontScaleFactorProperty(), o, n));
		networkView.optionZoomFactorProperty().addListener((v, o, n) -> undoManager.add("zoom factor", networkView.optionZoomFactorProperty(), o, n));

		Platform.runLater(this::setupMenuItems);

	}

	@Override
	public void setupMenuItems() {
		var mainController = mainWindow.getController();

		mainController.getCopyMenuItem().setOnAction(e -> {
			var list = new ArrayList<String>();
			for (var taxon : mainWindow.getTaxonSelectionModel().getSelectedItems()) {
				list.add(RichTextLabel.getRawText(taxon.getDisplayLabelOrName()).trim());
			}
			if (list.size() > 0) {
				var content = new ClipboardContent();
				content.put(DataFormat.PLAIN_TEXT, StringUtils.toString(list, "\n"));
				Clipboard.getSystemClipboard().setContent(content);
			}
		});
		mainController.getCopyMenuItem().disableProperty().bind(mainWindow.getTaxonSelectionModel().sizeProperty().isEqualTo(0).or(mainWindow.getStage().focusedProperty().not()));

		mainController.getCutMenuItem().disableProperty().bind(new SimpleBooleanProperty(true));
		mainController.getCopyNewickMenuItem().disableProperty().bind(new SimpleBooleanProperty(true));

		mainController.getPasteMenuItem().disableProperty().bind(new SimpleBooleanProperty(true));

		mainWindow.getController().getIncreaseFontSizeMenuItem().setOnAction(controller.getIncreaseFontButton().getOnAction());
		mainWindow.getController().getIncreaseFontSizeMenuItem().disableProperty().bind(controller.getIncreaseFontButton().disableProperty());
		mainWindow.getController().getDecreaseFontSizeMenuItem().setOnAction(controller.getDecreaseFontButton().getOnAction());
		mainWindow.getController().getDecreaseFontSizeMenuItem().disableProperty().bind(controller.getDecreaseFontButton().disableProperty());

		mainController.getZoomInMenuItem().setOnAction(controller.getZoomInButton().getOnAction());
		mainController.getZoomInMenuItem().disableProperty().bind(controller.getZoomOutButton().disableProperty());

		mainController.getZoomOutMenuItem().setOnAction(controller.getZoomOutButton().getOnAction());
		mainController.getZoomOutMenuItem().disableProperty().bind(controller.getZoomOutButton().disableProperty());

		mainController.getFindMenuItem().setOnAction(e -> findToolBar.setShowFindToolBar(true));
		mainController.getFindAgainMenuItem().setOnAction(e -> findToolBar.findAgain());
		mainController.getFindAgainMenuItem().disableProperty().bind(findToolBar.canFindAgainProperty().not());
		mainController.getReplaceMenuItem().setOnAction(e -> findToolBar.setShowReplaceToolBar(true));

		mainController.getSelectAllMenuItem().setOnAction(e -> mainWindow.getTaxonSelectionModel().selectAll(mainWindow.getWorkflow().getWorkingTaxaBlock().getTaxa()));
		mainController.getSelectAllMenuItem().disableProperty().bind(networkView.emptyProperty());

		mainController.getSelectNoneMenuItem().setOnAction(e -> mainWindow.getTaxonSelectionModel().clearSelection());
		mainController.getSelectNoneMenuItem().disableProperty().bind(mainWindow.getTaxonSelectionModel().sizeProperty().isEqualTo(0));

		mainController.getSelectInverseMenuItem().setOnAction(e -> mainWindow.getWorkflow().getWorkingTaxaBlock().getTaxa().forEach(t -> mainWindow.getTaxonSelectionModel().toggleSelection(t)));
		mainController.getSelectInverseMenuItem().disableProperty().bind(networkView.emptyProperty());

		mainController.getSelectFromPreviousMenuItem().setOnAction(e -> {
			var taxonBlock = mainWindow.getWorkflow().getWorkingTaxaBlock();
			if (taxonBlock != null) {
				MainWindowManager.getPreviousSelection().stream().map(taxonBlock::get).filter(Objects::nonNull).forEach(t -> mainWindow.getTaxonSelectionModel().select(t));
			}
		});
		mainController.getSelectFromPreviousMenuItem().disableProperty().bind(Bindings.isEmpty(MainWindowManager.getPreviousSelection()));

		mainController.getLayoutLabelsMenuItem().setOnAction(e -> updateLabelLayout());
		mainController.getLayoutLabelsMenuItem().disableProperty().bind(networkPane.isNull());

		mainController.getRotateLeftMenuItem().setOnAction(e -> networkView.setOptionOrientation(networkView.getOptionOrientation().getRotateLeft()));
		mainController.getRotateLeftMenuItem().disableProperty().bind(networkView.emptyProperty());
		mainController.getRotateRightMenuItem().setOnAction(e -> networkView.setOptionOrientation(networkView.getOptionOrientation().getRotateRight()));
		mainController.getRotateRightMenuItem().disableProperty().bind(networkView.emptyProperty());
		mainController.getFlipMenuItem().setOnAction(e -> networkView.setOptionOrientation(networkView.getOptionOrientation().getFlip()));
		mainController.getFlipMenuItem().disableProperty().bind(networkView.emptyProperty());

	}

	public LongProperty updateCounterProperty() {
		return updateCounter;
	}

	public void updateLabelLayout() {
		if (networkPane.get() != null)
			Platform.runLater(() -> networkPane.get().layoutLabels(networkView.getOptionOrientation()));
	}
}
