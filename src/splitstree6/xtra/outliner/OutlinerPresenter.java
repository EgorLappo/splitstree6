/*
 *  OutlinerPresenter.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.xtra.outliner;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;

public class OutlinerPresenter {

	public OutlinerPresenter(Outliner outliner) {
		var controller = outliner.getController();
		var model = outliner.getModel();

		var emptyProperty = new SimpleBooleanProperty(true);
		model.lastUpdateProperty().addListener(e -> Platform.runLater(() -> emptyProperty.set(model.getTreesBlock().getNTrees() == 0)));

		// MenuBar
		controller.getOpenMenuItem().setOnAction(e -> {
			openFile(outliner.getStage(), controller, model);
		});
		controller.getOpenMenuItem().disableProperty().bind(controller.getProgressBar().visibleProperty());
		controller.getCloseMenuItem().setOnAction(e -> Platform.exit());

		model.lastUpdateProperty().addListener((v, o, n) -> Platform.runLater(() -> {
			try {
				var width = outliner.getStage().getWidth() - 10;
				var height = outliner.getStage().getHeight() - 80;
				controller.getStackPane().getChildren().clear();
				controller.getStackPane().getChildren().add(ComputeOutlineAndReferenceTree.apply(model, controller.getReferenceCheckbox().isSelected(),
						controller.getOthersCheckBox().isSelected(), width, height));
			} catch (Exception ex) {
				controller.getLabel().setText("Error: " + ex.getMessage());
			}
		}));

		controller.getReferenceCheckbox().disableProperty().bind(emptyProperty);
		controller.getOthersCheckBox().disableProperty().bind(emptyProperty);

		controller.getRedrawButton().setOnAction(e -> model.incrementLastUpdate());
		controller.getRedrawButton().disableProperty().bind(emptyProperty);

	}

	private void openFile(Stage stage, OutlinerController controller, Model model) {
		final var fileChooser = new FileChooser();
		fileChooser.setTitle("Open trees");

		var file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			var service = new Service<Integer>() {
				@Override
				protected Task<Integer> createTask() {
					return new Task<>() {
						@Override
						protected Integer call() throws Exception {
							model.load(file);
							return model.getTreesBlock().getNTrees();
						}
					};
				}
			};
			controller.getProgressBar().visibleProperty().bind(service.runningProperty());
			controller.getProgressBar().progressProperty().bind(service.progressProperty());
			service.setOnSucceeded(v -> {
				System.out.println("Loading succeeded");
				controller.getLabel().setText("Taxa: %,d, Trees: %,d".formatted(model.getTaxaBlock().getNtax(),
						model.getTreesBlock().getNTrees()));
			});
			service.setOnFailed(u -> {
				System.out.println("Loading trees failed");
				controller.getLabel().setText("Loading trees failed");
			});
			service.start();

		}
	}
}
