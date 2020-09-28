package beastbooster.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Operator schedule that recognises MultiStepOperators and selects them for the desired "
		+ "number of steps consecutively")
public class MultiStepOperatorSchedule extends OperatorSchedule {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 3);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());
	// public Input<List<TargetableOperator>> operatorInput = new Input<>("operator", "operator the CompoundOperator chooses from with probability proportional to its weight", new ArrayList<>(), Validate.REQUIRED);
	final public Input<Boolean> fullTraverseInput = new Input<>("fullTraverse", "whether to visit every node once (false), or on every node visit (true)" , true);
	final public Input<Boolean> includeLeafsInput = new Input<>("includeLeafs", "whether to visit leaf nodes (true) or nor (false)" , false);
	
    // private List<TargetableOperator> operators = new ArrayList<>();
	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int target;
	private Tree tree;
	private boolean fullTraverse, includeLeafs;

    /** last operator used -- record the last choice for parameter tuning **/
    // public TargetableOperator lastOperator;


	// number of attempts for choosing a random operator, e.g. 
	// NNI may be rejected for leaf nodes, so give up if such
    // operators are chosen after MAX_ATTEMPTS times these are
    // selected
	private final static int MAX_ATTEMPTS = 10;

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		
		fullTraverse = fullTraverseInput.get();
		includeLeafs = includeLeafsInput.get();
		
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		operators = operatorsInput.get();
		for (Operator operator : operators) {
			if (operator instanceof TreeOperator) {
				tree = ((TreeOperator) operator).treeInput.get();
				if (fullTraverse) {
					if (includeLeafs) {
						order = new int[tree.getNodeCount() + tree.getInternalNodeCount() * 2];
					} else {
						order = new int[tree.getInternalNodeCount() * 3];
					}
				} else {
					if (includeLeafs) {
						order = new int[tree.getNodeCount()];
					} else {
						order = new int[tree.getInternalNodeCount()];
					}
				}
				break;
			}
		}
		
		currentStep = 0;
		traverse(tree.getRoot(), new int[1]);
		
	}
    
    
	
	/** establish post-order traversal on internal nodes **/
	private void traverse(Node node, int[] is) {
		if (node.isLeaf()) {
			if (includeLeafs) {
				order[is[0]++] = node.getNr();
			}
		} else {
			if (fullTraverse) {
				order[is[0]++] = node.getNr();
			}
			if (Randomizer.nextBoolean()) {
				traverse(node.getLeft(), is);
				order[is[0]++] = node.getNr();
				traverse(node.getRight(), is);
			} else {
				traverse(node.getRight(), is);
				order[is[0]++] = node.getNr();
				traverse(node.getLeft(), is);
			}
			if (fullTraverse) {
				order[is[0]++] = node.getNr();
			}
		}		
	}
    
    private int stepCount, currentStep;
	// private MultiStepOperator multiStepOperator = null;
	
	@Override
	public Operator selectOperator() {
		Operator operator = super.selectOperator();
		
        Node node = tree.getNode(target);

        for (int j = 0; j < MAX_ATTEMPTS; j++) {
        	if (operator instanceof TargetableOperator && ((TargetableOperator) operator).isTargetable()) {
        		TargetableOperator targetableOperator = (TargetableOperator) operator;

				if ((node.isLeaf() && targetableOperator.canHandleInternlTargets()) ||
					(node.isRoot() && targetableOperator.canHandleRootTargets()) ||
					(!node.isLeaf() &&  !node.isRoot() && targetableOperator.canHandleInternlTargets())) {
					if (currentStep == stepCount()) {
						target = order[order.length - 1];
						// set DuckTreeLikelihood targets, if target is not a leaf
						if (!tree.getNode(target).isLeaf()) {
							for (Targetable t : targets) {
								t.setTarget(target);
							}
						}
						((TargetableOperator) operator).setTarget(target);
						currentStep = 0;
						return operator;
					}
					if (currentStep == 0) {
						// first step, determine current post-order
						traverse(tree.getRoot(), new int[1]);
					}
	
					// set target nodes in Targetables (only if target node changes)
					if (currentStep % proposalsPerNode == 0) {
						target = order[currentStep / proposalsPerNode];
						for (Targetable t : targets) {
							t.setTarget(target);
						}
					}
					((TargetableOperator) operator).setTarget(target);
					currentStep++;
					return operator;
				}
			} else {
				return operator;
			}
		}
	        
		// recurse
		return selectOperator();
	        

		

		
//		if (multiStepOperator != null) {
//			currentStep++;
//			multiStepOperator.setStepNr(currentStep);
//			if (currentStep < stepCount) {
//				return (Operator) multiStepOperator;
//			} else {
//				multiStepOperator = null;
//			}
//		}
//		
//		Operator operator = super.selectOperator();
//		if (operator instanceof MultiStepOperator) {
//			multiStepOperator = (MultiStepOperator) operator;
//			currentStep = 0;
//			multiStepOperator.setStepNr(currentStep);
//			stepCount = multiStepOperator.stepCount();
//		}
// 		return operator;
    }

	public int stepCount() {
		return proposalsPerNode * order.length;
	}

}
