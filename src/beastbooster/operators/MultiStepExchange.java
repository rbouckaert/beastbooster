package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

/*
 * KNOWN BUGS: WIDE operator cannot be used on trees with 4 or less tips!
 */

@Description("Nearest neighbour interchange but with the restriction that node height must remain consistent.")
public class MultiStepExchange extends TreeOperator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 2);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());


	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int candidateCount = -1; 
	private int target;

	@Override
	public void initAndValidate() {
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		order = new int[treeInput.get().getInternalNodeCount()];
	}

	@Override
	public int stepCount() {
		if (candidateCount < 0) {
			return proposalsPerNode * (treeInput.get().getInternalNodeCount() - 1);
		} else {
			return proposalsPerNode * candidateCount;
		}
	}

	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = treeInput.get().getRoot().getNr();
			for (Targetable t : targets) {
				t.setTarget(target);
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			candidateCount = 0;
			traverse(treeInput.get().getRoot());
		}

		// set target nodes in Targetables (only if target node changes)
		if (step % proposalsPerNode == 0) {
			target = order[step / proposalsPerNode];
			for (Targetable t : targets) {
				t.setTarget(target);
			}
		}
	}

	/** establish post-order traversal on internal nodes **/
	private void traverse(Node node) {
		if (!node.isLeaf()) {
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft());
				if (!(node.getLeft().isLeaf() && node.getRight().isLeaf())) {
					order[candidateCount++] = node.getNr();
				}
				traverse(node.getRight());			
			} else {
				traverse(node.getRight());
				if (!(node.getLeft().isLeaf() && node.getRight().isLeaf())) {
					order[candidateCount++] = node.getNr();
				}
				traverse(node.getLeft());
			}
		}		
	}
	
	
	/**
	 * override this for proposals,
	 *
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
	 *         should not be accepted *
	 */
	@Override
	public double proposal() {
		final Tree tree = treeInput.get(this);

		double logHastingsRatio = 0;
		logHastingsRatio = narrow(tree);

		return logHastingsRatio;
	}

	private int isg(final Node n) {
		return (n.getLeft().isLeaf() && n.getRight().isLeaf()) ? 0 : 1;
	}

	private int sisg(final Node n) {
		return n.isLeaf() ? 0 : isg(n);
	}

	/**
	 * WARNING: Assumes strictly bifurcating beast.tree.
	 */
	public double narrow(final Tree tree) {

		final int internalNodes = tree.getInternalNodeCount();
		if (internalNodes <= 1) {
			return Double.NEGATIVE_INFINITY;
		}

		Node grandParent = tree.getNode(target);
		
		Node parentIndex = grandParent.getLeft();
		Node uncle = grandParent.getRight();
		if (parentIndex.getHeight() < uncle.getHeight()) {
			parentIndex = grandParent.getRight();
			uncle = grandParent.getLeft();
		}

		if (parentIndex.isLeaf()) {
			// tree with dated tips
			return Double.NEGATIVE_INFINITY;
		}

		int validGP = 0;
		{
			for (int i = internalNodes + 1; i < 1 + 2 * internalNodes; ++i) {
				validGP += isg(tree.getNode(i));
			}
		}

		final int c2 = sisg(parentIndex) + sisg(uncle);

		final Node i = (Randomizer.nextBoolean() ? parentIndex.getLeft() : parentIndex.getRight());
		exchangeNodes(i, uncle, parentIndex, grandParent);

		final int validGPafter = validGP - c2 + sisg(parentIndex) + sisg(uncle);

		return Math.log((float) validGP / validGPafter);
	}

	
	/* exchange sub-trees whose root are i and j */
	protected void exchangeNodes(Node i, Node j, Node p, Node jP) {
		// precondition p -> i & jP -> j
		replace(p, i, j);
		replace(jP, j, i);
		// postcondition p -> j & p -> i
	}
}
