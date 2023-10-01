/*
 * GeneContentDistance.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.algorithms.characters.characters2distances;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jloda.util.BitSetUtils;
import jloda.util.progress.ProgressListener;
import splitstree6.data.CharactersBlock;
import splitstree6.data.DistancesBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.data.parts.CharactersType;

import java.util.BitSet;
import java.util.List;

/**
 * Gene content distance
 * Daniel Huson, 2004
 */

public class GeneContentDistance extends Characters2Distances {
	public enum Method {MLE, SharedGenes}

	private final ObjectProperty<Method> optionMethod = new SimpleObjectProperty<>(this, "optionMethod", Method.MLE);

	@Override
	public String getCitation() {
		return (getOptionMethod() == Method.MLE ?
				"Huson and Steel 2004; D.H. Huson  and  M. Steel. Phylogenetic  trees  based  on  gene  content. Bioinformatics, 20(13):2044–9, 2004." :
				"Snel et al 1997; B. Snel, P. Bork and MA Huynen. Genome phylogeny based on gene content, Nature Genetics, 21:108-110, 1997.");
	}

	public List<String> listOptions() {
		return List.of(optionMethod.getName());
	}

	@Override
	public String getToolTip(String optionName) {
		if (optionName.equals("optionMethod"))
			return "Choose Maximum likelihood distance estimation (Huson and Steel 2004, eq. 4), or shared genes distance (Snel et al, 1997)";
		else
			return optionName;
	}

	@Override
	public void compute(ProgressListener progress, TaxaBlock taxaBlock, CharactersBlock charactersBlock, DistancesBlock distancesBlock) {
		final var geneSets = computeGeneSets(charactersBlock);
		if (getOptionMethod().equals(Method.SharedGenes))
			computeSharedGenes(distancesBlock, taxaBlock.getNtax(), geneSets);
		else
			computeMLE(distancesBlock, taxaBlock.getNtax(), geneSets);
	}

	/**
	 * computes the SnelBork et al distance
	 */
	private static void computeSharedGenes(DistancesBlock dist, int ntax, BitSet[] geneSets) {
		dist.setNtax(ntax);
		for (var i = 1; i <= ntax; i++) {
			dist.set(i, i, 0.0);
			for (var j = i + 1; j <= ntax; j++) {
				var d = 1.0 - ((double) BitSetUtils.intersection(geneSets[i], geneSets[j]).cardinality()
							   / (double) Math.min(geneSets[i].cardinality(), geneSets[j].cardinality()));
				dist.set(i, j, d);
				dist.set(j, i, d);
			}
		}
	}

	/**
	 * computes the maximum likelihood estimator distance Huson and Steel 2003
	 */
	private static void computeMLE(DistancesBlock dist, int ntax, BitSet[] genes) {
		dist.setNtax(ntax);

		// determine average genomes size:
		var m = 0.0;
		for (var i = 1; i <= ntax; i++) {
			m += genes[i].cardinality();
		}
		m /= ntax;

		final var alpha = new double[ntax + 1];
		for (var i = 1; i <= ntax; i++) {
			alpha[i] = ((double) genes[i].cardinality()) / m;
		}

		for (int i = 1; i <= ntax; i++) {
			dist.set(i, i, 0.0);

			for (var j = i + 1; j <= ntax; j++) {
				var alphaIJ = ((double) BitSetUtils.intersection(genes[i], genes[j]).cardinality()) / m;
				var beta = 1.0 + alphaIJ - alpha[i] - alpha[j];
				var arg = 0.5 * (beta + Math.sqrt(beta * beta + 4.0 * alphaIJ * alphaIJ));
				var d = (arg < 0.0000001 ? 1.0 : -Math.log(arg));
				dist.set(i, j, d);
				dist.set(j, i, d);
			}
		}
	}


	/**
	 * computes gene sets from strings
	 *
	 * @param characters sequences
	 * @return sets of genes
	 */
	static private BitSet[] computeGeneSets(CharactersBlock characters) {
		final var geneSets = new BitSet[characters.getNtax() + 1];

		for (var s = 1; s <= characters.getNtax(); s++) {
			geneSets[s] = new BitSet();
			for (int i = 1; i <= characters.getNchar(); i++) {
				if (characters.get(s, i) == '1')
					geneSets[s].set(i);
			}
		}
		return geneSets;
	}

	@Override
	public boolean isApplicable(TaxaBlock taxa, CharactersBlock datablock) {
		return super.isApplicable(taxa, datablock) && datablock.getDataType() == CharactersType.Standard && datablock.getSymbols().contains("1");
	}

	//GETTER AND SETTER

	public Method getOptionMethod() {
		return optionMethod.get();
	}

	public ObjectProperty<Method> optionMethodProperty() {
		return optionMethod;
	}

	public void setOptionMethod(Method method) {
		this.optionMethod.set(method);
	}
}