package beastbooster.operators;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Multi step version of NNI operator")
public class MultiStepNNI extends TreeOperator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 2);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());

	final public Input<Double> sizeInput = new Input<>("size", "size of the slide, default 1.0", 1.0);
    final public Input<Boolean> gaussianInput = new Input<>("gaussian", "Gaussian (=true=default) or uniform delta", true);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Double> limitInput = new Input<>("limit", "limit on step size, default disable, " +
            "i.e. -1. (when positive, gets multiplied by tree-height/log2(n-taxa).", -1.0);
    // shadows size
    private double size;
    private double limit;

	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int candidateCount = -1; 
	private int target;

    private Tree tree = null;

    @Override
    public void initAndValidate() {
        size = sizeInput.get();
        limit = limitInput.get();
        this.tree = treeInput.get();
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		order = new int[treeInput.get().getInternalNodeCount()];
	}

	@Override
	public int stepCount() {
		if (candidateCount < 0) {
			return proposalsPerNode * (treeInput.get().getInternalNodeCount() - 1);
		} else {
			return proposalsPerNode * candidateCount;
		}
	}

	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = treeInput.get().getRoot().getNr();
			for (Targetable t : targets) {
				t.setTarget(target);
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			candidateCount = 0;
			traverse(treeInput.get().getRoot());
		}

		// set target nodes in Targetables (only if target node changes)
		if (step % proposalsPerNode == 0) {
			target = order[step / proposalsPerNode];
			for (Targetable t : targets) {
				t.setTarget(target);
			}
		}
	}

	/** establish post-order traversal on internal nodes **/
	private void traverse(Node node) {
		if (!node.isLeaf()) {
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft());
				if (!(node.getLeft().isLeaf() && node.getRight().isLeaf()) && !node.isRoot()) {
					order[candidateCount++] = node.getNr();
				}
				traverse(node.getRight());
			} else {
				traverse(node.getRight());
				if (!(node.getLeft().isLeaf() && node.getRight().isLeaf()) && !node.isRoot()) {
					order[candidateCount++] = node.getNr();
				}
				traverse(node.getLeft());
			}
		}		
	}
	
	
	/**
	 * override this for proposals,
	 *
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
	 *         should not be accepted *
	 */
	@Override
	public double proposal() {
        final Tree tree = treeInput.get(this);
        // 0. determine set of candidate nodes

        double logq;

        Node i = tree.getNode(target);

        final Node iP = i.getParent();
        final Node CiP = getOtherChild(iP, i);
        final Node PiP = iP.getParent();

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = iP.getHeight();
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = iP;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }


                // 3.1.1 if creating a new root
                if (newChild.isRoot()) {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);

                    iP.setParent(null);
                    tree.setRoot(iP);

                }
                // 3.1.2 no new root
                else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);
                }

                iP.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }

            // 4.1 will the move change the topology
            if (CiP.getHeight() > newHeight) {

                final List<Node> newChildren = new ArrayList<Node>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();
                
                // only allow the parent to be in the candidate set
                // we don't want to slide further down, otherwise the HR is incorrect;
                // there is no way to move back to the original situation using this operator if we slide down below our candidate set
// can ignore the following condition if the complete tree is included
//                if (!candidates.contains(newParent)) {
//                	return Double.NEGATIVE_INFINITY;
//                }


                // 4.1.1 if iP was root
                if (iP.isRoot()) {
                    // new root is CiP
                    replace(iP, CiP, newChild);
                    replace(newParent, newChild, iP);

                    CiP.setParent(null);
                    tree.setRoot(CiP);

                } else {
                    replace(iP, CiP, newChild);
                    replace(PiP, iP, CiP);
                    replace(newParent, newChild, iP);
                }

                iP.setHeight(newHeight);

                logq = Math.log(possibleDestinations);
            } else {
                iP.setHeight(newHeight);
                logq = 0.0;
            }
        }
        return logq;
	}
	
    private double getDelta() {
        if (!gaussianInput.get()) {
            return (Randomizer.nextDouble() * size) - (size / 2.0);
        } else {
            return Randomizer.nextGaussian() * size;
        }
    }

    private int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();

        if (parent.getHeight() < height) return 0;

        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        if (node.isLeaf()) {
            // TODO: verify that this makes sense
            return 0;
        } else {
            final int count = intersectingEdges(node.getLeft(), height, directChildren) +
                    intersectingEdges(node.getRight(), height, directChildren);
            return count;
        }
    }
    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(size);
            final double f = Math.exp(delta);
//            double f = Math.exp(delta);
            if( limit > 0 ) {
                final Tree tree = treeInput.get();
                final double h = tree.getRoot().getHeight();
                final double k = Math.log(tree.getLeafNodeCount()) / Math.log(2);
                final double lim = (h / k) * limit;
                if( f <= lim ) {
                    size = f;
                }
            } else {
               size = f;
            }
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return size;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        size = value;
    }

    @Override
    public String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;

        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        final double newDelta = size * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try decreasing size to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try increasing size to about " + formatter.format(newDelta);
        } else return "";
    }
    
}
