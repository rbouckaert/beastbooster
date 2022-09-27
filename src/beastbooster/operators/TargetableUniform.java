package beastbooster.operators;

import beast.base.core.Description;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.operator.UniformOperator;
import beast.base.util.Randomizer;

@Description("Assign target parameter value to a uniformly selected value in its range.")
public class TargetableUniform extends UniformOperator implements TargetableOperator {

	private Parameter<?> parameter;
	private double lower, upper;
	private int lowerIndex, upperIndex;

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
	    	throw new RuntimeException("use UniformOperator instead of " + this.getClass().getCanonicalName());
		}
	}
	
	@Override
	public void initAndValidate() {
		parameter = parameterInput.get();
		if (parameter instanceof RealParameter) {
			lower = (Double) parameter.getLower();
			upper = (Double) parameter.getUpper();
		} else if (parameter instanceof IntegerParameter) {
			lowerIndex = (Integer) parameter.getLower();
			upperIndex = (Integer) parameter.getUpper();
		} else {
			throw new IllegalArgumentException(
					"parameter should be a RealParameter or IntergerParameter, not " + parameter.getClass().getName());
		}

		int howMany;
		howMany = howManyInput.get();
		if (howMany > 1) {
			throw new IllegalArgumentException("howMany must be 1 for TargetableUniformOperator");
		}
	}

	@Override
	public double proposal(int index) {
		// do not worry about duplication, does not matter
		if (parameter instanceof IntegerParameter) {
			int newValue = Randomizer.nextInt(upperIndex - lowerIndex + 1) + lowerIndex; 
			((IntegerParameter) parameter).setValue(index, newValue);
		} else {
			double newValue = Randomizer.nextDouble() * (upper - lower) + lower;
			((RealParameter) parameter).setValue(index, newValue);
		}
		return 0.0;
	}

	@Override
	public boolean canHandleLeafTargets() {
		return true;
	}

	@Override
    public String getName() {
    	return "TargetableUniform(" + getID() + ")"; 
    }
}
