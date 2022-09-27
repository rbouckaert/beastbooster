package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Node operator that proposes node heights in traversal order")
public class MultiStepBactrianNodeOperator extends beast.base.evolution.operator.kernel.BactrianNodeOperator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 2);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());
	
	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int target;
	
	@Override
	public void initAndValidate() {
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		order = new int[treeInput.get().getInternalNodeCount()];
		super.initAndValidate();
		
	}
	
	
	@Override
	public int stepCount() {
		return proposalsPerNode * (treeInput.get().getInternalNodeCount() - 1);
	}
	
	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = order[order.length - 1];
			for (Targetable t : targets) {
				t.setTarget(target);
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			traverse(treeInput.get().getRoot(), new int[1]);
			order[order.length-1] = treeInput.get().getRoot().getNr();
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
	private void traverse(Node node, int[] is) {
		if (!node.isLeaf()) {
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft(), is);
				if (!node.isRoot()) {
					order[is[0]++] = node.getNr();
				}
				traverse(node.getRight(), is);
			} else {
				traverse(node.getRight(), is);
				if (!node.isRoot()) {
					order[is[0]++] = node.getNr();
				}
				traverse(node.getLeft(), is);
			}
		}		
	}

    @Override
    public double proposal() {
        Tree tree = treeInput.get();
        tree.startEditing(this);

        // select target node

        // Abort if no non-root internal nodes
        if (tree.getInternalNodeCount()==1)
            return Double.NEGATIVE_INFINITY;
        
        Node node = tree.getNode(target);
        double upper = node.getParent().getHeight();
        double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        
        double scale = kernelDistribution.getScaler(0, Double.NaN, scaleFactor);

        // transform value
        double value = node.getHeight();
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);
        
        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }
        
        node.setHeight(newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

 
	
	
	
}
