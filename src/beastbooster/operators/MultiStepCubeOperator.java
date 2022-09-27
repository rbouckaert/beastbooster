package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Node operator that proposes cube heights in traversal order")
public abstract class MultiStepCubeOperator extends TreeOperator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 1);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());


	final static boolean debug = false;

	protected int [] order;
	protected double [] heights;
	protected int target;
	protected Tree tree;
	protected int leafNodeCount;

	private int proposalsPerNode;
	private List<Targetable> targets;


	@Override
	public void initAndValidate() {
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();

		tree = treeInput.get();
		leafNodeCount = tree.getLeafNodeCount();

		order = new int[tree.getNodeCount()];
		heights = new double[tree.getInternalNodeCount()];
	}
	
	
	@Override
	public int stepCount() {
		return proposalsPerNode * treeInput.get().getInternalNodeCount();
	}
	
	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = tree.getRoot().getNr();
			for (Targetable t : targets) {
				t.setTarget(target);
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			traverse(treeInput.get().getRoot(), new int[]{0});
		}

		// set target nodes in Targetables (only if target node changes)
		if (step % proposalsPerNode == 0) {
			target = order[leafNodeCount + step / proposalsPerNode];
			for (Targetable t : targets) {
				t.setTarget(target);
			}
		}
	}

	/** establish randomised post-order traversal on internal nodes **/
	private void traverse(Node node, int[] is) {
		if (node.isLeaf()) {
			order[is[0]] = node.getNr();
		} else {
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft(), is);
				heights[is[0]] = node.getHeight();
				order[leafNodeCount + is[0]++] = node.getNr();
				traverse(node.getRight(), is);
			} else {
				traverse(node.getRight(), is);				
				heights[is[0]] = node.getHeight();
				order[leafNodeCount + is[0]++] = node.getNr();
				traverse(node.getLeft(), is);
			}
		}		
	}
	
}
