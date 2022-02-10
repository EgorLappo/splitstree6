/*
 * TaxLabelFormatterController.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree6.view.format.taxlabels;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;

public class TaxLabelFormatterController {
	@FXML
	private VBox vBox;

	@FXML
	private ComboBox<String> fontFamilyCbox;

	@FXML
	private TextField fontSizeTextArea;

	@FXML
	private ToggleButton boldToggleButton;

	@FXML
	private ToggleButton italicToggleButton;

	@FXML
	private ToggleButton underlineToggleButton;

	@FXML
	private ToggleButton strikeToggleButton;

	@FXML
	private ColorPicker textFillColorChooser;

	@FXML
	private void initialize() {
		fontSizeTextArea.setTextFormatter(new TextFormatter<>(new DoubleStringConverter()));
	}

	public VBox getvBox() {
		return vBox;
	}

	public ComboBox<String> getFontFamilyCbox() {
		return fontFamilyCbox;
	}

	public TextField getFontSizeTextArea() {
		return fontSizeTextArea;
	}

	public ToggleButton getBoldToggleButton() {
		return boldToggleButton;
	}

	public ToggleButton getItalicToggleButton() {
		return italicToggleButton;
	}

	public ToggleButton getUnderlineToggleButton() {
		return underlineToggleButton;
	}

	public ToggleButton getStrikeToggleButton() {
		return strikeToggleButton;
	}

	public ColorPicker getTextFillColorChooser() {
		return textFillColorChooser;
	}
}