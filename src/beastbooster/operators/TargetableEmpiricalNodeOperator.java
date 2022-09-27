package beastbooster.operators;



import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;

@Description("Operator that moves internal node height (after transformation to make them "
		+ "more normally distributed) proposing samples from an empirical distribution that is "
		+ "learned during the MCMC run.") 
public class TargetableEmpiricalNodeOperator extends TreeOperator implements TargetableOperator {
    final public Input<Double> windowSizeInput = new Input<>("windowSize", "the size of standard deviation of Gaussian", Input.Validate.REQUIRED);

	final public Input<Double> betaInput = new Input<>("beta", "fraction of proposal determined by non-covariance matrix");
	
	final public Input<Integer> initialInput = new Input<>("initial", "Number of proposals before clade distribution is considered in proposal. "
			+ "If not specified (or < 0), the operator uses 200", 200);
	
	final public Input<Integer> burninInput = new Input<>("burnin", "Number of proposals that are ignored before clade distribution is being updated. "
			+ "If initial is not specified, uses half the default initial value (which equals 100 * number of internal nodes)", -1); 
	
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);
    final public Input<Boolean> storeInput = new Input<>("store", "flag to indicate if covariance and mean should be stored to the state file (default true)", true);

    final public Input<Double> stepUpperLimit = new Input<>("upper", "Upper Limit of step size", 10.0);
    final public Input<Double> stepLowerLimit = new Input<>("lower", "Lower limit of step size", 1e-8);

    final public Input<Mode> modeInput = new Input<>("mode", "proposal should make a MALA step, sample from empirical distribution, or random walk", Mode.empirical, Mode.values());

    enum Mode{MALA,empirical,walk};
    
	private double upper, lower;

    protected double windowSize;
    protected double beta;
    protected int initial, burnin;
    private boolean allowOptimisation;
    private int iterations;
    private Mode mode;


    /** maps clades onto objects containing updateable distributions **/
    private final Map<BitSet, CladeDist> cladeMap = new HashMap<>();
    
    Tree tree;
    
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
			return proposal(tree.getLeafNodeCount() + Randomizer.nextInt(tree.getInternalNodeCount()));
		}
	}

	@Override
	public void initAndValidate() {
    	
    	tree = treeInput.get();
    	mode = modeInput.get();
    	
    	int dim = tree.getInternalNodeCount();
        this.windowSize = windowSizeInput.get();
        this.beta = betaInput.get();
        
        this.initial = initialInput.get();        
        if (this.initial < 0) {
        	// options set according to recommendations in AVMVN paper
        	this.initial = 200;
        }

        this.burnin = burninInput.get();    	
        if (burnin < 0) {
            this.burnin = 100 * dim;
        }

        if (windowSize <= 0.0) {
            throw new IllegalArgumentException("ScaleFactor must be greater than zero.");
        }

        upper = stepUpperLimit.get();
		lower = stepLowerLimit.get();
		
		iterations = 0;
	}
    
    /** create new clade distribution **/
	private void addClade(Node node) {
		BitSet bits = new BitSet();
		for (Node leaf : node.getAllLeafNodes()) {
			bits.set(leaf.getNr());
		}
		updateCladeDist(bits, node);
	}

    /** returns clade distribution **/
	private CladeDist getClade(Node node) {
		BitSet bits = new BitSet();
		for (Node leaf : node.getAllLeafNodes()) {
			bits.set(leaf.getNr());
		}
		return cladeMap.get(bits);
	}

    /** update clade distribution **/
	private void updateCladeDist(BitSet bits, Node node) {
		CladeDist dist = cladeMap.get(bits);
		if (dist == null) {
			dist = new CladeDist();
			cladeMap.put(bits, dist);
		}
		dist.update(node);
	}


	/** represents log normal statistics for clade distribution **/
    private class CladeDist {
    	protected double total;
    	protected double total2;
    	protected int count;
    	
		public void update(Node node) {
			double logHeight = Math.log(node.getHeight());
			total += logHeight;
			total2 += logHeight * logHeight;
			count++;			
		}
		
		public double gradient(double x) {
			double m = logMean();
			double s = logStdev();
			
			// -(e^(-(x - μ)^2/(2 σ^2)) (x - μ))/(sqrt(2 π) σ^3)
			double df = -Math.exp(-(x-m)*(x-m)/(2 * s * s)) * (x-m) / (Math.sqrt(2 * Math.PI) * s * s * s);
			return df;
		}
		
		public double logMean() {
			return total / count;
		}
		
		public double logStdev() {
			final double mean = total / count;
			return Math.sqrt(total2/ count - mean * mean);
		}
    	
    }
    

	
