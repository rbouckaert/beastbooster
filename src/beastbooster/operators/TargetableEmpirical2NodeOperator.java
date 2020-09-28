package beastbooster.operators;



import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import beast.evolution.alignment.TaxonSet;
import beast.util.FrequencySet;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.KernelOperator;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.CladeSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Transform.*;
import beast.util.Randomizer;
import beast.util.Transform;
import beast.math.matrixalgebra.*;

@Description("Operator that moves 2 internal node heights (after transformation to make them "
		+ "more normally distributed) proposing samples from an empirical distribution that is "
		+ "learned during the MCMC run.") 
public class TargetableEmpirical2NodeOperator extends TargetableEmpiricalNodeOperator {

    /** maps clades onto objects containing updateable distributions **/
    private final Map<BitSet, CladeWithParentDist> cladeWithParentMap = new HashMap<>();
    
    Tree tree;
    
    @Override
	public void initAndValidate() {
    	
    	tree = treeInput.get();
    	
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
	private CladeWithParentDist getClade(Node node) {
		BitSet bits = new BitSet();
		for (Node leaf : node.getAllLeafNodes()) {
			bits.set(leaf.getNr());
		}
		return cladeWithParentMap.get(bits);
	}

    /** update clade distribution **/
	private void updateCladeDist(BitSet bits, Node node) {
		CladeWithParentDist dist = cladeWithParentMap.get(bits);
		if (dist == null) {
			dist = new CladeWithParentDist();
			cladeWithParentMap.put(bits, dist);
		}
		dist.update(node);
	}


	/** represents log normal statistics for clade distribution **/
    private class CladeWithParentDist {
    	protected int count;
    	private double [] mean;
    	private double [][] corr;
    	
    	public CladeWithParentDist() {
			mean = new double[2];
			corr = new double[2][2];
		}
    	
		public void update(Node node) {
			count++;			
			final double total = mean[0], ptotal = mean[1];
			double logHeight = Math.log(node.getHeight());
			double logPHeight = Math.log(node.getParent().getHeight());
			
			double newTotal = ((count-1) * total + logHeight) / count; 
			double newPTotal = ((count-1) * ptotal + logPHeight) / count; 
					
	    	// Welford-style update
	    	// https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm
	        double result = corr[0][1] * (count - 2);
	        result += (logHeight * logPHeight);
	        result += ((count - 1) * total * ptotal - count * newTotal * newPTotal);
	        result /= ((double)(count - 1));
	        corr[0][1] = result;
	        corr[1][0] = corr[0][1];
			
	        result = corr[0][0] * (count - 2);
	        result += (logHeight * logHeight);
	        result += ((count - 1) * total * total - count * newTotal * newTotal);
	        result /= ((double)(count - 1));
	        corr[0][0] = result;
	        
	        result = corr[1][1] * (count - 2);
	        result += (logPHeight * logPHeight);
	        result += ((count - 1) * ptotal * ptotal - count * newPTotal * newPTotal);
	        result /= ((double)(count - 1));
	        corr[1][1] = result;
	        
	        mean[0] = newTotal;
	        mean[1] = newPTotal;
		}
		
		public double [] logMean() {
			return mean;
		}
		
		
		public double [][] cholesky() {
			double [][] L = new double[2][2];
//				int j = 0; 
//				//double[] Lrowj = L[j];
//				double d = 0.0;
////				for (int k = 0; k < j; k++) {
////					double[] Lrowk = L[k];
////					double s = 0.0;
////					for (int i = 0; i < k; i++) {
////						s += Lrowk[i] * Lrowj[i];
////					}
////					Lrowj[k] = s = (corr[j][k] - s) / L[k][k];
////					d = d + s * s;
////				}
//				d = corr[j][j] - d;
//				L[j][j] = Math.sqrt(Math.max(d, 0.0));
//
//				j = 1;
//				double [] Lrowj = L[j];
//				d = 0.0;
//				int k = 0; 
////					double [] Lrowk = L[k];
//					double s = 0.0;
////					for (int i = 0; i < k; i++) {
////						s += Lrowk[i] * Lrowj[i];
////					}
//					Lrowj[k] = s = (corr[j][k] - s) / L[k][k];
//					d = d + s * s;
//
//				d = corr[j][j] - d;
//				L[j][j] = Math.sqrt(Math.max(d, 0.0));
				
			L[0][0] = Math.sqrt(Math.max(corr[0][0], 0));
			L[1][0] = corr[1][0] / L[0][0];
			L[0][1] = L[1][0];
 			double d = L[1][0] * L[1][0]; 
			L[1][1] = Math.sqrt(Math.max(corr[1][1] - d, 0));
			return L;

		}
    	
    }
    

	@Override
	public double proposal() {
		return proposal(tree.getLeafNodeCount() + Randomizer.nextInt(tree.getInternalNodeCount()));
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
		double oldPHeight = node.getParent().getHeight();
		
        int iterations = (m_nNrAccepted + m_nNrRejected);

        if (iterations > 1 && iterations > burnin) {
        	addClade(node);
        }

        
        double epsilon = windowSize * Randomizer.nextGaussian();
        double newHeight = oldHeight + epsilon;
        epsilon = windowSize * Randomizer.nextGaussian();
        double newPHeight = oldPHeight + epsilon;
        
        double logHR = 0.0;
        CladeWithParentDist dist = getClade(node);
        if (dist != null && dist.count > initial) {
            
            double [] logMean = dist.logMean();            
            double [][] cholesky = dist.cholesky();
            
            double [] eps = new double[] {Randomizer.nextGaussian(), Randomizer.nextGaussian()};
            
            double logEmpirical = logMean[0] + eps[0] * cholesky[0][0] + eps[1] * cholesky[0][1];
            double logPEmpirical= logMean[1] + eps[0] * cholesky[1][0] + eps[1] * cholesky[1][1];
            newHeight = beta * newHeight + (1-beta) * Math.exp(logEmpirical);
            newPHeight = beta * newPHeight + (1-beta) * Math.exp(logPEmpirical);

            logHR += -Math.log(oldHeight) - -Math.log(newHeight);
            
            logHR += -Math.log(oldPHeight) - -Math.log(newPHeight);
            
//            k++;
//            if (k % 1000 == 0) {
//            	System.err.print(".");
//            }            
        }
        if (!node.isRoot() && newHeight > node.getParent().getHeight()) {
        	return Double.NEGATIVE_INFINITY;
        }
        if (newHeight < node.getLeft().getHeight() || newHeight < node.getRight().getHeight()) {
        	return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newHeight);
        node.getParent().setHeight(newPHeight);

        return logHR;

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
}
