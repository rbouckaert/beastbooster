package beastbooster.operators;


import java.text.DecimalFormat;

import org.apache.commons.math3.util.FastMath;

import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.State;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

@Description("Node operator that proposes node heights in traversal order")
public class TargetableMALANodeOperator extends TreeOperator implements TargetableOperator {
    final public Input<Double> windowSizeInput = new Input<>("windowSize", "the size of standard deviation of Gaussian", Input.Validate.REQUIRED);
    final public Input<Double> stepUpperLimit = new Input<>("upper", "Upper Limit of step size", 10.0);
    final public Input<Double> stepLowerLimit = new Input<>("lower", "Lower limit of step size", 1e-8);

	private State state;
	private Distribution posterior;
	private double windowSize, upper, lower;
	private Tree tree;

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
			throw new RuntimeException("Target is not set, use TargetableOperatorSchedule");
		}
	}
	
	@Override
	public void initAndValidate() {
		tree = treeInput.get();
		getStateAndPosterior();
		windowSize = windowSizeInput.get();
		upper = stepUpperLimit.get();
		lower = stepLowerLimit.get();
	}
	
	private void getStateAndPosterior() {		
		Runnable runnable = getRunnable(this);
		if (runnable == null) {
			throw new RuntimeException("Expected this operator to be in a Runnable");
		}
		Object o = runnable.getInput("state").get();
		if (o == null) {
			throw new RuntimeException("Expected this operator to be in a Runnable with a state input");
		}
		if (o instanceof State) {
			state = (State) o;
			
			o = runnable.getInput("distribution").get();
			if (o == null) {
				throw new RuntimeException("Expected this operator to be in a Runnable with a distribution input");
			}
			if (o instanceof Distribution) {			
				posterior = (Distribution) o;
				return;
			}
			throw new RuntimeException("Expected this operator to be in a Runnable with a distribution input of type Distribution");			
		}
		throw new RuntimeException("Expected this operator to be in a Runnable with a state input of type State");
	}
	
	private Runnable getRunnable(BEASTInterface o) {
		for (BEASTInterface o1 : o.getOutputs()) {
			if (o1 instanceof Runnable) {
				return (Runnable) o1;
			} else {
				o1 = getRunnable(o1);
				if (o1 != null) {
					return (Runnable) o1;
				}
			}
		}
		return null;
	}

	final static double EPSILON = 1e-8;
	
	@Override
    public double proposal(int target) {
		// determine gradient in height[target]
		double logP = posterior.getCurrentLogP();

		// state.storeCalculationNodes();
        
		Node node = tree.getNode(target);
		double height = node.getHeight();
		node.setHeight(height + EPSILON);
		if (node.getLength() < 0) {
			// abort when node is higher than its parent,
			// this should almost never happen
			return Double.NEGATIVE_INFINITY;
		}

		state.checkCalculationNodesDirtiness();
		double logPh = posterior.calculateLogP();
		
		double gradient = (logPh - logP) / EPSILON;
		
		double newHeight = height + 0.5 * gradient * windowSize + Randomizer.nextGaussian() * windowSize;

		// range check
		if (node.getLeft().getHeight() < newHeight && node.getRight().getHeight() < newHeight && 
				(node.isRoot() || node.getParent().getHeight() > newHeight)) {
			// state.storeCalculationNodes();
			node.setHeight(newHeight + EPSILON);
			state.checkCalculationNodesDirtiness();
			double logPhNew = posterior.calculateLogP();
			
			//state.storeCalculationNodes();
			node.setHeight(newHeight);
			state.checkCalculationNodesDirtiness();
			double logPNew = posterior.calculateLogP();
			double gradientBackward = (logPhNew - logPNew) / EPSILON;
			
			double logPForward = logPmove(newHeight - height - 0.5 * gradient * windowSize, windowSize);
			double logPBackward = logPmove(height - newHeight - 0.5 * gradientBackward * windowSize, windowSize);
			
			return logPBackward - logPForward; 
			
		}
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * probability a move of size "mean" is made using Randomizer.nextGaussian()
	 */
    private double logPmove(double mean, double standardDeviation) {    	
        final double x0 = 0 - mean;
        final double x1 = x0 / standardDeviation;
        final double logStandardDeviationPlusHalfLog2Pi = FastMath.log(standardDeviation) + 0.5 * FastMath.log(2 * FastMath.PI);
        return -0.5 * x1 * x1 - logStandardDeviationPlusHalfLog2Pi;
	}

	@Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
        double delta = calcDelta(logAlpha);

        delta += Math.log(windowSize);
        windowSize = Math.exp(delta);
        windowSize = Math.max(windowSize, lower);
        windowSize = Math.min(windowSize, upper);
    }

    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.5; /** 0.4 -- 0.85 suggested in Adrian Barbu & Song-Chun Zhu, Monte Carlo Methods, 2020 **/
    }
    
    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > 0.40) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
 
    @Override
    public boolean canHandleLeafTargets() {
    	return false;
    }
    
    @Override
    public boolean canHandleRootTargets() {
    	return true;
    }
	
	@Override
	public boolean requiresStateInitialisation() {
		return true;//false;
	}

	@Override
	public String getName() {
    	return "TargetableMALANodeOperator(" + getID() + ")"; 
	}
}
