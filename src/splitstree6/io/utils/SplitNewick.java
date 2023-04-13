/*
 *  SplitNewick.java Copyright (C) 2023 Daniel H. Huson
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

package splitstree6.io.utils;

import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloTree;
import jloda.phylo.algorithms.ClusterPoppingAlgorithm;
import jloda.util.*;
import splitstree6.algorithms.utils.SplitsUtilities;
import splitstree6.algorithms.utils.TreesUtilities;
import splitstree6.data.parts.ASplit;
import splitstree6.data.parts.Compatibility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static splitstree6.algorithms.utils.SplitsUtilities.isCompatibleWithOrdering;

/**
 * Newick IO for splits
 * Daniel Huson, 3.2023
 */
public class SplitNewick {

	/**
	 * reads a string in SplitNewick format
	 *
	 * @param r             the input reader
	 * @param labelTaxonMap the label taxon map to use. If null, we setup new mapping
	 * @param taxonLabelMap if non-null and empty, will return the taxon to label mapping here
	 * @return the collection of splits obtained
	 * @throws IOException
	 */

	public static ArrayList<ASplit> read(Reader r, Map<String, Integer> labelTaxonMap, Map<Integer, String> taxonLabelMap) throws IOException {
		BufferedReader br;
		if (r instanceof BufferedReader) {
			br = (BufferedReader) r;
		} else {
			br = new BufferedReader(r);
		}
		return parse(br.readLine(), labelTaxonMap, taxonLabelMap);
	}

	/**
	 * parses a string in SplitNewick format
	 *
	 * @param newickString  the input newick string
	 * @param labelTaxonMap the label taxon map to use. If null, we setup new mapping
	 * @param taxonLabelMap if non-null and empty, will return the taxon to label mapping here
	 * @return the collection of splits obtained
	 * @throws IOException
	 */
	public static ArrayList<ASplit> parse(String newickString, Map<String, Integer> labelTaxonMap, Map<Integer, String> taxonLabelMap) throws IOException {
		var hasSplits = Pattern.compile("<[0-9]+|").matcher(newickString).find();

		String treeNewick;
		if (hasSplits)
			treeNewick = newickString.replaceAll("<[0-9]+\\|", "").replaceAll("\\|[0-9]+(:[0-9eE.-]+)?(:[0-9eE.-]+)?(:[0-9eE.-]+)?>", "");
		else
			treeNewick = newickString;
		// System.err.println("treeNewick in: " + treeNewick);

		var tree = new PhyloTree();
		tree.parseBracketNotation(treeNewick, true, true);
		if (labelTaxonMap == null) {
			var nTax = 0;
			labelTaxonMap = new HashMap<>();
			for (var v : tree.leaves()) {
				labelTaxonMap.put(tree.getLabel(v), ++nTax);
			}
		}
		for (var v : tree.leaves()) {
			tree.addTaxon(v, labelTaxonMap.get(tree.getLabel(v)));
		}
		var nTax = IteratorUtils.asStream(tree.getTaxa()).mapToInt(t -> t).max().orElse(0);

		var splits = new ArrayList<ASplit>();
		TreesUtilities.computeSplits(BitSetUtils.asBitSet(tree.getTaxa()), tree, splits);

		if (hasSplits) {
			var pos2tax = new TreeMap<Integer, Integer>();
			{
				// here we surround the taxon pattern by (?=( and )) to allow overlapping matches to get both a and b in a,b
				var matcher = Pattern.compile("(?=([(,|]('{0,1}[a-zA-z]+[^#<>(:,)|]*)[:,|)]))").matcher(newickString);
				while (matcher.find()) {
					var label = matcher.group(2);
					if (label.startsWith("'") && label.endsWith("'") && label.length() > 1)
						label = label.substring(1, label.length() - 1);
					var t = labelTaxonMap.get(label);
					pos2tax.put(matcher.start(2), t);
				}
			}

			var ids = new TreeSet<Integer>();
			{
				var matcher = Pattern.compile("<(\\d+)|").matcher(newickString);
				while (matcher.find()) {
					var label = matcher.group(1);
					if (label != null)
						ids.add(Integer.parseInt(label));
				}
			}
			{
				for (var id : ids) {
					var set = new BitSet();
					{
						var matcher = Pattern.compile("(<%d\\|)|(\\|%d>|\\|%d:[0-9.eE-]+>)".formatted(id, id, id)).matcher(newickString);
						var start = -1;
						while (matcher.find()) {
							if (matcher.group(1) != null) {
								start = matcher.start(1);
							} else if (matcher.group(2) != null) {
								var end = matcher.end(2);
								for (var pos : pos2tax.keySet()) {
									if (pos >= start && pos <= end)
										set.set(pos2tax.get(pos));
								}
							}
						}
					}
					var split = new ASplit(set, nTax);
					{
						var weight = Double.MIN_VALUE;
						var confidence = Double.MIN_VALUE;
						var probability = Double.MIN_VALUE; // not used (yet?)
						var matcher = Pattern.compile("\\|%d(:[0-9.-eE]+)?(:[0-9.-eE]+)?(:[0-9.-eE]+)?>".formatted(id)).matcher(newickString);
						while (matcher.find()) {
							if (matcher.group(1) != null) {
								weight = Double.parseDouble(matcher.group(1).substring(1));
								if (matcher.group(2) != null) {
									confidence = Double.parseDouble(matcher.group(2).substring(1));
									if (matcher.group(3) != null) {
										probability = Double.parseDouble(matcher.group(3).substring(1));
									}
								}
							}
						}
						if (weight != Double.MIN_VALUE)
							split.setWeight(weight);
						if (confidence != Double.MIN_VALUE)
							split.setConfidence(confidence);
					}
					splits.add(split);
				}
			}
		}
		if (taxonLabelMap != null && taxonLabelMap.isEmpty()) {
			for (var entry : labelTaxonMap.entrySet()) {
				taxonLabelMap.put(entry.getValue(), entry.getKey());
			}
		}
		return splits;
	}

