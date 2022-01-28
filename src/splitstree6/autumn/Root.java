/*
 * Root.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree6.autumn;


import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.Single;
import jloda.util.StringUtils;
import splitstree6.data.TaxaBlock;

import java.util.*;

/**
 * Root of a rooted tree or network inside a larger Graph
 * Daniel Huson, 4.2011
 */
public class Root extends Node {
	final private BitSet taxa = new BitSet();
	final private BitSet removedTaxa = new BitSet();

	/**
	 * constructor a new root inside an existing graph
	 *
	 * @param graph
	 */
	public Root(Graph graph) {
		super(graph);
	}

	/**
	 * constructor a new root inside an existing graph and set the taxa associated with it
	 *
	 * @param graph
	 * @param taxa
	 */
	public Root(Graph graph, BitSet taxa) {
		super(graph);
		setTaxa(taxa);
	}

	/**
	 * this this root and the whole subtree or network that hangs below it
	 */
	public void deleteSubTree() {
		deleteSubTreeBelow();
		getOwner().deleteNode(this);
	}

	/**
	 * delete the subtree below this root
	 */
	public void deleteSubTreeBelow() {
		Set<Node> nodes = getAllNodesBelow();
		for (Node v : nodes)
			getOwner().deleteNode(v);
	}

	/**
	 * get all nodes below this one
	 *
	 * @return all nodes below
	 */
	public Set<Node> getAllNodesBelow() {
		Set<Node> nodes = new HashSet<>();
		getAllNodesBelowRec(this, nodes);
		return nodes;
	}

