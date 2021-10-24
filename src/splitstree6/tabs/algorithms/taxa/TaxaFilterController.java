/*
 *  TaxaFilterController.java Copyright (C) 2021 Daniel H. Huson
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

package splitstree6.tabs.algorithms.taxa;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

public class TaxaFilterController {
	@FXML
	private AnchorPane anchorPane;

	@FXML
	private ListView<String> activeListView;

	@FXML
	private ListView<String> inactiveListView;

	@FXML
	private Button moveSelectedRightButton;

	@FXML
	private Button moveAllRightButton;

	@FXML
	private Button moveSelectedLeftButton;

	@FXML
	private Button moveAllLeftButton;

	public AnchorPane getAnchorPane() {
		return anchorPane;
	}

	public ListView<String> getActiveListView() {
		return activeListView;
	}

	public ListView<String> getInactiveListView() {
		return inactiveListView;
	}

	public Button getMoveSelectedRightButton() {
		return moveSelectedRightButton;
	}

	public Button getMoveAllRightButton() {
		return moveAllRightButton;
	}

	public Button getMoveSelectedLeftButton() {
		return moveSelectedLeftButton;
	}

	public Button getMoveAllLeftButton() {
		return moveAllLeftButton;
	}
}
