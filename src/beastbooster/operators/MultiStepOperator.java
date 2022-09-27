package beastbooster.operators;

import beast.base.core.Description;

@Description("Operator that performs multiple steps consecutively in an efficient fashion")
public interface MultiStepOperator {
	
	// number of steps to perform consecutively
	public int stepCount();
	
	// tell operator which step needs to be executed
	// at step==0, special initialisation can take place (like determining post-order of nodes)
	// ate step==stepCount(), special clean up can take place
	default public void setStepNr(int step) {
		// ignored if not used
	}
}
