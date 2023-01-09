/*
 * ConsensusSplits.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.algorithms.trees.trees2splits;

import javafx.beans.property.SimpleObjectProperty;
import jloda.util.progress.ProgressListener;
import splitstree6.algorithms.utils.GreedyCircular;
import splitstree6.algorithms.utils.GreedyCompatible;
import splitstree6.algorithms.utils.GreedyWeaklyCompatible;
import splitstree6.algorithms.utils.SplitsUtilities;
import splitstree6.data.SplitsBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.data.TreesBlock;
import splitstree6.data.parts.ASplit;
import splitstree6.data.parts.Compatibility;

import java.io.IOException;
import java.util.List;

/**
 * implements consensus tree splits
 * <p>
 * Daniel Huson, 2.2018
 */
public class ConsensusSplits extends Trees2Splits {
	public enum Consensus {Majority, Strict, GreedyCompatible, GreedyCircular, GreedyWeaklyCompatible} // todo: add loose?

	private final SimpleObjectProperty<Consensus> optionConsensus = new SimpleObjectProperty<>(this, "optionConsensus", Consensus.Majority);
	private final SimpleObjectProperty<ConsensusNetwork.EdgeWeights> optionEdgeWeights = new SimpleObjectProperty<>(this, "optionEdgeWeights", ConsensusNetwork.EdgeWeights.TreeSizeWeightedMean);

	@Override
	public List<String> listOptions() {
		return List.of(optionConsensus.getName(), optionEdgeWeights.getName());
	}

	@Override
	public String getToolTip(String optionName) {
		if (optionName.equals(optionConsensus.getName()))
			return "Select consensus method";
		else if (optionName.equals(optionEdgeWeights.getName()))
			return "Determine how to calculate edge weights in resulting network";
		else
			return super.getToolTip(optionName);
	}

	/**
	 * compute the consensus splits
	 */
	public void compute(ProgressListener progress, TaxaBlock taxaBlock, TreesBlock treesBlock, SplitsBlock splitsBlock) throws IOException {
		final ConsensusNetwork consensusNetwork = new ConsensusNetwork();
		switch (getOptionConsensus()) {
			default -> consensusNetwork.setOptionThresholdPercent(50);
			case Majority -> consensusNetwork.setOptionThresholdPercent(50);
			case Strict -> consensusNetwork.setOptionThresholdPercent(99.999999); // todo: implement without use of splits
			case GreedyCompatible, GreedyCircular, GreedyWeaklyCompatible -> consensusNetwork.setOptionThresholdPercent(0);
		}

		final SplitsBlock consensusSplits = new SplitsBlock();
		consensusNetwork.setOptionEdgeWeights(getOptionEdgeWeights());
		consensusNetwork.setOptionHighDimensionFilter(false);
		consensusNetwork.compute(progress, taxaBlock, treesBlock, consensusSplits);

		splitsBlock.clear();

		switch (getOptionConsensus()) {
			case GreedyCompatible -> {
				splitsBlock.getSplits().addAll(GreedyCompatible.apply(progress, consensusSplits.getSplits(), ASplit::getConfidence));
				splitsBlock.setCompatibility(Compatibility.compatible);
				splitsBlock.setCycle(SplitsUtilities.computeCycle(taxaBlock.getNtax(), splitsBlock.getSplits()));
			}
			case GreedyCircular -> {
				splitsBlock.getSplits().addAll(GreedyCircular.apply(progress, taxaBlock.getTaxaSet(), consensusSplits.getSplits(), ASplit::getConfidence));
				splitsBlock.setCompatibility(Compatibility.compute(taxaBlock.getNtax(), splitsBlock.getSplits()));
				splitsBlock.setCycle(SplitsUtilities.computeCycle(taxaBlock.getNtax(), splitsBlock.getSplits()));
			}
			case GreedyWeaklyCompatible -> {
				splitsBlock.getSplits().addAll(GreedyWeaklyCompatible.apply(progress, consensusSplits.getSplits(), ASplit::getConfidence));
				splitsBlock.setCompatibility(Compatibility.compute(taxaBlock.getNtax(), splitsBlock.getSplits()));
				splitsBlock.setCycle(SplitsUtilities.computeCycle(taxaBlock.getNtax(), splitsBlock.getSplits()));
			}
			case Majority, Strict -> {
				splitsBlock.getSplits().clear();
				splitsBlock.getSplits().addAll(consensusSplits.getSplits());
				splitsBlock.setCompatibility(Compatibility.compatible);
				splitsBlock.setCycle(SplitsUtilities.computeCycle(taxaBlock.getNtax(), splitsBlock.getSplits()));
			}
		}
	}

	@Override
	public boolean isApplicable(TaxaBlock taxaBlock, TreesBlock parent) {
		return !parent.isPartial() && !parent.isReticulated();
	}

	public Consensus getOptionConsensus() {
		return optionConsensus.get();
	}

	public SimpleObjectProperty<Consensus> optionConsensusProperty() {
		return optionConsensus;
	}

	public void setOptionConsensus(Consensus optionConsensus) {
		this.optionConsensus.set(optionConsensus);
	}

	public void setOptionConsensus(String optionConsensus) {
		this.optionConsensus.set(Consensus.valueOf(optionConsensus));
	}

	public ConsensusNetwork.EdgeWeights getOptionEdgeWeights() {
		return optionEdgeWeights.get();
	}

	public SimpleObjectProperty<ConsensusNetwork.EdgeWeights> optionEdgeWeightsProperty() {
		return optionEdgeWeights;
	}

	public void setOptionEdgeWeights(ConsensusNetwork.EdgeWeights optionEdgeWeights) {
		this.optionEdgeWeights.set(optionEdgeWeights);
	}
}
