package beastbooster.operators;

import beast.core.Description;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import consoperators.InConstantDistanceOperator;

@Description("Targetable version of Constant Distance Operator")
public class TargetableInConstantDistanceOperator extends InConstantDistanceOperator implements TargetableOperator {
	int target;
	
	@Override
	public double proposal(int target) {
		this.target = target;
		return proposal();
	}
	
	@Override
	protected Node sampleNode(Tree tree) {
		return treeInput.get().getNode(target);
	}

	@Override
	public boolean canHandleLeafTargets() {
		return false;
	}


	
	@Override
	public String getName() {
    	return "TargetableInConstantDistanceOperator(" + getID() + ")"; 
	}
}
