package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.operators.BactrianSubtreeSlide;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;


@Description("As sub-tree slide but with Bactrian kernel to determine size of step. " +
		"Moves the height of an internal node along the branch. " +
        "If it moves up, it can exceed the root and become a new root. " +
        "If it moves down, it may need to make a choice which branch to " +
        "slide down into.")
public class MultiStepBactrianSubtreeSlide extends beast.evolution.operators.BactrianSubtreeSlide implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 2);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());


	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int target;
	private double size;
	
	@Override
	public void initAndValidate() {
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		order = new int[treeInput.get().getInternalNodeCount()];
    	super.initAndValidate();
    }
    
    
	@Override
	public int stepCount() {
		return proposalsPerNode * (treeInput.get().getInternalNodeCount() - 1);
	}
	
	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = order[order.length - 1];
			for (Targetable t : targets) {
				t.setTarget(target);
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			traverse(treeInput.get().getRoot(), new int[1]);
			order[order.length-1] = treeInput.get().getRoot().getNr();
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
	private void traverse(Node node, int[] is) {
		if (!node.isLeaf()) {
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft(), is);
				if (!node.isRoot()) {
					order[is[0]++] = node.getNr();
				}
				traverse(node.getRight(), is);
			} else {
				traverse(node.getRight(), is);
				if (!node.isRoot()) {
					order[is[0]++] = node.getNr();
				}
				traverse(node.getLeft(), is);
			}
		}		
	}


    /**
     * Do a probabilistic subtree slide move.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {
        final Tree tree = treeInput.get(this);

        double logq;

        Node i;
        final boolean markClades = markCladesInput.get();
        // 1. choose a random node avoiding root        
        i = tree.getNode(target);

        final Node p = i.getParent();
        final Node CiP = getOtherChild(p, i);
        final Node PiP = p.getParent();

        // 2. choose a delta to move
        final double delta = getDelta();
        final double oldHeight = p.getHeight();
        final double newHeight = oldHeight + delta;

        // 3. if the move is up
        if (delta > 0) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = p;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    if( markClades ) newParent.makeDirty(Tree.IS_FILTHY); // JH
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }
                // the moved node 'p' would become a child of 'newParent'
                //

                // 3.1.1 if creating a new root
                if (newChild.isRoot()) {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);

                    p.setParent(null);
                    tree.setRoot(p);
                }
                // 3.1.2 no new root
                else {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }

                p.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                //System.out.println("possible sources = " + possibleSources);

                logq = -Math.log(possibleSources);

            } else {
                // just change the node height
                p.setHeight(newHeight);
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

                final List<Node> newChildren = new ArrayList<>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();

                // 4.1.1 if p was root
                if (p.isRoot()) {
                    // new root is CiP
                    replace(p, CiP, newChild);
                    replace(newParent, newChild, p);

                    CiP.setParent(null);
                    tree.setRoot(CiP);

                } else {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }

                p.setHeight(newHeight);
                if( markClades ) {
                    // make dirty the path from the (down) moved node back up to former parent.
                    Node n = p;
                    while( n != CiP ) {
                        n.makeDirty(Tree.IS_FILTHY); // JH
                        n = n.getParent();
                    }
                }

                logq = Math.log(possibleDestinations);
            } else {
                p.setHeight(newHeight);
                logq = 0.0;
            }
        }
        return logq;
    }

}
