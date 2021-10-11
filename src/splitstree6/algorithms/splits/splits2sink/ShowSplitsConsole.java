/*
 *  Copyright (C) 2018. Daniel H. Huson
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

package splitstree6.algorithms.splits.splits2sink;

import jloda.util.ProgressListener;
import splitstree6.data.SinkBlock;
import splitstree6.data.SplitsBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.io.writers.splits.TabbedTextWriter;

import java.io.IOException;
import java.io.StringWriter;

public class ShowSplitsConsole extends Splits2Sink {
	@Override
	public void compute(ProgressListener progress, TaxaBlock taxaBlock, SplitsBlock inputData, SinkBlock outputData) throws IOException {
		try (var w = new StringWriter()) {
			w.write(inputData.getName() + ":\n");
			var writer = new TabbedTextWriter();
			writer.write(w, taxaBlock, inputData);
			System.out.println(w);
		}
	}
}