	public static ArrayList<ASplit> extractSplits(PhyloSplitsGraph graph) {
		var id2cluster = new HashMap<Integer, BitSet>();
		extractSplitsRec(graph.getTaxon2Node(1), 0, new BitSet(), id2cluster);
		var taxa = BitSetUtils.asBitSet(graph.getTaxa());
		var split2weight = new HashMap<Integer, Double>();
		for (var e : graph.edges()) {
			var splitId = graph.getSplit(e);
			if (!split2weight.containsKey(splitId))
				split2weight.put(splitId, graph.getWeight(e));
		}
		return id2cluster.entrySet().stream().map(e -> new ASplit(BitSetUtils.minus(taxa, e.getValue()), e.getValue(), split2weight.get(e.getKey()))).collect(Collectors.toCollection(ArrayList::new));
	}

	private static void extractSplitsRec(Node v, int splitId, BitSet used, HashMap<Integer, BitSet> id2cluster) {
		var graph = (PhyloSplitsGraph) v.getOwner();
		if (splitId > 0) {
			used.set(splitId);
			if (graph.hasTaxa(v)) {
				id2cluster.computeIfAbsent(splitId, k -> new BitSet()).set(graph.getTaxon(v));
			}
		}
		for (var e : v.adjacentEdges()) {
			var eSplitId = graph.getSplit(e);
			if (eSplitId != splitId) {
				extractSplitsRec(e.getOpposite(v), eSplitId, used, id2cluster);
			}
		}
		if (splitId > 0) {
			used.clear(splitId);
		}
	}

	/**
	 * write a collection of splits in SplitsNewick format
	 *
	 * @param splits the splits
	 * @param w      writer
	 */
	public static void write(Function<Integer, String> taxonLabelFunction, List<ASplit> splits, boolean includeWeights, Writer w) throws IOException {
		write(taxonLabelFunction, splits, includeWeights, null, w);
	}

	/**
	 * write a collection of splits in SplitsNewick format, using the given taxon ordering
	 *
	 * @param splits   splits
	 * @param ordering the taxon ordering - using a good ordering will result in a shorter Newick string
	 * @param w        writer
	 */
	public static void write(Function<Integer, String> taxonLabelFunction, List<ASplit> splits, boolean includeWeights, ArrayList<Integer> ordering, Writer w) throws IOException {
		w.write(toString(taxonLabelFunction, splits, includeWeights, ordering));
	}

	public static String toString(Function<Integer, String> taxonLabelFunction, List<ASplit> splits, boolean includeWeights) throws IOException {
		return toString(taxonLabelFunction, splits, includeWeights, (ArrayList<Integer>) null);
	}

	public static String toString(Function<Integer, String> taxonLabelFunction, List<ASplit> splits, boolean includeWeights, int[] cycle1based) throws IOException {
		var ordering = new ArrayList<Integer>();
		for (int i = 1; i < cycle1based.length; i++)
			ordering.add(cycle1based[i]);
		return toString(taxonLabelFunction, splits, includeWeights, ordering);
	}


