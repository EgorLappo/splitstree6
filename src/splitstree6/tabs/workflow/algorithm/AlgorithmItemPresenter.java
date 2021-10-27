/*
 *  AlgorithmItemPresenter.java Copyright (C) 2021 Daniel H. Huson
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

package splitstree6.tabs.workflow.algorithm;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ProgressIndicator;
import jloda.fx.util.ResourceManagerFX;
import splitstree6.contextmenus.algorithmnode.AlgorithmNodeContextMenu;
import splitstree6.tabs.workflow.WorkflowTab;
import splitstree6.tabs.workflow.WorkflowTabPresenter;
import splitstree6.window.MainWindow;
import splitstree6.workflow.Workflow;

public class AlgorithmItemPresenter {

	public AlgorithmItemPresenter(MainWindow mainWindow, WorkflowTab workflowTab, AlgorithmItem algorithmItem) {

		var algorithmNode = algorithmItem.getWorkflowNode();
		var controller = algorithmItem.getController();

		algorithmItem.setEffect(WorkflowTabPresenter.getDropShadow());

		controller.getNameLabel().textProperty().bind(algorithmNode.nameProperty());

		var selected = new SimpleBooleanProperty(false);
		workflowTab.getSelectionModel().getSelectedItems().addListener((InvalidationListener) e -> selected.set(workflowTab.getSelectionModel().isSelected(algorithmItem)));

		controller.getEditButton().setOnAction(e -> mainWindow.getAlgorithmTabsManager().showTab(algorithmNode, true));
		controller.getEditButton().disableProperty().bind(selected.not());

		controller.getPlayButton().setOnAction(e -> algorithmNode.restart());
		controller.getPlayButton().disableProperty().bind((algorithmNode.getService().runningProperty().and(algorithmNode.allParentsValidProperty()).and(selected)).not());

		algorithmNode.getService().runningProperty().addListener((v, o, n) -> {
			if (n) {
				controller.getPlayButton().setGraphic(ResourceManagerFX.getIconAsImageView("sun/Stop16.gif", 16));
				controller.getPlayButton().setOnAction(e -> algorithmNode.getService().cancel());
				controller.getPlayButton().disableProperty().bind((algorithmNode.getService().runningProperty().and(algorithmNode.allParentsValidProperty()).and(selected)).not());
				controller.getPlayButton().getTooltip().setText("Stop this algorithm");
			} else {
				controller.getPlayButton().setGraphic(ResourceManagerFX.getIconAsImageView("sun/Play16.gif", 16));
				controller.getPlayButton().setOnAction(e -> algorithmNode.restart());
				controller.getPlayButton().disableProperty().bind((algorithmNode.getService().runningProperty().not().and(algorithmNode.allParentsValidProperty()).and(selected).not()));
				controller.getPlayButton().getTooltip().setText("Run this algorithm");
			}
		});

		controller.getNameLabel().setGraphic(ResourceManagerFX.getIconAsImageView(algorithmNode.getName().endsWith("Filter") ? "Filter16.gif" : "Algorithm16.gif", 16));

		controller.getAnchorPane().getStyleClass().add("background");
		if (!mainWindow.getWorkflow().isDerivedNode(algorithmNode)) {
			controller.getNameLabel().setStyle("-fx-text-fill: darkgray");
			if (controller.getNameLabel().getGraphic() != null)
				controller.getNameLabel().getGraphic().setOpacity(0.5);
		}

		algorithmNode.validProperty().addListener((v, o, n) -> {
			if (!n)
				controller.getIconPane().getChildren().setAll(ResourceManagerFX.getIconAsImageView("Scheduled.png", 16));
		});

		var progressIndicator = new ProgressIndicator();
		// progressIndicator.progressProperty().bind(algorithmNode.getService().progressProperty());
		progressIndicator.setPrefHeight(16);
		progressIndicator.setPrefWidth(16);

		controller.getIconPane().getChildren().setAll(ResourceManagerFX.getIconAsImageView("Scheduled.png", 16));

		algorithmNode.getService().stateProperty().addListener((v, o, n) -> {
			switch (n) {
				case CANCELLED, FAILED -> controller.getIconPane().getChildren().setAll(ResourceManagerFX.getIconAsImageView("Failed.png", 16));
				case READY, SCHEDULED -> controller.getIconPane().getChildren().setAll(ResourceManagerFX.getIconAsImageView("Scheduled.png", 16));
				case SUCCEEDED -> controller.getIconPane().getChildren().setAll(ResourceManagerFX.getIconAsImageView("Done.png", 16));
				case RUNNING -> controller.getIconPane().getChildren().setAll(progressIndicator);
			}
		});

		if (algorithmNode.getName().equals(Workflow.INPUT_TAXA_DATA_FILTER)) {
			controller.getEditButton().setVisible(false);
			controller.getPlayButton().setVisible(false);
		}

		algorithmItem.setOnContextMenuRequested(e -> {
					if (selected.get())
						AlgorithmNodeContextMenu.create(mainWindow, workflowTab.getUndoManager(), algorithmNode).show(algorithmItem, e.getScreenX(), e.getScreenY());
				}
		);
	}
}