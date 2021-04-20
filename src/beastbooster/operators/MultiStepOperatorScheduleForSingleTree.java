package beastbooster.operators;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.util.Log;
import beast.evolution.operators.Exchange;
import beast.evolution.operators.SubtreeSlide;
import beast.evolution.operators.TreeOperator;
import beast.evolution.operators.Uniform;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Operator schedule that recognises MultiStepOperators and selects them for the desired "
		+ "number of steps consecutively")
public class MultiStepOperatorScheduleForSingleTree extends OperatorSchedule {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 3);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal. "
			+ "If not specified, traverse the model graph in search of targetable objects.", new ArrayList<>());
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
		if (targets.size() == 0) {
			discoverTargets(this);
		}
		operators = new ArrayList<>();// operatorsInput.get();
		
		processOperators();
		for (Operator p : operatorsInput.get()) {
			addOperator(p);
		}

	}
    
	private void discoverTargets(BEASTInterface o2) {
		for (BEASTInterface o : o2.getOutputs()) {
			if (o instanceof MCMC) {
				discoverTargetsDown(o);
			} else {
				discoverTargets(o);
			}
		}
	}

	private void discoverTargetsDown(BEASTInterface o) {
		for (BEASTInterface o2 : o.listActiveBEASTObjects()) {
			if (o2 instanceof Targetable) {
				if (!targets.contains((Targetable) o2)) {
					targets.add((Targetable) o2);
				}
			} else {
				discoverTargetsDown(o2);
			}
		}
	}


	@Override
	public void addOperator(Operator p) {
		if (p.getClass() == Uniform.class) {
			Operator bp = new beastbooster.operators.TargetableBactrianNodeOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == SubtreeSlide.class) {
			Operator bp = new beastbooster.operators.TargetableSubTreeSlide();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == Exchange.class && ((Exchange)p).isNarrowInput.get()) {
			Operator bp = new beastbooster.operators.TargetableExchange();
			p = initialiseOperator(p, bp);
		}
		super.addOperator(p);
		processOperators();
	}
	
	private Operator initialiseOperator(Operator p, Operator bp) {
		Log.warning("replacing " + p.getID() + " with " + bp.getClass().getSimpleName());

		List<Object> os = new ArrayList<>();
		Set<String> inputNames = new LinkedHashSet<>();
		for (Input<?> input : p.listInputs()) {
			inputNames.add(input.getName());
		}
		
		for (Input<?> input : bp.listInputs()) {
			if (inputNames.contains(input.getName())) {
				Object value = p.getInputValue(input.getName());
				if (value != null && !(value instanceof List && ((List<?>)value).size() == 0)) {
				    os.add(input.getName());
				    os.add(value);
				}	
			}
		}
		bp.initByName(os.toArray());
		bp.setID(p.getID());
		return bp;
	}
	
	@Override
	protected void addOperators(Collection<Operator> ops) {
		super.addOperators(ops);
		processOperators();
	}
	
	
	private void processOperators() {
		for (Operator operator : operators) {
			if (operator instanceof TreeOperator) {
				if (tree != null && tree != ((TreeOperator) operator).treeInput.get()) {
					throw new IllegalArgumentException("This operator schedule can only handle 1 tree but found another " + tree.getID() + " " + ((TreeOperator) operator).treeInput.get().getID());
				}
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
			}
		}
		
		currentStep = 0;
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
    
    private int currentStep;	

    
    @Override
	public Operator selectOperator() {

    	
        while (true) {
    		Operator operator = super.selectOperator();
    		
        	if (operator instanceof TargetableOperator && ((TargetableOperator) operator).isTargetable()) {
        		TargetableOperator targetableOperator = (TargetableOperator) operator;
        		int attemptTargetable = 0;

        		while (true) {
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
						if (!tree.getNode(target).isLeaf()) {
							for (Targetable t : targets) {
								t.setTarget(target);
							}
						}
					}
					((TargetableOperator) operator).setTarget(target);
	                Node node = tree.getNode(target);
					if ((node.isLeaf() && targetableOperator.canHandleLeafTargets()) ||
						(node.isRoot() && targetableOperator.canHandleRootTargets()) ||
						(!node.isLeaf() && !node.isRoot() && targetableOperator.canHandleInternlTargets())) {
						currentStep++;
						return operator;
					}
					attemptTargetable++;
					if (attemptTargetable >= MAX_ATTEMPTS) {
						currentStep++;
						if (currentStep >= stepCount()) {
							currentStep = 0;
						}
						attemptTargetable = 0;
					}
					do {
						operator = super.selectOperator();
					} while (!(operator instanceof TargetableOperator));
	        	}
			} else {
				return operator;
			}
		}
		
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
