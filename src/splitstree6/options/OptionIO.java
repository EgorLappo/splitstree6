/*
 *  OptionIO.java Copyright (C) 2021 Daniel H. Huson
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

package splitstree6.options;

import javafx.beans.property.StringProperty;
import jloda.util.IOExceptionWithLineNumber;
import jloda.util.parse.NexusStreamParser;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Input and output of options
 * Daniel Huson, 11.2021
 * todo: replace by Beans
 */
public class OptionIO {
	/**
	 * parse options
	 */
	public static void parseOptions(NexusStreamParser np, IHasOptions optionsCarrier) throws IOExceptionWithLineNumber {
		if (np.peekMatchIgnoreCase("OPTIONS")) {
			np.matchIgnoreCase("OPTIONS");

			if (!np.peekMatchIgnoreCase(";")) {
				final var optionsNext = new ArrayList<>(Option.getAllOptions(optionsCarrier));
				if (optionsNext.size() > 0) {
					final var legalOptions = new HashMap<String, Option>();
					for (var option : optionsNext) {
						legalOptions.put(option.getName(), option);
					}
					while (true) {
						final var name = np.getWordRespectCase();
						np.matchIgnoreCase("=");
						final var option = legalOptions.get(name);
						if (option != null) {
							final OptionValueType type = option.getOptionValueType();
							switch (type) {
								case doubleArray -> {
									final double[] array = (double[]) option.getProperty().getValue();
									for (int i = 0; i < array.length; i++) {
										array[i] = np.getDouble();
									}
								}
								case doubleSquareMatrix -> {
									final double[][] matrix = (double[][]) option.getProperty().getValue();
									for (int i = 0; i < matrix.length; i++) {
										for (int j = 0; j < matrix.length; j++)
											matrix[i][j] = np.getDouble();
									}
								}
								case Enum -> {
									option.getProperty().setValue(option.getEnumValueForName(np.getWordRespectCase()));
								}
								case stringArray -> {
									final ArrayList<String> list = new ArrayList<>();
									while (!np.peekMatchAnyTokenIgnoreCase(", ;"))
										list.add(np.getWordRespectCase());
									option.getProperty().setValue(list.toArray(new String[0]));
								}
								default -> {
									option.getProperty().setValue(OptionValueType.parseType(option.getOptionValueType(), np.getWordRespectCase()));
								}
							}

						} else {
							final var buf = new StringBuilder();
							while (!np.peekMatchIgnoreCase(",") && !np.peekMatchIgnoreCase(";"))
								buf.append(" ").append(np.getWordRespectCase());
							System.err.println("WARNING: skipped unknown option for '" + optionsCarrier.getClass().getSimpleName() + "': '" + name + "=" + buf + "' in line " + np.lineno());

						}
						if (np.peekMatchIgnoreCase(";"))
							break; // finished reading options
						else
							np.matchIgnoreCase(",");
					}
				}
			}
		}
		np.matchIgnoreCase(";");
	}

	public static void parseOptions(StringProperty initialization, IHasOptions optionsCarrier) throws IOException {
		if (!initialization.get().isBlank()) {
			try (var np = new NexusStreamParser(new StringReader("OPTIONS " + initialization.get() + ";"))) {
				parseOptions(np, optionsCarrier);
			}
		}
	}

	/**
	 * write options
	 */
	public static void writeOptions(Writer w, IHasOptions optionsCarrier) throws IOException {
		final var options = new ArrayList<>(Option.getAllOptions(optionsCarrier));
		if (options.size() > 0) {
			w.write("OPTIONS\n");
			boolean first = true;
			for (var option : options) {
				final var valueString = OptionValueType.toStringType(option.getOptionValueType(), option.getProperty().getValue());
				if (valueString.length() > 0) {
					if (first)
						first = false;
					else
						w.write(",\n");
					w.write("\t" + option.getName() + " = " + valueString);
				}
			}
			w.write(";\n");
		}
	}
}