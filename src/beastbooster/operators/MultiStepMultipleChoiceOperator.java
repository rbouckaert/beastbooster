package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Multi step operator that randomly chooses one of four local tree operators")
public class MultiStepMultipleChoiceOperator extends TreeOperator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 3);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());
    final public Input<Double> sizeInput = new Input<>("size", "size of the slide, default 1.0", 1.0);
    final public Input<String> operatorWeightsInput = new Input<>("operatorWeights", "comma separated string of relative operator weights"
    		+ " for uniform, sub-tree slide, narrow exchange, NNI and meta data respectively,", "3,1,1,1,0");

    final public Input<List<RealParameter>> metadataInput = new Input<>("metadata", "meta data associated with nodes in the tree", new ArrayList<>());
    
    // TODO: implement auto-optimisation of size
    // note size is used both for sub-tree slide and NNI
    
	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int target;
	private Tree tree;
	private double size;
	private double [] operatorWeights;
	private List<RealParameter> metadata;
	
	// number of attempts for choosing a random operator
	// NNI may be rejected, so give up if it is chosen after MAX_ATTEMPTS times it is selected
	private final static int MAX_ATTEMPTS = 10;
	private final static int WEIGHTS = 5;
	
	@Override
	public void initAndValidate() {
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		tree = treeInput.get();
		order = new int[tree.getInternalNodeCount()];		
		size = sizeInput.get();
		
		String [] weights = operatorWeightsInput.get().split(",");
		if (weights.length != WEIGHTS) {
			throw new IllegalArgumentException("expected 5 weights, not " + weights.length);
		}
		operatorWeights = new double[WEIGHTS];
		for (int i = 0; i < WEIGHTS; i++) {
			operatorWeights[i] = Double.parseDouble(weights[i]);
			if (operatorWeights[i]< 0) {
				throw new IllegalArgumentException("operator weights should be 0 or larger");
			}
		}
		// normalise
		double sum = 0;
		for (double d : operatorWeights) sum += d;
		if (sum == 0) {
			throw new IllegalArgumentException("sum of weights should be positive");
		}
		for (int i = 0; i < WEIGHTS; i++) {
			operatorWeights[i] /= sum;
		}
		
		metadata = metadataInput.get();
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


	int callCount = 0;
	@Override
	public double proposal() {
		double [] operatorWeights = 
		(callCount < 0) ? 
				new double[]{0.3,0.3,0.0,0.4,0.0}:this.operatorWeights;
		callCount++;
		
		for (int j = 0; j < MAX_ATTEMPTS; j++) {
			double r = Randomizer.nextDouble();
			int i = 0;
			while (r > operatorWeights[i]) {
				r = r - operatorWeights[i];
				i++;
			}

			Node node2 = tree.getNode(target);
			if (node2.isLeaf()) {
				int h = 3;
				h++;
			}
			
			switch (i) {
			case 0:
				return proposeUniform();
			case 1:
				return proposeSubTreeSlide();
			case 2:
				return proposeNarrowExchange();
			case 3:
		        Node node = tree.getNode(target);
				if (!node.isLeaf() && (!node.getLeft().isLeaf() || !node.getRight().isLeaf())) {
					return proposeNNI();
				}
			case 4:
				if (metadata.size() > 0) {
					return proposeMetaDataChange();
				}
			}
		}		
		return Double.NEGATIVE_INFINITY;
	}
	
	
	private double proposeMetaDataChange() {
		RealParameter p = metadata.get(Randomizer.nextInt(metadata.size()));
        Node node = tree.getNode(target);
        int i = target;
        switch (Randomizer.nextInt(3)) {
        case 0: i = target; break;
        case 1: i = node.getLeft().getNr(); break;
        case 2: i = node.getRight().getNr(); break;
        }
		double value = p.getValue(i);
		double newValue = value * Math.exp(Randomizer.nextDouble()-0.5);
		p.setValue(i, newValue);
		return 0;
	}


	private double proposeUniform() {
        Node node = tree.getNode(target);
        double upper = node.getParent().getHeight();
        double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        final double newValue = (Randomizer.nextDouble() * (upper - lower)) + lower;
        node.setHeight(newValue);
        return 0.0;
	}
	
	
	private double proposeSubTreeSlide() {
        double logq;

        Node i;
        final boolean markClades = markCladesInput.get();
        // 1. choose a random node avoiding root
        final int nodeCount = tree.getNodeCount();
        if (nodeCount == 1) {
        	// test for degenerate case (https://github.com/CompEvol/beast2/issues/887)
        	return Double.NEGATIVE_INFINITY;
        }
        i = tree.getNode(target);
        
        final Node p = i.getParent();
        final Node CiP = getOtherChild(p, i);
        final Node PiP = p.getParent();

        // 2. choose a delta to move
        final double delta = Randomizer.nextGaussian() * size;
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

    private int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();
        
        if (parent == null) {
        	// can happen with non-standard non-mutable trees
        	return 0;
        }

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
    
    private int isg(final Node n) {
		return (n.getLeft().isLeaf() && n.getRight().isLeaf()) ? 0 : 1;
	}

	private int sisg(final Node n) {
		return n.isLeaf() ? 0 : isg(n);
	}

	/**
	 * WARNING: Assumes strictly bifurcating beast.tree.
	 */
	public double proposeNarrowExchange() {

		final int internalNodes = tree.getInternalNodeCount();
		if (internalNodes <= 1) {
			return Double.NEGATIVE_INFINITY;
		}

		Node grandParent = tree.getNode(target);
		
		Node parentIndex = grandParent.getLeft();
		Node uncle = grandParent.getRight();
		if (parentIndex.getHeight() < uncle.getHeight()) {
			parentIndex = grandParent.getRight();
			uncle = grandParent.getLeft();
		}

		if (parentIndex.isLeaf()) {
			// tree with dated tips
			return Double.NEGATIVE_INFINITY;
		}

		int validGP = 0;
		{
			for (int i = internalNodes + 1; i < 1 + 2 * internalNodes; ++i) {
				validGP += isg(tree.getNode(i));
			}
		}

		final int c2 = sisg(parentIndex) + sisg(uncle);

		final Node i = (Randomizer.nextBoolean() ? parentIndex.getLeft() : parentIndex.getRight());
		exchangeNodes(i, uncle, parentIndex, grandParent);

		final int validGPafter = validGP - c2 + sisg(parentIndex) + sisg(uncle);

		return Math.log((float) validGP / validGPafter);
	}

	
	/* exchange sub-trees whose root are i and j */
	private void exchangeNodes(Node i, Node j, Node p, Node jP) {
		// precondition p -> i & jP -> j
		replace(p, i, j);
		replace(jP, j, i);
		// postcondition p -> j & p -> i
	}
	
	
	private double proposeNNI() {
	    double logq;
	
	    Node i = tree.getNode(target);
	
	    final Node iP = i.getParent();
	    final Node CiP = getOtherChild(iP, i);
	    final Node PiP = iP.getParent();
	
	    // 2. choose a delta to move
        final double delta = Randomizer.nextGaussian() * size;
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
	//can ignore the following condition if the complete tree is included
	//            if (!candidates.contains(newParent)) {
	//            	return Double.NEGATIVE_INFINITY;
	//            }
	
	
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

}
