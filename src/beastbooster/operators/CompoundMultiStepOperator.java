package beastbooster.operators;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.StateNode;
import beast.util.Randomizer;
import beastbooster.likelihood.Targetable;

@Description("Multi step operator that randomly chooses one of four local tree operators")
public class CompoundMultiStepOperator extends Operator implements MultiStepOperator {
	final public Input<Integer> proposalsPerNodeInput = new Input<>("proposalsPerNode", "number of proposals done for a node before moving on to the next node", 3);
	final public Input<List<Targetable>> targetsInput = new Input<>("target", "likelihoods affected by the node proposal", new ArrayList<>());
	public Input<List<TargetableOperator>> operatorInput = new Input<>("operator", "operator the CompoundOperator chooses from with probability proportional to its weight", new ArrayList<>(), Validate.REQUIRED);
	final public Input<Boolean> fullTraverseInput = new Input<>("fullTraverse", "whether to visit every node once (false), or on every node visit (true)" , true);
	final public Input<Boolean> includeLeafsInput = new Input<>("includeLeafs", "whether to visit leaf nodes (true) or nor (false)" , false);
	
    private List<TargetableOperator> operators = new ArrayList<>();
	private int proposalsPerNode;
	private List<Targetable> targets;
	private int [] order;
	private int target;
	private Tree tree;
	private boolean fullTraverse, includeLeafs;

    /** last operator used -- record the last choice for parameter tuning **/
    public TargetableOperator lastOperator;

    
    /**
     * cumulative weights, with unity as max value *
     */
    private double[] cumulativeProbs;
    
	// number of attempts for choosing a random operator, e.g. 
	// NNI may be rejected for leaf nodes, so give up if such
    // operators are chosen after MAX_ATTEMPTS times these are
    // selected
	private final static int MAX_ATTEMPTS = 10;