	/**
	 * get string in SplitsNewick format, using the given taxon ordering
	 *
	 * @param splits   splits
	 * @param ordering the taxon ordering - using a good ordering will result in a shorter Newick string
	 */
	public static String toString(Function<Integer, String> taxonLabelFunction0, List<ASplit> splits, boolean includeWeights, ArrayList<Integer> ordering) throws IOException {
		if (splits.size() == 0)
			return "";
		else {
			Function<Integer, String> taxonLabelFunction;
			if (true)
				taxonLabelFunction = taxonLabelFunction0;
			else if (false)
				taxonLabelFunction = t -> "t" + t;
			else if (false)
				taxonLabelFunction = t -> taxonLabelFunction0.apply(t).replaceAll(" ", "_");

			var nTax = splits.get(0).getAllTaxa().cardinality();
			if (ordering == null) {
				ordering = new ArrayList<>();
				var cycle1based = SplitsUtilities.computeCycle(splits.get(0).getAllTaxa().cardinality(), splits);
				for (var i = 1; i < cycle1based.length; i++)
					ordering.add(cycle1based[i]);
			}
			var compatible = new ArrayList<ASplit>();
			var additional = new ArrayList<ASplit>();
			for (var split : splits) {
				if (split.isTrivial() || isCompatibleWithOrdering(split.getPartNotContaining(ordering.get(0)), ordering) && Compatibility.isCompatible(split, compatible)) {
					compatible.add(split);
				} else {
					additional.add(split);
				}
			}

			// System.err.println("Ordering:" + StringUtils.toString(ordering.stream().map(taxonLabel).collect(Collectors.toList()), ","));

			var taxonRank = new HashMap<Integer, Integer>();
			for (int rank = 0; rank < ordering.size(); rank++) {
				taxonRank.put(ordering.get(rank), rank);
			}
			var tree = new PhyloTree();
			var treeClusters = new ArrayList<BitSet>();
			var clusterMap = new HashMap<BitSet, Pair<Double, Double>>();
			for (var split : compatible) {
				var cluster = split.getPartNotContaining(ordering.get(0));
				treeClusters.add(cluster);
				clusterMap.put(cluster, new Pair<>(split.getWeight(), split.getConfidence()));
				if (split.getPartContaining(ordering.get(0)).cardinality() == 1) {
					var other = split.getPartContaining(ordering.get(0));
					treeClusters.add(other);
					clusterMap.put(other, new Pair<>(0.0, split.getConfidence()));
				}
			}
			ClusterPoppingAlgorithm.apply(treeClusters, c -> clusterMap.get(c).getFirst(), c -> clusterMap.get(c).getSecond(), tree);
			tree.leaves().forEach(v -> tree.setLabel(v, taxonLabelFunction.apply(tree.getTaxon(v))));

			try (NodeArray<Integer> node2rank = tree.newNodeArray()) {
				for (var entry : TreesUtilities.extractClusters(tree).entrySet()) {
					var rank = BitSetUtils.asStream(entry.getValue()).mapToInt(taxonRank::get).min().orElse(0);
					node2rank.put(entry.getKey(), rank);
				}

				for (var v : tree.nodes()) {
					var outEdges = IteratorUtils.asList(v.outEdges());
					outEdges.sort(Comparator.comparingInt(a -> node2rank.get(a.getTarget())));
					var all = CollectionUtils.concatenate(IteratorUtils.asList(v.inEdges()), outEdges);
					v.rearrangeAdjacentEdges(all);
				}
			}
			if (false) {
				System.err.println("Tree taxon ordering:");
				tree.postorderTraversal(v -> {
					if (v.isLeaf())
						System.err.print(" " + tree.getTaxon(v));
				});
				System.err.println();
			}

			var treeNewick = tree.toBracketString(includeWeights);

			if (false)
				System.err.println("TreeNewick out: " + treeNewick);

			if (additional.size() == 0)
				return treeNewick;
			else { // insert other splits
				var taxaList = new ArrayList<Integer>();
				var taxon2StartEnd = new int[nTax + 1][2];
				{
					var labelTaxonMap = new HashMap<String, Integer>();
					for (var t : BitSetUtils.members(splits.get(0).getAllTaxa())) {
						labelTaxonMap.put(taxonLabelFunction.apply(t), t);
					}
					var matcher = Pattern.compile("[(,]([^#<|>(:,)]+)").matcher(treeNewick);
					while (matcher.find()) {
						var label = matcher.group(1);
						if (label.startsWith("'") && label.endsWith("'") && label.length() > 1) {
							label = label.substring(1, label.length() - 1);
						}
						//System.err.println(label);
						var t = labelTaxonMap.get(label);
						taxaList.add(t);
						taxon2StartEnd[t][0] = matcher.start(1);
						taxon2StartEnd[t][1] = matcher.end(1);
					}
				}

				var insertBeforeTaxon = new StringBuilder[nTax + 1];
				var appendAfterTaxon = new StringBuilder[nTax + 1];

				var splitNumber = 0;
				for (var split : additional) {
					splitNumber++;
					var inside = false;
					var count = 0;
					var set = split.getPartNotContaining(ordering.get(0));
					var prev = 0;
					for (var t : taxaList) {
						if (set.get(t)) {
							if (!inside) {
								inside = true;
								if (insertBeforeTaxon[t] == null)
									insertBeforeTaxon[t] = new StringBuilder();
								insertBeforeTaxon[t].append("<%d|".formatted(splitNumber));
							}
							count++;
							if (count == set.cardinality()) {
								if (appendAfterTaxon[t] == null)
									appendAfterTaxon[t] = new StringBuilder();
								if (!includeWeights) {
									appendAfterTaxon[t].append("|%d>".formatted(splitNumber));
								} else if (split.getConfidence() <= 0)
									appendAfterTaxon[t].append("|%d:%s>".formatted(splitNumber,
											StringUtils.removeTrailingZerosAfterDot("%.8f", split.getWeight())));
								else
									appendAfterTaxon[t].append("|%d:%s:%s>".formatted(splitNumber,
											StringUtils.removeTrailingZerosAfterDot("%.8f", split.getWeight()),
											StringUtils.removeTrailingZerosAfterDot("%.8f", split.getConfidence())));
							}
						} else {
							if (inside) {
								if (count < set.cardinality()) {
									if (appendAfterTaxon[prev] == null)
										appendAfterTaxon[prev] = new StringBuilder();
									appendAfterTaxon[prev].append("|%d>".formatted(splitNumber));
								}
								inside = false;
							}
						}
						prev = t;
					}
				}

				var buf = new StringBuilder();
				var prev = 0;
				for (var t : taxaList) {
					var tStart = taxon2StartEnd[t][0];
					var tEnd = taxon2StartEnd[t][1];
					buf.append(treeNewick, prev, tStart);
					if (insertBeforeTaxon[t] != null)
						buf.append(insertBeforeTaxon[t].toString());
					buf.append(treeNewick, tStart, tEnd);
					if (appendAfterTaxon[t] != null)
						buf.append(appendAfterTaxon[t]);
					prev = tEnd;
				}
				buf.append(treeNewick.substring(prev));

				var result = buf.toString();

				if (true) {
					var labelTaxonMap = new HashMap<String, Integer>();
					for (var t : BitSetUtils.members(splits.get(0).getAllTaxa())) {
						labelTaxonMap.put(taxonLabelFunction.apply(t), t);
					}
					var backSplits = parse(result, labelTaxonMap, null);
					{
						var lines = new TreeSet<String>();
						for (var split : splits) {
							if (!backSplits.contains(split))
								lines.add("%s: %s".formatted(StringUtils.toString(split.getPartNotContaining(ordering.get(0))), StringUtils.removeTrailingZerosAfterDot("%.8f", split.getWeight())));
						}
						if (lines.size() > 0) {
							System.err.println("In input, not in output: " + lines.size());
							System.err.println(StringUtils.toString(lines, "\n"));
						}
					}
					{
						var lines = new TreeSet<String>();
						for (var split : backSplits) {
							if (!splits.contains(split))
								lines.add("%s: %s".formatted(StringUtils.toString(split.getPartNotContaining(ordering.get(0))), StringUtils.removeTrailingZerosAfterDot("%.8f", split.getWeight())));
						}
						if (lines.size() > 0) {
							System.err.println("In output, not in input: " + lines.size());
							System.err.println(StringUtils.toString(lines, "\n"));
						}
					}
				}

				return result;
			}
		}
	}

	/**
	 * writes a split graph in SplitsNewick format
	 *
	 * @param graph split graph
	 * @param w     writer
	 */
	public static void write(PhyloSplitsGraph graph, boolean includeWeights, Writer w) throws IOException {
		write(t -> graph.getLabel(graph.getTaxon2Node(t)), extractSplits(graph), includeWeights, null, w);
	}

	/**
	 * writes a split graph in SplitsNewick format, using the given taxon ordering
	 *
	 * @param graph    split graph
	 * @param ordering the taxon ordering
	 * @param w        writer
	 */
	public static void write(PhyloSplitsGraph graph, boolean includeWeights, ArrayList<Integer> ordering, Writer w) throws IOException {
		write(t -> graph.getLabel(graph.getTaxon2Node(t)), extractSplits(graph), includeWeights, ordering, w);
	}
}