package beastbooster.operators;

import beast.core.parameter.RealParameter;
import beast.evolution.operators.BactrianScaleOperator;

public class TargetableBactrianScaleOperator extends BactrianScaleOperator implements TargetableOperator {

	@Override
	public double proposal(int index) {
		final RealParameter param = parameterInput.get(this);

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
