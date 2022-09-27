package beastbooster.likelihood;

import beast.base.core.Description;

@Description("Allows target node (for rerooting) in a tree to be set, which can speed up treelikelihood calculations")
public interface Targetable {

	public void setTarget(int target);
}
