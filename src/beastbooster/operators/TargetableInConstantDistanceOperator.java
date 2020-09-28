package beastbooster.operators;

import beast.core.Description;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import consoperators.InConstantDistanceOperator;

@Description("Targetable version of Constant Distance Operator")
public class TargetableInConstantDistanceOperator extends InConstantDistanceOperator implements TargetableOperator {
	
	private int target = -1;
	
	@Override
	public void setTarget(int target) {
		this.target = target;
	}
	
	@Override
	public double proposal() {
		if (target >= 0) {
			return proposal(target);
		} else {
			throw new RuntimeException("Target is not set, use InConstantDistanceOperator");
		}
	}
	
	@Override
	public double proposal(int target) {
		this.target = target;
		return super.proposal();
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
