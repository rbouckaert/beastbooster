package beastbooster.operators;


import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.IntegerParameter;
import beast.util.Randomizer;


@Description("A random walk operator that selects a random dimension of the integer parameter and perturbs the value a " +
        "random amount within +/- windowSize.")
public class TargetableIntRandomWalkOperator extends Operator implements TargetableOperator {
    final public Input<Integer> windowSizeInput =
            new Input<>("windowSize", "the size of the window both up and down", Validate.REQUIRED);
    final public Input<IntegerParameter> parameterInput =
            new Input<>("parameter", "the parameter to operate a random walk on.", Validate.REQUIRED);

    int windowSize = 1;

    @Override
	public void initAndValidate() {
        windowSize = windowSizeInput.get();
    }

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
    public double proposal(int target) {
        final IntegerParameter param = parameterInput.get(this);

        final int i = target;
        final int value = param.getValue(i);
        final int newValue = value + Randomizer.nextInt(2 * windowSize + 1) - windowSize;

        if (newValue < param.getLower() || newValue > param.getUpper()) {
            // invalid move, can be rejected immediately
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
    public void optimize(final double logAlpha) {
        // nothing to optimise
    }

	@Override
	public boolean canHandleLeafTargets() {
		return true;
	}

} // class TargetableIntRandomWalkOperator