	/**
	 * get all nodes below v
	 *
	 * @param v
	 */
	private void getAllNodesBelowRec(Node v, Set<Node> nodes) {
		for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
			Node w = e.getTarget();
			if (!nodes.contains(w)) {
				nodes.add(w);
				getAllNodesBelowRec(w, nodes);
			}
		}
	}

	/**
	 * sets the taxa associated with this node
	 *
	 * @param taxa
	 */
	public void setTaxa(BitSet taxa) {
		this.taxa.clear();
		this.taxa.or(taxa);
	}

	/**
	 * gets the taxa for this node
	 *
	 * @return taxa
	 */
	public BitSet getTaxa() {
		return taxa;
	}


	/**
	 * sets the removed taxa associated with this node
	 *
	 * @param taxa
	 */
	public void setRemovedTaxa(BitSet taxa) {
		this.removedTaxa.clear();
		this.removedTaxa.or(taxa);
	}

	/**
	 * gets the removed taxa for this node
	 *
	 * @return taxa
	 */
	public BitSet getRemovedTaxa() {
		return removedTaxa;
	}

	/**
	 * creates a copy of a rooted tree. Sets the taxa for each node
	 *
	 * @param graph   graph that contains the rooted tree
	 * @param tree    input tree
	 * @param allTaxa
	 * @return root of copy
	 */
	public static Root createACopy(Graph graph, PhyloTree tree, TaxaBlock allTaxa) {
		Root root = new Root(graph);
		copyRec(tree.getRoot(), root, allTaxa);
		return root;
	}

	/**
	 * recursively does the work
	 *
	 * @param v1
	 * @param v2
	 * @param allTaxa
	 */
	public static void copyRec(Node v1, Root v2, TaxaBlock allTaxa) {
		BitSet taxa = new BitSet();
		if (v1.getOutDegree() == 0) { // is at a leaf, grab the taxon name
			int id = allTaxa.indexOf(((PhyloTree) v1.getOwner()).getLabel(v1));
			taxa.set(id);
		} else {
			for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
				Node w1 = e1.getTarget();
				Root w2 = v2.newNode();
				Edge f = w2.getOwner().newEdge(v2, w2);
				f.setInfo(e1.getInfo());
				copyRec(w1, w2, allTaxa);
				taxa.or(w2.getTaxa());
			}
		}

		v2.setTaxa(taxa);
	}

	/**
	 * create a new node
	 *
	 * @return new node
	 */
	public Root newNode() {
		return new Root(getOwner());
	}

	/**
	 * creates a new node and sets its taxa
	 *
	 * @param taxa
	 * @return
	 */
	public Root newNode(BitSet taxa) {
		Root v = new Root(getOwner());
		v.setTaxa(taxa);
		return v;
	}

	/**
	 * creates a new edge
	 *
	 * @param v
	 * @param w
	 * @return new edge
	 */
	public Edge newEdge(Node v, Node w) {
		return getOwner().newEdge(v, w);
	}

	/**
	 * deletes an edge in the graph containing this node
	 *
	 * @param e
	 */
	public void deleteEdge(Edge e) {
		getOwner().deleteEdge(e);
	}

	/**
	 * gets all leaves below this node  in order of occurrence
	 *
	 * @return all leaves
	 */
	public List<Root> getAllLeaves() {
        List<Root> result = new LinkedList<>();
		getAllLeavesRec(this, result);
		return result;
	}

	/**
	 * recursively does the work
	 *
	 * @param v
	 * @param result
	 */
	private void getAllLeavesRec(Root v, List<Root> result) {
		for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
			Root w = ((Root) e.getTarget());
			if (w.getOutDegree() == 0)
				result.add(w);
			else
				getAllLeavesRec(w, result);
		}
	}

	/**
	 * print this node
	 *
	 * @return string
	 */
	public String toString() {
        StringBuilder buf = new StringBuilder();
		buf.append("node indeg=").append(getInDegree()).append(" outdeg=").append(getOutDegree()).append(" taxa=").append(getTaxaString());
		return buf.toString();
	}

	/**
	 * print tree in bracket format in sparsest format
	 *
	 * @return tree in bracket format
	 */
	public String toStringTreeSparse() {
		StringBuffer buf = new StringBuffer();
		toStringTreeRec(buf, false, true, false);
		return buf.toString();
	}

	/**
	 * print tree in bracket format. Put an x before taxa numbers for Newick compatibility
	 *
	 * @return tree in bracket format
	 */
	public String toStringFullTreeX() {
		StringBuffer buf = new StringBuffer();
		toStringTreeRec(buf, true, false, true);
		checkTreeRec(this, buf.toString());
        return buf + ";";
	}

	/**
	 * print tree in bracket format.
	 *
	 * @return tree in bracket format
	 */
	public String toStringTree() {
		StringBuffer buf = new StringBuffer();
		toStringTreeRec(buf, false, false, false);
		checkTreeRec(this, buf.toString());
        return buf + ";";
	}

	/**
	 * recursively do the work
	 *
	 * @param buf
	 * @param labelInternalNodes
	 * @param oneTaxonPerLeaf
	 */
	private void toStringTreeRec(StringBuffer buf, boolean labelInternalNodes, boolean oneTaxonPerLeaf, boolean showX) {

		if (getOutDegree() == 0) {
			if (showX)
				buf.append("x");
			if (oneTaxonPerLeaf)
				buf.append(getTaxa().nextSetBit(0));
			else
				buf.append(getTaxaString());
		} else {
			buf.append("(");
			for (Edge e = getFirstOutEdge(); e != null; e = getNextOutEdge(e)) {
				if (e != getFirstOutEdge()) {
					buf.append(",");
				}
				((Root) e.getTarget()).toStringTreeRec(buf, labelInternalNodes, oneTaxonPerLeaf, showX);
			}
			buf.append(")");
			if (labelInternalNodes) {
				if (showX)
					buf.append("x");
				buf.append(getTaxaString());
			}
		}
	}

	/**
	 * check that nodes are consistently labeled by taxa and removedTaxa
	 *
	 * @param root
	 */
	private void checkTreeRec(Root root, String string) {
		if (root.getOutDegree() > 0) {
			BitSet taxa = new BitSet();
			BitSet removed = new BitSet();
			for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
				Root w = (Root) e.getTarget();
				checkTreeRec(w, string);
				taxa.or(w.getTaxa());
				removed.or(w.getRemovedTaxa());
			}
			if (root.getInDegree() == 1 && root.getOutDegree() == 1)
				throw new RuntimeException("Node with indegree=outdegree=1: " + string + ";");
			if (!root.getTaxa().equals(taxa))
				throw new RuntimeException("Taxa discrepancy: " + string + ";");
			if (!root.getRemovedTaxa().equals(removed))
                throw new RuntimeException("Removed-taxa discrepancy at " + root + ": " + string + ";"
                        + "\nExpected(root): " + StringUtils.toString(root.getRemovedTaxa()) + ", got(below): " + StringUtils.toString(removed));
			if (root.getOutDegree() == 0 && root.getTaxa().cardinality() == 0 && root.getInDegree() == 1 && root.getFirstInEdge().getInfo() == null)
				throw new RuntimeException("In-edge without tree id:: " + string + ";");
		}
	}

	/**
	 * print as network in bracket format (with full information)
	 *
	 * @return network in bracket format
	 */
	public String toStringNetworkFull() {
		StringBuffer buf = new StringBuffer();
		if (getOutDegree() == 0) {
			buf.append("(").append(getTaxaString()).append(")");
		} else {
            toStringNetworkRec(buf, true, false, new HashMap<>(), new Single<>(0));
		}
        return buf + ";";
	}

	/**
	 * print as network in bracket format
	 *
	 * @return network in bracket format
	 */
	public String toStringNetwork() {
		StringBuffer buf = new StringBuffer();
		if (getOutDegree() == 0)
			buf.append("(").append(getTaxaString()).append(")");
		else {
            toStringNetworkRec(buf, false, true, new HashMap<>(), new Single<>(0));
		}
        return buf + ";";
	}

	/**
	 * recursively do the work
	 *
	 * @param buf
	 * @param showTaxonIds
	 * @param oneTaxonPerLeaf
	 * @param hybrid2id
	 */
	private void toStringNetworkRec(StringBuffer buf, boolean showTaxonIds, boolean oneTaxonPerLeaf, Map<Node, Integer> hybrid2id, Single<Integer> hybridId) {
		if (hybrid2id.keySet().contains(this)) {
			buf.append("#H").append(hybrid2id.get(this));
			return;
		}
		if (getInDegree() > 1) {
			hybridId.set(hybridId.get() + 1);
			hybrid2id.put(this, hybridId.get());
		}

		if (getOutDegree() == 0) {
			if (oneTaxonPerLeaf)
				buf.append("x").append(getTaxa().nextSetBit(0));
			else
				buf.append("x").append(getTaxaString());
		} else {
			buf.append("(");
			for (Edge e = getFirstOutEdge(); e != null; e = getNextOutEdge(e)) {
				if (e != getFirstOutEdge()) {
					buf.append(",");
				}
				((Root) e.getTarget()).toStringNetworkRec(buf, showTaxonIds, oneTaxonPerLeaf, hybrid2id, hybridId);
			}
			buf.append(")");
			if (showTaxonIds) {
				buf.append("x").append(getTaxaString());
			}
		}
		if (getInDegree() > 1)
			buf.append("#H").append(hybrid2id.get(this));
	}

	/**
	 * get bit-string description of taxa and removed taxa
	 *
	 * @return get taxa
	 */
	public String getTaxaString() {
        StringBuilder buf = new StringBuilder();
        BitSet both = new BitSet();
		both.or(getTaxa());
		both.or(getRemovedTaxa());
		for (int t = both.nextSetBit(0); t != -1; t = both.nextSetBit(t + 1)) {
			if (getTaxa().get(t))
				buf.append("+").append(t);
			if (getRemovedTaxa().get(t))
				buf.append("-").append(t);
		}
		return buf.toString();
	}


	/**
	 * reorder the subtree below this node so that all children are in lexicographic order
	 */
	public void reorderSubTree() {
		reorderChildrenRec(this, true);
	}

	/**
	 * reorder children  in lexicographic order
	 */
	public void reorderChildren() {
		reorderChildrenRec(this, false);
	}

	/**
	 * recursively does the work
	 */
	private void reorderChildrenRec(Root v, boolean recurse) {
        List<Edge> children = new LinkedList<>();

		for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
			Root w = (Root) e.getTarget();
			if (recurse)
				reorderChildrenRec(w, recurse);
			children.add(e);
		}

		// if(!v.hasLexicographicChildren())
        {
            Edge[] array = children.toArray(new Edge[0]);
            Arrays.sort(array, (e1, e2) -> {
                Root v1 = (Root) e1.getTarget();
                Root v2 = (Root) e2.getTarget();

                int t1 = v1.getTaxa().nextSetBit(0);
                int t2 = v2.getTaxa().nextSetBit(0);
                while (t1 != -1 && t2 != -1) {
                    if (t1 < t2)
                        return -1;
                    else if (t1 > t2)
                        return 1;
                    t1 = v1.getTaxa().nextSetBit(t1 + 1);
                    t2 = v2.getTaxa().nextSetBit(t2 + 1);
                }
                if (t1 == -1 && t2 != -1)
                    return -1;
                if (t1 != -1 && t2 == -1)
                    return 1;
                return 0;
            });
            List<Edge> list = new LinkedList<>(Arrays.asList(array));
			if (v.getInDegree() > 0)
				list.add(v.getFirstInEdge());
			v.rearrangeAdjacentEdges(list);
		}
	}

	/**
	 * reorder the network below this node so that all children are in lexicographic order
	 */
	public void reorderNetwork() {
        Map<Node, Integer> order = new HashMap<>();
        Single<Integer> postOrderNumber = new Single<>(1);
        order.put(this, postOrderNumber.get());
        computePostOrderNumberingRec(this, order, postOrderNumber);
        reorderNetworkChildrenRec(this, order);
    }

	/**
	 * computes a post-order numbering of all nodes, avoiding edges that are only contained in tree2
	 *
	 * @param v
	 * @param order
	 * @param postOrderNumber
	 * @return taxa below
	 */
	private BitSet computePostOrderNumberingRec(Root v, final Map<Node, Integer> order, Single<Integer> postOrderNumber) {
		final BitSet taxaBelow = new BitSet();

		if (v.getOutDegree() == 0) {
			taxaBelow.or(v.getTaxa());
		} else {
            SortedSet<Pair<BitSet, Root>> child2TaxaBelow = new TreeSet<>((pair1, pair2) -> {
                int t1 = pair1.getFirst().nextSetBit(0);
                int t2 = pair2.getFirst().nextSetBit(0);
                if (t1 < t2)
                    return -1;
                else if (t1 > t2)
                    return 1;

                int id1 = pair1.getSecond().getId();
                int id2 = pair2.getSecond().getId();

                if (id1 < id2)
                    return -1;
                else if (id1 > id2)
                    return 1;
                else
                    return 0;
            });

			// first visit the children:
			for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
				Root w = (Root) e.getTarget();
				if (w.getTaxa().cardinality() > 0) {
					Integer treeId = (Integer) e.getInfo();
					if (w.getInDegree() > 1 && treeId == null)
						throw new RuntimeException("Node has two in-edges, one not labeled");
					if (w.getInDegree() == 1 || treeId == 1) {
						if (w.getInDegree() == 2 && treeId != null && treeId != 1)
							throw new RuntimeException("Node has two in-edges, but chosen one is not labeled 1");

						BitSet childTaxa = computePostOrderNumberingRec(w, order, postOrderNumber);
                        child2TaxaBelow.add(new Pair<>(childTaxa, w));

					} else {
						if (w.getInDegree() < 2)
							throw new RuntimeException("Node has only one in edge, which is labeled 2");
						if (w.getInDegree() == 2 && (Integer) w.getFirstInEdge().getInfo() == 2 && (Integer) w.getLastInEdge().getInfo() == 2)
							throw new RuntimeException("Node has two in edges, both labeled 2");
					}
				}
			}
			for (Pair<BitSet, Root> pair : child2TaxaBelow) {
				postOrderNumber.set(postOrderNumber.get() + 1);
				order.put(pair.getSecond(), postOrderNumber.get());
				taxaBelow.or(pair.getFirst());
			}
		}
		return taxaBelow;
	}

	/**
	 * recursively reorders the network using the post-order numbering computed above
	 */
	private void reorderNetworkChildrenRec(Root v, final Map<Node, Integer> order) {
		if (v.getTaxa().cardinality() > 0 && order.get(v) == null) {
			throw new RuntimeException("reorderNetworkChildrenRec: Unlabeled node encountered: " + v);
		}
		if (v.getTaxa().cardinality() > 0) {
            List<Edge> children = new LinkedList<>();

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Root w = (Root) e.getTarget();
                Integer treeId = (Integer) e.getInfo();
                if (w.getInDegree() == 1 || treeId == null || treeId != 2)
                    reorderNetworkChildrenRec(w, order);
                children.add(e);
            }

            Edge[] array = children.toArray(new Edge[0]);
            Arrays.sort(array, (e1, e2) -> {
                Integer rank1 = order.get(e1.getTarget());
                Integer rank2 = order.get(e2.getTarget());

                if (rank1 == null)  // dead node
                    rank1 = Integer.MAX_VALUE;
                if (rank2 == null)  // dead node
                    rank2 = Integer.MAX_VALUE;

                if (rank1 < rank2)
                    return -1;
                else if (rank1 > rank2)
                    return 1;
                else if (e1.getId() < e2.getId())
                    return -1;
                else if (e1.getId() > e2.getId())
                    return 1;
                else
                    return 0;
            });
            List<Edge> list = new LinkedList<>(Arrays.asList(array));
			if (v.getInDegree() > 0)
				list.add(v.getFirstInEdge());
			v.rearrangeAdjacentEdges(list);
		}
	}


	/**
	 * determines whether the children of the current node are lexicographically ordered
	 *
	 * @return true, if ordered
	 */
	public boolean hasLexicographicChildren() {
		if (getOutDegree() <= 1)
			return true;
		BitSet previous = null;
		for (Edge e = getFirstOutEdge(); e != null; e = getNextOutEdge(e)) {
			BitSet current = ((Root) e.getTarget()).getTaxa();
			if (previous != null && Cluster.compare(previous, current) >= 0)
				return false;
			previous = current;
		}
		return true;
	}

	/**
	 * check that tree is valid
	 */
	public void checkTree() {
		checkTreeRec(this);
	}

	/**
	 * recursively do the work
	 *
	 * @param v
	 */
	private void checkTreeRec(Root v) {
		if (!v.hasLexicographicChildren())
			throw new RuntimeException("checkTreeRec: children not lexicographic");
		if (v.getOutDegree() > 0) {
			BitSet childrenTaxa = new BitSet();
			for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
				Root w = (Root) e.getTarget();
				checkTreeRec(w);
				if (!Cluster.contains(v.getTaxa(), w.getTaxa()))
					throw new RuntimeException("checkTreeRec: children have taxa that parent does not");
				BitSet wTaxa = w.getTaxa();
				if (wTaxa.intersects(childrenTaxa))
					throw new RuntimeException("checkTreeRec: different children have same taxa");
				childrenTaxa.or(w.getTaxa());
			}
			if (!Cluster.equals(v.getTaxa(), childrenTaxa))
				throw new RuntimeException("checkTreeRec: Parent has taxa that children do not");
		}
	}

	/**
	 * does the tree rooted at this node have more than two leaves?
	 *
	 * @return true, if more two leaves  below this node
	 */
	public boolean hasMoreThanTwoLeaves() {
		return getOutDegree() > 2 || (getOutDegree() == 2 && (getFirstOutEdge().getTarget().getOutDegree() > 0 || getLastOutEdge().getTarget().getOutDegree() > 0));
	}

	/**
	 * produce a copy of the sub network rooted at the node
	 *
	 * @return sub tree or network below
	 */
	public Root copySubNetwork() {
		Root root2 = new Root(new Graph());
		root2.setTaxa(getTaxa());
		root2.setRemovedTaxa(getRemovedTaxa());
        Map<Root, Root> old2new = new HashMap<>();
		old2new.put(this, root2);

		copySubNetworkRec(this, root2, old2new);
		return root2;
	}

	/**
	 * recursively does the work
	 *
	 * @param v1
	 * @param v2
	 * @param old2new
	 */
	private void copySubNetworkRec(Root v1, Root v2, Map<Root, Root> old2new) {
		for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
			Root w1 = (Root) e1.getTarget();
			Root w2 = old2new.get(w1);
			if (w2 == null) {
				w2 = v2.newNode();
				w2.setTaxa(w1.getTaxa());
				w2.setRemovedTaxa(w1.getRemovedTaxa());
				old2new.put(w1, w2);
				copySubNetworkRec(w1, w2, old2new);
			}
			Edge f = v2.newEdge(v2, w2);
			f.setInfo(e1.getInfo());
		}
	}

	/**
	 * adds the network rooted at root1 to the given network
	 *
	 * @param root1
	 * @return new root in given network
	 */
	public Root addNetwork(Root root1) {
		Root root2 = newNode();
		root2.setTaxa(root1.getTaxa());
        Map<Root, Root> old2new = new HashMap<>();
		old2new.put(root1, root2);
		copySubNetworkRec(root1, root2, old2new);
		return root2;
	}

	/**
	 * is this a branching node, i.e. does it have two different children that are alive
	 *
	 * @return true, if two children alive
	 */
	public boolean isBranching() {
		boolean foundOne = false;
		for (Edge e = getFirstOutEdge(); e != null; e = getNextOutEdge(e)) {
			Root w = (Root) e.getTarget();
			if (w.getTaxa().cardinality() > 0) {
				if (foundOne)
					return true;
				else
					foundOne = true;
			}
		}
		return false;
	}
}