	@Override
	public void initAndValidate() {
		fullTraverse = fullTraverseInput.get();
		includeLeafs = includeLeafsInput.get();
		
		proposalsPerNode = proposalsPerNodeInput.get();
		targets = targetsInput.get();
		operators = operatorInput.get();
		for (TargetableOperator operator : operators) {
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

		// collect weights
		cumulativeProbs = new double[operators.size()];
		cumulativeProbs[0] = ((Operator)operators.get(0)).getWeight();
		for (int i = 1; i < operators.size(); i++) {
			cumulativeProbs[i] = ((Operator)operators.get(i)).getWeight() + cumulativeProbs[i-1];
		}
		// normalise
		for (int i = 0; i < operators.size(); i++) {
			cumulativeProbs[i] /= cumulativeProbs[operators.size() - 1];
		}
	}

	
	@Override
	public int stepCount() {
		return proposalsPerNode * order.length;
	}
	
	@Override
	public void setStepNr(int step) {
		if (step == stepCount()) {
			target = order[order.length - 1];
			// set DuckTreeLikelihood targets, if target is not a leaf
			if (!tree.getNode(target).isLeaf()) {
				for (Targetable t : targets) {
					t.setTarget(target);
				}
			}
			return;
		}
		if (step == 0) {
			// first step, determine current post-order
			traverse(tree.getRoot(), new int[1]);
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
	
	@Override
	public double proposal() {
        Node node = tree.getNode(target);
        
		for (int j = 0; j < MAX_ATTEMPTS; j++) {
			final int operatorIndex = Randomizer.randomChoice(cumulativeProbs);
			lastOperator = operators.get(operatorIndex);

			if ((node.isLeaf() && lastOperator.canHandleLeafTargets()) ||
				(node.isRoot() && lastOperator.canHandleRootTargets()) ||
				(!node.isLeaf() &&  !node.isRoot() && lastOperator.canHandleInternlTargets())) {
				return lastOperator.proposal(target);
			}
		}

		// give up -- could not find suitable candidate operator
		return Double.NEGATIVE_INFINITY;
	}
	
	
	@Override
	public void optimize(double logAlpha) {
		((Operator)lastOperator).optimize(logAlpha);
	}
	
	@Override
	public void accept() {
		((Operator)lastOperator).accept();
	}
	
	@Override
	public void reject() {
		((Operator)lastOperator).reject();
	}
	
	@Override
	public void reject(int reason) {
		((Operator)lastOperator).reject(reason);
	}

	@Override
	public List<StateNode> listStateNodes() {
		List<StateNode> list = new ArrayList<StateNode>();
		for (TargetableOperator o : operators) {
			Operator operator = (Operator) o;
			List<StateNode> list2 = operator.listStateNodes();
			list.addAll(list2);
			
		}
		return list;
	}

	@Override
	public void storeToFile(PrintWriter out) {
		out.print("{\"id\":\"" + getID() + "\",\"operators\":[\n");
		int k = 0;
		for (TargetableOperator o : operators) {
			Operator operator = (Operator) o;
			operator.storeToFile(out);
            if (k++ < operators.size() - 1) {
            	out.println(",");
            }
		}
        out.print("]}");
	}
	
	
	@Override
	public void restoreFromFile(JSONObject o) {
        try {
        JSONArray operatorlist = o.getJSONArray("operators");
        for (int i = 0; i < operatorlist.length(); i++) {
            JSONObject item = operatorlist.getJSONObject(i);
            String id = item.getString("id");
    		boolean found = false;
            if (!id.equals("null")) {
            	for (TargetableOperator to: operators) {
        			Operator operator = (Operator) to;
            		if (id.equals(operator.getID())) {
                    	operator.restoreFromFile(item);
                        found = true;
            			break;
            		}
            	}
            }
        	if (!found) {
        		Log.warning.println("Operator (" + id + ") found in state file that is not in operator list any more");
        	}
        }
    	for (TargetableOperator to: operators) {
			Operator operator = (Operator) to;
    		if (operator.getID() == null) {
        		Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
    		}
    	}
        } catch (JSONException e) {
        	// it is not a JSON file -- probably a version 2.0.X state file
	    }
	}
	
	
	@Override
	public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
		if (!(operatorSchedule instanceof MultiStepOperatorSchedule)) {
			Log.warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			Log.warning("==============================================================================================================");
			Log.warning("");
			Log.warning("      WARNING: Using " + this.getClass().getCanonicalName() + " without using MultiStepOperatorSchedule!");
			Log.warning("                          expect this to fail...");
			Log.warning("==============================================================================================================");
			Log.warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
        for (TargetableOperator o : operators) {
			Operator operator = (Operator) o;
        	operator.setOperatorSchedule(operatorSchedule);
        }
        super.setOperatorSchedule(operatorSchedule);
    }


	@Override
	public String getPerformanceSuggestion() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
		for (TargetableOperator operator : operators) {
			sb.append("\n\t");
			Operator op = (Operator) operator;
			int nameColWidth = 75;
			int colWidth = 10;
		      double tuning = op.getCoercableParameterValue();
		        double accRate = (double) op.get_m_nNrAccepted() / (double) (op.get_m_nNrAccepted() + op.get_m_nNrRejected());


		        String intFormat = " %" + colWidth + "d";
		        String doubleFormat = " %" + colWidth + "." + 5 + "f";

		        formatter.format("%-" + nameColWidth + "s", op.getName());
		        if (!Double.isNaN(tuning)) {
		            formatter.format(doubleFormat, tuning);
		        } else {
		            formatter.format(" %" + colWidth + "s", "-");
		        }

		        formatter.format(intFormat, op.get_m_nNrAccepted());
		        formatter.format(intFormat, op.get_m_nNrRejected());
		        formatter.format(doubleFormat, accRate);

		        sb.append(" " + op.getPerformanceSuggestion());

		}
        formatter.close();
		return sb.toString();
	}
	
	
}
