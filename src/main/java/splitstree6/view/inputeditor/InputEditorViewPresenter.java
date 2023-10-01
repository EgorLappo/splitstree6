/*
 * InputEditorViewPresenter.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.view.inputeditor;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import jloda.fx.util.ProgramProperties;
import jloda.fx.util.RunAfterAWhile;
import jloda.util.StringUtils;
import splitstree6.io.readers.ImportManager;
import splitstree6.view.displaytext.DisplayTextViewPresenter;
import splitstree6.window.MainWindow;

import java.io.File;
import java.util.ArrayList;

/**
 * input editor tab presenter
 * Daniel Huson, 10.2021
 */
public class InputEditorViewPresenter extends DisplayTextViewPresenter {
	private final MainWindow mainWindow;
	private final InputEditorView view;

	private final StringProperty firstLine = new SimpleStringProperty(this, "firstLine", "");

	private final ReadOnlyBooleanProperty TRUE = new SimpleBooleanProperty(true);

	public InputEditorViewPresenter(MainWindow mainWindow, InputEditorView view) {
		super(mainWindow, view, true);
		this.mainWindow = mainWindow;
		this.view = view;

		view.setShowLineNumbers(true);
		view.setWrapText(false);

		var tabController = view.getController();
		var toolBarController = view.getInputEditorViewController();

		var codeArea = tabController.getCodeArea();
		codeArea.setEditable(true);

		var list = new ArrayList<>(toolBarController.getFirstToolBar().getItems());
		list.addAll(tabController.getToolBar().getItems());
		list.addAll(toolBarController.getLastToolBar().getItems());
		tabController.getToolBar().getItems().setAll(list);

		toolBarController.getParseAndLoadButton().setOnAction(e -> view.parseAndLoad());
		toolBarController.getParseAndLoadButton().disableProperty().bind(view.emptyProperty().or(toolBarController.getFormatLabel().textProperty().isEmpty()));

		toolBarController.getOpenButton().setOnAction(e -> {
			final var previousDir = new File(ProgramProperties.get("InputDir", ""));
			final var fileChooser = new FileChooser();
			if (previousDir.isDirectory())
				fileChooser.setInitialDirectory(previousDir);
			fileChooser.setTitle("Open input file");
			fileChooser.getExtensionFilters().addAll(ImportManager.getInstance().getExtensionFilters());
			final var selectedFile = fileChooser.showOpenDialog(mainWindow.getStage());
			if (selectedFile != null) {
				if (selectedFile.getParentFile().isDirectory())
					ProgramProperties.put("InputDir", selectedFile.getParent());
				view.importFromFile(selectedFile.getPath());
			}
		});
		toolBarController.getOpenButton().disableProperty().bind(view.emptyProperty().not());

		toolBarController.getSaveButton().setOnAction(e -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle("Save input text");
			var previousDir = new File(ProgramProperties.get("InputDir", ""));
			if (previousDir.isDirectory()) {
				fileChooser.setInitialDirectory(previousDir);
			}
			fileChooser.setInitialFileName(mainWindow.getFileName());
			var selectedFile = fileChooser.showSaveDialog(mainWindow.getStage());
			if (selectedFile != null) {
				view.saveToFile(selectedFile);
			}
		});
		toolBarController.getSaveButton().disableProperty().bind(view.emptyProperty());

		// prevent double paste:
		codeArea.addEventHandler(KeyEvent.ANY, e -> {
			if (e.getCode() == KeyCode.V && e.isShortcutDown()) {
				e.consume();
			}
		});

		codeArea.focusedProperty().addListener((c, o, n) -> {
			if (n) {
				mainWindow.getController().getPasteMenuItem().disableProperty().unbind();
				mainWindow.getController().getPasteMenuItem().disableProperty().set(!Clipboard.getSystemClipboard().hasString());
			}
		});

		Platform.runLater(codeArea::requestFocus);

		codeArea.textProperty().addListener((v, o, n) -> {
			firstLine.set(StringUtils.getFirstLine(n));
		});

		firstLine.addListener((v, o, n) -> {
			RunAfterAWhile.apply(toolBarController.getFormatLabel(), () -> Platform.runLater(() -> {
				var readers = ImportManager.getInstance().getReadersByText(n);
				if (readers.size() == 0)
					toolBarController.getFormatLabel().setText(null);
				else if (readers.size() == 1) {
					toolBarController.getFormatLabel().setText(readers.get(0).getName());
				} else {
					toolBarController.getFormatLabel().setText(readers.get(0).getName() + " * ");
				}
			}));
		});
	}

	public void setupMenuItems() {
		super.setupMenuItems();

		var controller = mainWindow.getController();
		var toolBarController = view.getInputEditorViewController();

		mainWindow.getController().getMenuBar().getMenus().stream().filter(m -> m.getText().equals("Edit"))
				.forEach(m -> m.disableProperty().bind(new SimpleBooleanProperty(false)));

		controller.getOpenMenuItem().setOnAction(toolBarController.getOpenButton().getOnAction());
		controller.getOpenMenuItem().disableProperty().bind(toolBarController.getOpenButton().disableProperty());

		controller.getReplaceDataMenuItem().disableProperty().bind(TRUE);
		controller.getAnalyzeGenomesMenuItem().disableProperty().bind(TRUE);
		controller.getInputEditorMenuItem().disableProperty().bind(TRUE);

		controller.getOpenRecentMenu().disableProperty().bind(TRUE);

		controller.getImportMultipleTreeFilesMenuItem().disableProperty().bind(TRUE);
	}
}