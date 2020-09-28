package beastbooster.operators;

import beast.core.Description;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.UniformOperator;
import beast.util.Randomizer;

@Description("Assign target parameter value to a uniformly selected value in its range.")
public class TargetableUniform extends UniformOperator implements TargetableOperator {

	private Parameter<?> parameter;
	private double lower, upper;
	private int lowerIndex, upperIndex;

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
