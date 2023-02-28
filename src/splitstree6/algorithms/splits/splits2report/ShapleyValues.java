/*
 * ShowSplits.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.algorithms.splits.splits2report;

import jloda.fx.util.ProgramExecutorService;
import jloda.util.BitSetUtils;
import jloda.util.ExecuteInParallel;
import jloda.util.progress.ProgressListener;
import splitstree6.data.SplitsBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.data.parts.ASplit;
import splitstree6.data.parts.Taxon;

import java.util.*;

/**
 * compute shapley values
 * Daniel Huson, 2.2023
 */
public class ShapleyValues extends Splits2ReportBase {
	@Override
	String runAnalysis(ProgressListener progress, TaxaBlock taxaBlock, SplitsBlock splitsBlock, Collection<Taxon> selectedTaxa) {
		return report(taxaBlock, splitsBlock.getSplits());
	}

	public static Map<Integer, Double> compute(BitSet taxa, Collection<ASplit> splits) {
		var ntax = taxa.cardinality();
		var taxonShapleyMap = new HashMap<Integer, Double>();

		try {
			ExecuteInParallel.apply(BitSetUtils.asList(taxa), t -> {
				taxonShapleyMap.put(t, splits.stream().mapToDouble(s -> (s.getPartNotContaining(t).cardinality() * s.getWeight()) / (ntax * s.getPartContaining(t).cardinality())).sum());
			}, ProgramExecutorService.getNumberOfCoresToUse());
		} catch (Exception ignored) {
		}

		{ // scale to 1:
			var sum = taxonShapleyMap.values().stream().mapToDouble(d -> d).sum();
			if (sum > 0)
				taxonShapleyMap.entrySet().forEach(e -> e.setValue(e.getValue() / sum));

		}
		return taxonShapleyMap;
	}

	public static String report(TaxaBlock taxaBlock, Collection<ASplit> splits) {
		var map = compute(taxaBlock.getTaxaSet(), splits);
		var entries = new ArrayList<>(map.entrySet());
		entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // by decreasing value
		var buf = new StringBuilder("Unrooted Shapley values:\n");
		for (var entry : entries) {
			buf.append(String.format("%s: %.2f%%%n", taxaBlock.get(entry.getKey()).getName(), 100 * entry.getValue()));
		}
		return buf.toString();
	}
}