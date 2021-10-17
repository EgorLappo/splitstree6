/*
 *  InputEditorTabPresenter.java Copyright (C) 2021 Daniel H. Huson
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

package splitstree6.treeview;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TreeItem;
import splitstree6.tabs.IDisplayTabPresenter;
import splitstree6.window.MainWindow;

import java.util.LinkedList;

public class WorkflowTreeViewPresenter implements IDisplayTabPresenter {
	private final MainWindow mainWindow;
	private final WorkflowTreeView tab;

	public WorkflowTreeViewPresenter(MainWindow mainWindow, WorkflowTreeView tab) {
		this.mainWindow = mainWindow;
		this.tab = tab;

		tab.getController().getWorkflowTreeView().setRoot(new WorkflowTreeItem(mainWindow));
	}

	public void setup() {
		var controller = mainWindow.getController();
		var tabController = tab.getController();

		controller.getCopyMenuItem().setOnAction(null);

		controller.getFindMenuItem().setOnAction(null);
		controller.getFindAgainMenuItem().setOnAction(null);

		controller.getSelectAllMenuItem().setOnAction(null);
		controller.getSelectNoneMenuItem().setOnAction(null);

		controller.getIncreaseFontSizeMenuItem().setOnAction(null);
		controller.getDecreaseFontSizeMenuItem().setOnAction(null);

		controller.getZoomInMenuItem().setOnAction(null);
		controller.getZoomOutMenuItem().setOnAction(null);


		var treeView = tabController.getWorkflowTreeView();
		treeView.getStyleClass().add("background");

		tabController.getCollapseAllButton().setOnAction((e) -> treeView.getRoot().setExpanded(false));

		tabController.getExpandAllButton().setOnAction((e) -> {
			final var queue = new LinkedList<TreeItem<String>>();
			queue.add(treeView.getRoot());
			while (queue.size() > 0) {
				final var item = queue.poll();
				item.setExpanded(true);
				queue.addAll(item.getChildren());
			}
		});

		tabController.getShowButton().setOnAction((e) -> {
			for (var item : treeView.getSelectionModel().getSelectedItems()) {
				//final Point2D point2D = item.getGraphic().localToScreen(item.getGraphic().getLayoutX(), item.getGraphic().getLayoutY());
				((WorkflowTreeItem) item).showView();
			}
		});
		tabController.getShowButton().disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedItems()));

	}
}
