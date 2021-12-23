/*
 *  SplitsViewController.java Copyright (C) 2021 Daniel H. Huson
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

package splitstree6.view.splits.viewer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import splitstree6.view.trees.treepages.LayoutOrientation;

public class SplitsViewController {

	@FXML
	private AnchorPane anchorPane;

	@FXML
	private BorderPane borderPane;

	@FXML
	private VBox vBox;

	@FXML
	private ToolBar toolBar;

	@FXML
	private Button openButton;

	@FXML
	private Button saveButton;

	@FXML
	private Button printButton;

	@FXML
	private Button findButton;

	@FXML
	private ComboBox<SplitsDiagramType> diagramCBox;

	@FXML
	private ComboBox<SplitsRooting> rootingCBox;

	@FXML
	private ComboBox<LayoutOrientation> orientationCBox;

	@FXML
	private Button zoomInButton;

	@FXML
	private Button zoomOutButton;

	@FXML
	private ScrollPane scrollPane;

	public AnchorPane getAnchorPane() {
		return anchorPane;
	}

	public BorderPane getBorderPane() {
		return borderPane;
	}

	public VBox getvBox() {
		return vBox;
	}

	public ToolBar getToolBar() {
		return toolBar;
	}

	public Button getOpenButton() {
		return openButton;
	}

	public Button getSaveButton() {
		return saveButton;
	}

	public Button getPrintButton() {
		return printButton;
	}

	public Button getFindButton() {
		return findButton;
	}

	public ComboBox<SplitsDiagramType> getDiagramCBox() {
		return diagramCBox;
	}

	public ComboBox<SplitsRooting> getRootingCBox() {
		return rootingCBox;
	}

	public ComboBox<LayoutOrientation> getOrientationCBox() {
		return orientationCBox;
	}

	public Button getZoomInButton() {
		return zoomInButton;
	}

	public Button getZoomOutButton() {
		return zoomOutButton;
	}

	public ScrollPane getScrollPane() {
		return scrollPane;
	}
}
