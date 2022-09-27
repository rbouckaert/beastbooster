package beastbooster.operators;

import beast.base.inference.parameter.RealParameter;
import beast.base.inference.operator.RealRandomWalkOperator;
import beast.base.util.Randomizer;

public class TargetableRealRandomWalkOperator extends RealRandomWalkOperator implements TargetableOperator {

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
			throw new RuntimeException("Target is not set, use RealRandomWalkOperator");
		}
	}
	
    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
	public double proposal(int i) {

        RealParameter param = parameterInput.get();
		param.startEditing(this);

        double value = param.getValue(i);
        double newValue = value;
        double windowSize = getCoercableParameterValue();
        if (useGaussianInput.get()) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newValue < param.getLower() || newValue > param.getUpper()) {
            return Double.NEGATIVE_INFINITY;
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }

        param.setValue(i, newValue);

        return 0.0;
    }


	@Override
	public boolean canHandleLeafTargets() {
		return true;
	}

	@Override
    public String getName() {
    	return "TargetableRealRandomWalkOperator(" + getID() + ")"; 
    }
}