//	int k = 0;
	
	@Override
	public double proposal(int target) {
		
		Node node = tree.getNode(target);
		if (node.isLeaf()) {
			// cannot handle leaf nodes
			return Double.NEGATIVE_INFINITY;
		}

		double oldHeight = node.getHeight();
		
		iterations++;

        if (iterations > 1 && iterations > burnin) {
        	addClade(node);
        }

        
        double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        double upper = node.getParent().getHeight();
        
        double newHeight = lower + Randomizer.nextDouble() * (upper - lower);

        
        double logHR = 0.0;
        CladeDist dist = getClade(node);
        if (dist != null && dist.count > initial) {
    		double logHeight = Math.log(oldHeight);
        	switch (mode) {
        	case MALA:
	    		double gradient = dist.gradient(logHeight);
	
	    		// MALA step in log space
	    		double newLogHeight = logHeight + 0.5 * gradient * windowSize + Randomizer.nextGaussian() * windowSize;
	    		newHeight = Math.exp(newLogHeight);
	    		
	    		// update HR
	    		double gradientBackward = dist.gradient(newLogHeight);
				double logPForward = logPmove(newLogHeight - logHeight - 0.5 * gradient * windowSize, windowSize);
				double logPBackward = logPmove(logHeight - newLogHeight - 0.5 * gradientBackward * windowSize, windowSize);
				logHR += logPBackward - logPForward;
				break;
        	case empirical:
            	double logEmpirical = dist.logMean() + Randomizer.nextGaussian() * dist.logStdev(); 
            	newHeight = beta * newHeight + (1-beta) * Math.exp(logEmpirical);
				break;
        	case walk:
            	double newLogHeight2 = logHeight + Randomizer.nextGaussian() * dist.logStdev() * windowSize;
            	newHeight = beta * newHeight + (1-beta) * Math.exp(newLogHeight2);
        		break;
        	}
        	
    		// update HR due to log-transform
            logHR += -Math.log(oldHeight) - -Math.log(newHeight);
            
//            k++;
//            if (k % 1000 == 0) {
//            	System.err.print(".");
//            }   
            
            allowOptimisation = true;
        } else {
        	allowOptimisation = false;
        }
        
        if (!node.isRoot() && newHeight > upper) {
        	return Double.NEGATIVE_INFINITY;
        }
        if (newHeight < lower) {
        	return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newHeight);

        return logHR;

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
    public void setCoercableParameterValue(double scaleFactor) {
    	this.windowSize = scaleFactor;
    }

    
    @Override
    public double getCoercableParameterValue() {
    	return windowSize;
    }
    
    @Override
    public void optimize(double logAlpha) {
    	if (optimiseInput.get() && allowOptimisation) {
            double delta = calcDelta(logAlpha);

            delta += Math.log(windowSize);
            windowSize = Math.exp(delta);
            windowSize = Math.max(windowSize, lower);
            windowSize = Math.min(windowSize, upper);
    	}
   }
    
    
    @Override
    public void accept() {
    	if (allowOptimisation) {
    		super.accept();
    	}
    }
    
    @Override
    public void reject(int reason) {
    	if (allowOptimisation) {
    		super.reject(reason);
    	}
    }
    
    final static double MINIMUM_ACCEPTANCE_LEVEL = 0.4;
    final static double MAXIMUM_ACCEPTANCE_LEVEL = 0.85;
    final static double MINIMUM_GOOD_ACCEPTANCE_LEVEL = 0.5;
    final static double MAXIMUM_GOOD_ACCEPTANCE_LEVEL = 0.75;    
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.625; /** 0.4 -- 0.85 suggested in Adrian Barbu & Song-Chun Zhu, Monte Carlo Methods, 2020 **/
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
        if (prob < MINIMUM_GOOD_ACCEPTANCE_LEVEL) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > MAXIMUM_GOOD_ACCEPTANCE_LEVEL) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
 
    
    
    
