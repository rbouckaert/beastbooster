package beastbooster.operators;

import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.operator.kernel.BactrianScaleOperator;

public class TargetableBactrianScaleOperator extends BactrianScaleOperator implements TargetableOperator {

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
			throw new RuntimeException("Target is not set, use BactrianScaleOperator");
		}
	}


	@Override
	public double proposal(int index) {
		final RealParameter param = parameterInput.get();
		param.startEditing(this);

		final double oldValue = param.getValue(index);

		if (oldValue == 0) {
			// Error: parameter has value 0 and cannot be scaled
			return Double.NEGATIVE_INFINITY;
		}

		final double scale = getScaler(index, oldValue);
		double hastingsRatio = Math.log(scale);

		final double newValue = scale * oldValue;

		if (outsideBounds(newValue, param)) {
			// reject out of bounds scales
			return Double.NEGATIVE_INFINITY;
		}

		param.setValue(index, newValue);
		return hastingsRatio;
	}

	@Override
	public boolean canHandleLeafTargets() {
		return true;
	}

	@Override
	public String getName() {
    	return "TargetableBactrianScaleOperator(" + getID() + ")"; 
	}
}
