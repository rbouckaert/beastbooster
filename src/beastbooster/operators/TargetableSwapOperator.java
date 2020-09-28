package beastbooster.operators;



import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;



@Description("A generic operator swapping a one or more pairs in a multi-dimensional parameter")
public class TargetableSwapOperator extends Operator implements TargetableOperator {
    final public Input<RealParameter> parameterInput = new Input<>("parameter", "a real parameter to swap individual values for");
    

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
	    	throw new RuntimeException("use SwapOperator instead of " + this.getClass().getCanonicalName());
		}
	}
	
	Parameter<?> parameter;

    @Override
    public void initAndValidate() {
    	parameter = parameterInput.get();
    }



    @Override
    public double proposal(int target) {
        int other = target;
        do {
        	other = Randomizer.nextInt(parameter.getDimension());
        } while (other == target);
        parameter.swap(target, other);
        return 0;
    }

	@Override
	public boolean canHandleLeafTargets() {
		return true;
	}

	@Override
    public String getName() {
    	return "TargetableSwapOperator(" + getID() + ")"; 
    }
}
