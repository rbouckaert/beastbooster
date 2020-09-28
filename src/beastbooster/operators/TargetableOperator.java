package beastbooster.operators;

import beast.core.BEASTInterface;

public interface TargetableOperator {

	/** 
	 * Propose new state for target node or dimension
	 * @param target dimension to propose for
	 * @return log Hasting's ratio
	 */
	abstract public double proposal(int target);


	/** tell which nodes in the tree can be handled **/
	
	abstract boolean canHandleLeafTargets();
	
	default boolean canHandleRootTargets() {
		return false;
	};
	
	default public boolean canHandleInternlTargets() {
		return true;
	}
}
