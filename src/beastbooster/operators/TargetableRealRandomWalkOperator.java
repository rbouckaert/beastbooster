package beastbooster.operators;

import beast.core.parameter.RealParameter;
import beast.evolution.operators.RealRandomWalkOperator;
import beast.util.Randomizer;

public class TargetableRealRandomWalkOperator extends RealRandomWalkOperator implements TargetableOperator {

	
    /**
     * override this for proposals,
     * returns log of hastingRatio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
	public double proposal(int i) {

        RealParameter param = parameterInput.get(this);

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