//    /**
//     * Since this operator additionally uses the covariances for proposal, they have to be stored to a file as well
//     */
//    @Override
//    public void storeToFile(final PrintWriter out) {
//    	try {
//	        JSONStringer json = new JSONStringer();
//	        json.object();
//	
//	        if (getID()==null)
//	           setID("unknown");
//	
//	        json.key("id").value(getID());
//	
//	        double p = getCoercableParameterValue();
//	        if (Double.isNaN(p)) {
//	            json.key("p").value("NaN");
//	        } else if (Double.isInfinite(p)) {
//	        	if (p > 0) {
//	        		json.key("p").value("Infinity");
//	        	} else {
//	        		json.key("p").value("-Infinity");
//	        	}
//	        } else {
//	            json.key("p").value(p);
//	        }
//	        
//	        if (storeInput.get()) {
//	        	// make the covariance matrix into an array	        
//		        int cov_length = empirical.length;
//		        int c = 0;
//		        double[] flat_cov = new double[(cov_length*cov_length-cov_length)/2+cov_length];
//		        for (int a = 0; a < empirical.length; a++) {
//		        	for (int b = a; b < empirical.length; b++) {
//		        		flat_cov[c] = empirical[a][b];
//		        		c++;
//		        	}
//		        } 
//		        json.key("means").value(Arrays.toString(oldMeans));
//		        json.key("covariance").value(Arrays.toString(flat_cov));	        
//	        }
//	        json.key("accept").value(m_nNrAccepted);
//	        json.key("reject").value(m_nNrRejected);
//	        json.key("acceptFC").value(m_nNrAcceptedForCorrection);
//	        json.key("rejectFC").value(m_nNrRejectedForCorrection);
//	        json.key("rejectIv").value(m_nNrRejectedInvalid);
//	        json.key("rejectOp").value(m_nNrRejectedOperator);
//	        json.endObject();
//	        out.print(json.toString());
//    	} catch (JSONException e) {
//    		// failed to log operator in state file
//    		// report and continue
//    		e.printStackTrace();
//    	}
//    }
//
//    @Override
//    public void restoreFromFile(JSONObject o) {
//
//    	try {
//    		if (storeInput.get()) {
//	    		String[] means_string = ((String) o.getString("means")).replace("[", "").replace("]", "").split(", ");
//		        String[] cov_string = ((String) o.getString("covariance")).replace("[", "").replace("]", "").split(", ");
//		        
//		        
//		        oldMeans = new double[means_string.length];
//		        for (int a = 0; a < oldMeans.length; a++)
//		        	oldMeans[a] = Double.parseDouble(means_string[a]);
//		        
//		        empirical = new double[oldMeans.length][oldMeans.length];
//		        int c = 0;
//		        for (int a = 0; a < empirical.length; a++) {
//		        	for (int b = a; b < empirical.length; b++) {
//		        		empirical[a][b] = Double.parseDouble(cov_string[c]);
//		        		empirical[b][a] = Double.parseDouble(cov_string[c]);
//		        		c++;
//		        	}	        	
//		    	}    	
//    		}else {
//    	        this.empirical = new double[dim][dim];
//    	        this.oldMeans = new double[dim];
//    	        this.newMeans = new double[dim];
//
//    			
//                for (int i = 0; i < dim; i++) {
//                    //oldMeans[i] = transformedX[i];
//                    //newMeans[i] = transformedX[i];
//                    oldMeans[i] = 0.0;
//                    newMeans[i] = 0.0;
//                }
//
//                for (int i = 0; i < dim; i++) {
//                    for (int j = 0; j < dim; j++) {
//                        empirical[i][j] = 0.0;
//                    }
//                }
//    		}
//	        super.restoreFromFile(o);  	
//    	} catch (JSONException e) {
//    		// failed to restore from state file
//    		// report and continue
//    		e.printStackTrace();
//    	}
//    }

	
	@Override
	public boolean canHandleLeafTargets() {
		return false;
	}

	@Override
	public boolean canHandleRootTargets() {
		return false;
	}
	
	@Override
    public String getName() {
    	return "TargetableEmpiricalNodeOperator(" + getID() + ")"; 
    }
    
}
