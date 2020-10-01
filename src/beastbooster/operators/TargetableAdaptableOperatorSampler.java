package beastbooster.operators;

import orc.operators.AdaptableOperatorSampler;

public class TargetableAdaptableOperatorSampler extends AdaptableOperatorSampler implements TargetableOperator {

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
			throw new RuntimeException("Target is not set, use AdaptableOperatorSampler");
		}
	}
	
	public double proposal(int target) {
		this.target = target;
		return super.proposal();
	}


	@Override
	public boolean canHandleLeafTargets() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getName() {
    	return "TargetableAdaptableOperatorSampler(" + this.getID() + ")"; 
	}

}
