package beastbooster.likelihood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

public class TreeLikelihoodF extends TreeLikelihood {
	final public Input<List<Frequencies>> freqListInput = new Input<>("f", "stationary frequencies at root, one for each category", new ArrayList<>(), Validate.REQUIRED);
	
	private List<Frequencies> freqList;
	private double [] freqArray;
	private int nrOfStates, nrOfPatterns, nrOfMatrices;
	private double [] outPartials;
	
	@Override
	public void initAndValidate() {
		System.setProperty("java.only", "true");
		super.initAndValidate();
		
		freqList = freqListInput.get();
		nrOfStates = freqList.get(0).getFreqs().length;
		nrOfMatrices = m_siteModel.getCategoryCount();
		nrOfPatterns = dataInput.get().getPatternCount();

		if (nrOfMatrices != freqList.size()) {
			throw new IllegalArgumentException("Number of frequencies (" + freqList.size() + " does not match number of categories in site model (" +nrOfMatrices +")");
		}
		freqArray = new double[nrOfMatrices * nrOfStates];
		
		m_fRootPartials = new double[nrOfMatrices * nrOfStates * nrOfPatterns];
		outPartials = new double[nrOfMatrices * nrOfStates * nrOfPatterns];
	}
	
	@Override
	protected int traverse(Node node) {

        int update = (node.isDirty() | hasDirt);

        final int nodeIndex = node.getNr();

        final double branchRate = branchRateModel.getRateForBranch(node);
        final double branchTime = node.getLength() * branchRate;

        // First update the transition probability matrix(ices) for this branch
        //if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_StoredBranchLengths[nodeIndex])) {
        
        if (!node.isRoot() && (update != Tree.IS_CLEAN || Math.abs(branchTime - m_branchLengths[nodeIndex]) > tolerance)) {
            m_branchLengths[nodeIndex] = branchTime;
            final Node parent = node.getParent();
            likelihoodCore.setNodeMatrixForUpdate(nodeIndex);
            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
                final double jointBranchRate = m_siteModel.getRateForCategory(i, node) * branchRate;
                substitutionModel.getTransitionProbabilities(node, parent.getHeight(), node.getHeight(), jointBranchRate, probabilities);
                //System.out.println(node.getNr() + " " + Arrays.toString(m_fProbabilities));
                likelihoodCore.setNodeMatrix(nodeIndex, i, probabilities);
            }
            update |= Tree.IS_DIRTY;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
            final Node child1 = node.getLeft(); //Two children
            final int update1 = traverse(child1);

            final Node child2 = node.getRight();
            final int update2 = traverse(child2);

            // If either child node was updated then update this node too
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN) {

                final int childNum1 = child1.getNr();
                final int childNum2 = child2.getNr();

                likelihoodCore.setNodePartialsForUpdate(nodeIndex);
                update |= (update1 | update2);
                if (update >= Tree.IS_FILTHY) {
                    likelihoodCore.setNodeStatesForUpdate(nodeIndex);
                }

                if (m_siteModel.integrateAcrossCategories()) {
                    likelihoodCore.calculatePartials(childNum1, childNum2, nodeIndex);
                } else {
                    throw new RuntimeException("Error TreeLikelihood 201: Site categories not supported");
                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }
                if (node.isRoot()) {
                    // No parent this is the root of the beast.tree -
                    // calculate the pattern likelihoods

                    final double[] proportions = m_siteModel.getCategoryProportions(node);
                    likelihoodCore.getNodePartials(nodeIndex, m_fRootPartials);
                    
                    for (int i = 0; i < freqList.size(); i++) {
                    	double [] freqs = freqList.get(i).getFreqs();
                		System.arraycopy(freqs, 0, freqArray, i * freqs.length, freqs.length);
                    }

                    
                    integratePartials(proportions, freqArray, m_fRootPartials, patternLogLikelihoods);

//                    if (constantPattern != null) { 
//                        proportionInvariant = m_siteModel.getProportionInvariant();
//                        // some portion of sites is invariant, so adjust root partials for this
//                        for (final int i : constantPattern) {
//                            m_fRootPartials[i] += proportionInvariant;
//                        }
//                    }
//
//                    double[] rootFrequencies = substitutionModel.getFrequencies();
//                    if (rootFrequenciesInput.get() != null) {
//                        rootFrequencies = rootFrequenciesInput.get().getFreqs();
//                    }
//                    likelihoodCore.calculateLogLikelihoods(m_fRootPartials, rootFrequencies, patternLogLikelihoods);
                }

            }
        }
        return update;
    }

	private void integratePartials(double[] proportions, double[] freqArray, double[] inPartials,
			double[] patternLogLikelihoods) {

        int u = 0;
        int v = 0;
        for (int k = 0; k < nrOfPatterns; k++) {

            for (int i = 0; i < nrOfStates; i++) {

                outPartials[u] = inPartials[v] * proportions[0] * freqArray[i];
                u++;
                v++;
            }
        }

        int w = 0;
        
        for (int l = 1; l < nrOfMatrices; l++) {
            u = 0;

            for (int k = 0; k < nrOfPatterns; k++) {
            	w = l * nrOfStates;
                for (int i = 0; i < nrOfStates; i++) {

                    outPartials[u] += inPartials[v] * proportions[l] * freqArray[w];
                    u++;
                    v++;
                    w++;
                }
            }
        }
        
        v = 0;
        for (int k = 0; k < nrOfPatterns; k++) {

            double sum = 0.0;
            for (int i = 0; i < nrOfStates; i++) {

                sum += outPartials[v];
                v++;
            }
            // TODO check that getLogScalingFactor works as advertised with this frequencies set up
            patternLogLikelihoods[k] = Math.log(sum) + likelihoodCore.getLogScalingFactor(k);
        }
	}
	
	@Override
	protected boolean requiresRecalculation() {
		boolean isDirty = super.requiresRecalculation();
		
		for (Frequencies freqs : freqList) {
			if (freqs.isDirtyCalculation()) {
	            hasDirt = Tree.IS_DIRTY;
			}
		}
		
		return isDirty || hasDirt != Tree.IS_CLEAN;
	}
}
