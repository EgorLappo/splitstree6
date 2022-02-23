/*
 * TreeSelectorSplits.java Copyright (C) 2022 Daniel H. Huson
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

package splitstree6.algorithms.splits.splits2splits;

import javafx.beans.property.*;
import jloda.fx.util.ProgramExecutorService;
import jloda.util.Pair;
import jloda.util.Single;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressSilent;
import splitstree6.algorithms.trees.trees2splits.TreeSelectorSplits;
import splitstree6.algorithms.utils.SplitsUtilities;
import splitstree6.data.CharactersBlock;
import splitstree6.data.SplitsBlock;
import splitstree6.data.TaxaBlock;
import splitstree6.data.TreesBlock;
import splitstree6.data.parts.ASplit;
import splitstree6.data.parts.Compatibility;
import splitstree6.workflow.Algorithm;
import splitstree6.workflow.DataBlock;
import splitstree6.workflow.DataNode;
import splitstree6.workflow.Workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * bootstrap splits to get bootstrap splits
 *
 * @author Daniel Huson, 2.2022
 */
public class BootstrapSplits extends Splits2Splits {
	private final IntegerProperty optionReplicates = new SimpleIntegerProperty(this, "optionReplicates", 100);
	private final DoubleProperty optionMinPercent = new SimpleDoubleProperty(this, "optionMinPercent", 10.0);
	private final BooleanProperty optionShowAllSplits = new SimpleBooleanProperty(this, "optionShowAllSplits", false);
	private final IntegerProperty optionRandomSeed = new SimpleIntegerProperty(this, "optionRandomSeed", 42);

	@Override
	public List<String> listOptions() {
		return List.of(optionReplicates.getName(), optionMinPercent.getName(), optionShowAllSplits.getName(), optionRandomSeed.getName());
	}

	@Override
	public String getToolTip(String optionName) {
		if (optionName.equals(optionReplicates.getName())) {
			return "Number of bootstrap replicates";
		} else if (optionName.equals(optionShowAllSplits.getName())) {
			return "Show all bootstrap splits, not just the original splits";
		} else if (optionName.equals(optionMinPercent.getName())) {
			return "Minimum percentage support for a split to be included";
		} else if (optionName.equals(optionRandomSeed.getName())) {
			return "If non-zero, is used as seed for random number generator";
		}
		return optionName;
	}

	@Override
	public void compute(ProgressListener progress, TaxaBlock taxaBlock, SplitsBlock inputSplits, SplitsBlock splitsBlock) throws IOException {
		compute(progress, taxaBlock, inputSplits, inputSplits.getNode(), splitsBlock);
	}

	public void compute(ProgressListener progress, TaxaBlock taxaBlock, SplitsBlock inputSplits, DataNode targetNode, SplitsBlock splitsBlock) throws IOException {

		setOptionReplicates(Math.max(1, optionReplicates.get()));

		setShortDescription(String.format("bootstrapping using %d replicates", getOptionReplicates()));

		// figure out the pipeline:
		var workflow = (Workflow) taxaBlock.getNode().getOwner();

		var splitCountMap = new ConcurrentHashMap<ASplit, Integer>();
		var splitWeightMap = new ConcurrentHashMap<ASplit, Double>();

		for (var split : inputSplits.getSplits()) {
			splitCountMap.put(new ASplit(split), 0);
		}

		if (workflow.getWorkingDataNode().getDataBlock() instanceof CharactersBlock charactersBlock) {
			if (charactersBlock.isDiploid())
				throw new IOException("Bootstrapping not implemented for diploid data, if you need this, please contact the authors!");

			var seeds = new int[getOptionReplicates()];
			{
				var random = getOptionRandomSeed() == 0 ? new Random() : new Random(getOptionRandomSeed());
				for (var i = 0; i < seeds.length; i++)
					seeds[i] = random.nextInt();
			}

			var numberOfThreads = Math.max(1, Math.min(getOptionReplicates(), ProgramExecutorService.getNumberOfCoresToUse()));
			var service = Executors.newFixedThreadPool(numberOfThreads);
			var exception = new Single<IOException>();

			progress.setMaximum(getOptionReplicates() / numberOfThreads);
			progress.setProgress(0);

			for (var t = 0; t < numberOfThreads; t++) {
				var thread = t;

				service.execute(() -> {
					try {
						var path = extractPath(workflow.getWorkingDataNode(), targetNode);
						if (thread == 0)
							System.err.println("Bootstrap workflow: " + toString(charactersBlock, path));

						if (targetNode.getDataBlock() instanceof TreesBlock) {
							path.add(new Pair<>(new TreeSelectorSplits(), new SplitsBlock()));
						} else
							path.get(path.size() - 1).setSecond(new SplitsBlock());

						for (var r = thread; r < getOptionReplicates(); r += numberOfThreads) {
							SplitsBlock replicateSplits = (SplitsBlock) run(new ProgressSilent(), workflow.getWorkingTaxaBlock(), createReplicate(charactersBlock, new Random(seeds[r])), path);
							for (var split : replicateSplits.getSplits()) {
								if (isOptionShowAllSplits() || splitCountMap.containsKey(split)) {
									splitCountMap.put(split, splitCountMap.getOrDefault(split, 0) + 1);
									splitWeightMap.put(split, splitWeightMap.getOrDefault(split, 0.0) + split.getWeight());
								}
							}
							if (thread == 0)
								progress.incrementProgress();
							if (exception.isNotNull())
								return;
						}
					} catch (IOException ex) {
						exception.setIfCurrentValueIsNull(ex);
					}
				});
			}
			progress.reportTaskCompleted();

			service.shutdown();
			try {
				service.awaitTermination(1000, TimeUnit.DAYS);
			} catch (InterruptedException ignored) {
			}
			if (exception.isNotNull())
				throw exception.get();

			for (var split : splitCountMap.keySet()) {
				var count = splitCountMap.getOrDefault(split, 0);
				if (count > 0) {
					var percent = 100.0 * ((double) count / (double) getOptionReplicates());
					if (percent >= getOptionMinPercent()) {
						var totalWeight = splitWeightMap.getOrDefault(split, 0.0);
						if (totalWeight > 0) {
							split.setWeight(totalWeight / count);
							split.setConfidence(percent);
							splitsBlock.getSplits().add(split);
						}
					}
				}
			}
		}

		SplitsUtilities.addAllTrivial(taxaBlock.getNtax(), splitsBlock);

		SplitsUtilities.verifySplits(splitsBlock.getSplits(), taxaBlock);

		splitsBlock.setCycle(SplitsUtilities.computeCycle(taxaBlock.getNtax(), splitsBlock.getSplits()));
		splitsBlock.setFit(-1);
		splitsBlock.setCompatibility(Compatibility.compute(taxaBlock.getNtax(), splitsBlock.getSplits(), splitsBlock.getCycle()));

		splitsBlock.getFormat().setOptionConfidences(true);
	}

	/**
	 * creates a bootstrap replicate
	 *
	 * @param charactersBlock characters
	 * @param random          random number generator
	 * @return bootstrap replicate
	 */
	public static CharactersBlock createReplicate(CharactersBlock charactersBlock, Random random) {
		final var srcMatrix = charactersBlock.getMatrix();
		final var numRows = srcMatrix.length;
		final var numCols = srcMatrix[0].length;
		final var tarMatrix = new char[numRows][numCols];
		for (var col = 0; col < numCols; col++) {
			var randomCol = random.nextInt(numCols);
			for (var row = 0; row < numRows; row++) {
				tarMatrix[row][col] = srcMatrix[row][randomCol];
			}
		}
		return new CharactersBlock(charactersBlock, tarMatrix);
	}

	@Override
	public boolean isApplicable(TaxaBlock taxa, SplitsBlock datablock) {
		var workflow = (Workflow) datablock.getNode().getOwner();
		var workingDataBlock = workflow.getWorkingDataNode().getDataBlock();
		return workingDataBlock instanceof CharactersBlock;
	}

	public int getOptionReplicates() {
		return optionReplicates.get();
	}

	public IntegerProperty optionReplicatesProperty() {
		return optionReplicates;
	}

	public void setOptionReplicates(int optionReplicates) {
		this.optionReplicates.set(optionReplicates);
	}

	public double getOptionMinPercent() {
		return optionMinPercent.get();
	}

	public DoubleProperty optionMinPercentProperty() {
		return optionMinPercent;
	}

	public void setOptionMinPercent(double optionMinPercent) {
		this.optionMinPercent.set(optionMinPercent);
	}

	public boolean isOptionShowAllSplits() {
		return optionShowAllSplits.get();
	}

	public BooleanProperty optionShowAllSplitsProperty() {
		return optionShowAllSplits;
	}

	public void setOptionShowAllSplits(boolean optionShowAllSplits) {
		this.optionShowAllSplits.set(optionShowAllSplits);
	}

	public int getOptionRandomSeed() {
		return optionRandomSeed.get();
	}

	public IntegerProperty optionRandomSeedProperty() {
		return optionRandomSeed;
	}

	public void setOptionRandomSeed(int optionRandomSeed) {
		this.optionRandomSeed.set(optionRandomSeed);
	}

	/**
	 * get's the path of algorithms and datanodes from the working datanode to the target datanode
	 *
	 * @param workingDataNode
	 * @param target
	 * @return
	 */
	public static ArrayList<Pair<Algorithm, DataBlock>> extractPath(DataNode<? extends DataBlock> workingDataNode, DataNode target) throws IOException {
		var list = new ArrayList<Pair<Algorithm, DataBlock>>();

		var dataNode = target;
		while (dataNode != workingDataNode) {
			if (dataNode.getPreferredParent() == null)
				throw new IOException("Algorithm path not found");
			var algorithmNode = dataNode.getPreferredParent();
			list.add(0, new Pair<>(algorithmNode.getAlgorithm(), dataNode.getDataBlock().newInstance()));
			dataNode = algorithmNode.getPreferredParent();
		}
		return list;
	}

	/**
	 * run all algorithms in the path on the given characters
	 *
	 * @param progress
	 * @param taxa
	 * @param characters
	 * @param path
	 * @return the final datablock that is computed
	 * @throws IOException
	 */
	public static DataBlock run(ProgressListener progress, TaxaBlock taxa, CharactersBlock characters, Collection<Pair<Algorithm, DataBlock>> path) throws IOException {
		DataBlock inputData = characters;
		for (var pair : path) {
			var algorithm = pair.getFirst();
			var outputData = pair.getSecond();
			if (true || algorithm.isApplicable(taxa, inputData)) {
				outputData.clear();
				algorithm.compute(progress, taxa, inputData, outputData);
			}
			inputData = outputData;
		}
		return inputData;
	}

	/**
	 * return a overview of path
	 *
	 * @param characters input characters
	 * @param path       path of algorithms and data
	 * @return string
	 */
	public static String toString(CharactersBlock characters, ArrayList<Pair<Algorithm, DataBlock>> path) {
		var buf = new StringBuilder();

		DataBlock inputData = characters;
		buf.append(inputData.getBlockName());

		for (var pair : path) {
			var algorithm = pair.getFirst();
			var outputData = pair.getSecond();
			buf.append(" -> ").append(algorithm.getName()).append(" -> ").append(inputData.getBlockName());
			inputData = outputData;
		}
		return buf.toString();
	}
}
