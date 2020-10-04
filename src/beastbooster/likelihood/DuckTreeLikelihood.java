package beastbooster.likelihood;

import beast.core.Description;
import beast.evolution.datatype.DataType;
import beast.evolution.likelihood.BeerLikelihoodCore;
import beast.evolution.likelihood.BeerLikelihoodCore4;
import beast.evolution.likelihood.LikelihoodCore;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;

@Description("TreeLikelihood that can target an internal node as end-point instead of the root."
		+ "This can be more efficient than its parent class if many updates can be expected "
		+ "in the same area of the tree.")
public class DuckTreeLikelihood extends TreeLikelihood  implements Targetable {

	int target = -1;
	
	public void setTarget(int target) {
		if (target < treeInput.get().getLeafNodeCount() || 
			target >= treeInput.get().getNodeCount()) {
			throw new IllegalArgumentException("target should be an internal node in range " + 
					treeInput.get().getLeafNodeCount() + " -- " + 
					(treeInput.get().getNodeCount()-1) + " not " + target);
		}
		if (beagle != null) {
			((DuckBeagleTreeLikelihood) beagle).setTarget(target);
			return;
		}
		if (this.target >= 0) {
			m_branchLengths[this.target] = -1;
		}
		this.target = target;
		m_branchLengths[target] = -1;
	}
	
	
    @Override
    public void initAndValidate() {
        // sanity check: alignment should have same #taxa as tree
        if (dataInput.get().getTaxonCount() != treeInput.get().getLeafNodeCount()) {
            throw new IllegalArgumentException("The number of nodes in the tree does not match the number of sequences");
        }
        beagle = null;
        beagle = new DuckBeagleTreeLikelihood();
        try {
	        beagle.initByName(
                    "data", dataInput.get(), "tree", treeInput.get(), "siteModel", siteModelInput.get(),
                    "branchRateModel", branchRateModelInput.get(), "useAmbiguities", m_useAmbiguities.get(), 
                    "useTipLikelihoods", m_useTipLikelihoods.get(),"scaling", scaling.get().toString());
	        if (beagle.getBeagle() != null) {
	            //a Beagle instance was found, so we use it
	            return;
	        }
        } catch (Exception e) {
			// ignore
		}
        // No Beagle instance was found, so we use the good old java likelihood core
        beagle = null;
        String javaProperty = System.getProperty("java.only");
    	System.setProperty("java.only", "true");
    	super.initAndValidate();
    	if (javaProperty != null) {
    		System.setProperty("java.only", javaProperty);
    	}else {
    		System.clearProperty("java.only");
    	}
    }
    
    
	@Override
    protected LikelihoodCore createLikelihoodCore(int stateCount) {
		if (stateCount == 4) {
			return new DuckLikelihoodCore4(stateCount);
		} else {
			return new DuckLikelihoodCore(stateCount);
		}
    }

	@Override
	protected int traverse(Node node) {
		TreeInterface tree = treeInput.get();
		if (target == -1 || tree.getNode(target).isRoot()) {
			return super.traverse(node);
		}
		return traverseToTarget(tree.getNode(target), null);
	}


	private int traverseToTarget(Node node, Node origin) {
        int update = (node.isDirty() | hasDirt);

        final int nodeIndex = node.getNr();

        boolean updateTarget = m_branchLengths[nodeIndex] < 0;

        if (origin != null) {
        	double branchRate, branchTime;
        	if (node.getParent().isRoot() && origin.getParent().isRoot()) {
            	final double branchRate1 = branchRateModel.getRateForBranch(node);
            	final double branchRate2 = branchRateModel.getRateForBranch(origin);
            	Node root = node.getParent();
            	final double branchTime1 = root.getHeight() - node.getHeight();
            	final double branchTime2 = root.getHeight() - origin.getHeight();
            	branchTime = branchTime1 * branchRate1 + branchTime2 * branchRate2;
            	branchRate = branchTime / (branchTime1 + branchTime2);
        	} else {
            	branchRate = node.getParent() == origin ? branchRateModel.getRateForBranch(node) : branchRateModel.getRateForBranch(origin);
            	branchTime = Math.abs(node.getHeight() - origin.getHeight()) * branchRate;
        	}

	        // First update the transition probability matrix(ices) for this branch
	        //if (!node.isRoot() && (update != Tree.IS_CLEAN || branchTime != m_StoredBranchLengths[nodeIndex])) {
	        if (update != Tree.IS_CLEAN || Math.abs(branchTime - m_branchLengths[nodeIndex]) > 1e-13) {
	            m_branchLengths[nodeIndex] = branchTime;
	            final Node parentX = node.getParent();
	            likelihoodCore.setNodeMatrixForUpdate(nodeIndex);
	            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
	                if (node.getParent() == origin) {
		                final double jointBranchRate = m_siteModel.getRateForCategory(i, node) * branchRate;
	                	substitutionModel.getTransitionProbabilities(node, origin.getHeight(), node.getHeight(), jointBranchRate, probabilities);
	                } else if (origin.getParent() == node) {
		                final double jointBranchRate = m_siteModel.getRateForCategory(i, origin) * branchRate;
	                	substitutionModel.getTransitionProbabilities(node, node.getHeight(), origin.getHeight(), jointBranchRate, probabilities);
	                } else if (node.getParent().isRoot()) {
		                final double jointBranchRate = m_siteModel.getRateForCategory(i, node) * branchRate;
	                	substitutionModel.getTransitionProbabilities(node, node.getHeight() + branchTime/branchRate, node.getHeight(), jointBranchRate, probabilities);	                	
	            	} else {
	            		throw new RuntimeException("Programmer error: should not get here at " + this.getClass().getSimpleName() + ".traverse()");
	                }
	                //System.out.println(node.getNr() + " " + Arrays.toString(m_fProbabilities));
	                likelihoodCore.setNodeMatrix(nodeIndex, i, probabilities);
	            }
	            update |= Tree.IS_DIRTY;
	        }
        } else {
            for (int i = 0; i < m_siteModel.getCategoryCount(); i++) {
            	substitutionModel.getTransitionProbabilities(node, node.getHeight(), node.getHeight(), 0.0, probabilities);
                likelihoodCore.setNodeMatrix(nodeIndex, i, probabilities);
            }
            m_branchLengths[nodeIndex] = 0.0;
        }

        // If the node is internal, update the partial likelihoods.
        if (!node.isLeaf()) {

            // Traverse down the two child nodes
        	Node neighbour1, neighbour2, neighbour3 = null;
        	int update1 = Tree.IS_CLEAN;
        	int update2 = Tree.IS_CLEAN;
        	int update3 = Tree.IS_CLEAN;
        	if (node.getParent() == origin || (node.getParent().isRoot() && origin != null && origin.getParent().isRoot())) {
        		// going down
        		neighbour1 = node.getLeft(); //Two children
        		update1 |= traverseToTarget(neighbour1, node);

        		neighbour2 = node.getRight();
        		update2 |= traverseToTarget(neighbour2, node);
        	} else if (node.getLeft() == origin) {
        		// coming from the left
        		neighbour1 = node.getParent();
        		if (neighbour1.isRoot()) {
        			neighbour1 = neighbour1.getLeft() == node ? neighbour1.getRight() : neighbour1.getLeft();
        		}
        		update1 |= traverseToTarget(neighbour1, node);
        		
        		neighbour2 = node.getRight();
        		update2 |= traverseToTarget(neighbour2, node);
        		
        	} else if (node.getRight() == origin) {
        		// coming from the right
        		neighbour1 = node.getLeft(); //Two children
        		update1 |= traverseToTarget(neighbour1, node);
        		
        		neighbour2 = node.getParent();
        		if (neighbour2.isRoot()) {
        			neighbour2 = neighbour2.getLeft() == node ? neighbour2.getRight() : neighbour2.getLeft();
        		}
        		update2 |= traverseToTarget(neighbour2, node);
        	} else if (origin == null) {
        		// going down
        		neighbour1 = node.getLeft(); //Two children
        		update1 |= traverseToTarget(neighbour1, node);

        		neighbour2 = node.getRight();
        		update2 |= traverseToTarget(neighbour2, node);

        		// going up
        		neighbour3 = node.getParent();
        		if (neighbour3.isRoot()) {
        			neighbour3 = neighbour3.getLeft() == node ? neighbour3.getRight() : neighbour3.getLeft();
        		}
        		update3 = traverseToTarget(neighbour3, node);
        	} else {
        		throw new RuntimeException("Programmer error: should not get at " + this.getClass().getSimpleName() + ".traverse()");
        	}

        	if (true) {
            // If either child node was updated then update this node too
        	if (origin == null) { // && (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN || updateTarget)) {
            	int rootIndex = treeInput.get().getRoot().getNr();
                likelihoodCore.setNodePartialsForUpdate(rootIndex);
                if (update >= Tree.IS_FILTHY) {
                    likelihoodCore.setNodeStatesForUpdate(rootIndex);
                }

                if (m_siteModel.integrateAcrossCategories()) {
                    ((DuckLikelihoodCore) likelihoodCore).calculatePartials(neighbour1.getNr(), neighbour2.getNr(), neighbour3.getNr(), rootIndex);
                } else {
                    throw new RuntimeException("Error TreeLikelihood 201: Site categories not supported");
                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }
                update |= Tree.IS_DIRTY;
        		
        	} else 
            if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN || updateTarget) {

                likelihoodCore.setNodePartialsForUpdate(nodeIndex);
                if (update >= Tree.IS_FILTHY) {
                    likelihoodCore.setNodeStatesForUpdate(nodeIndex);
                }

                if (m_siteModel.integrateAcrossCategories()) {
                    likelihoodCore.calculatePartials(neighbour1.getNr(), neighbour2.getNr(), nodeIndex);
                } else {
                    throw new RuntimeException("Error TreeLikelihood 201: Site categories not supported");
                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }
                update |= Tree.IS_DIRTY;
            }
        	} else {
                if (update1 != Tree.IS_CLEAN || update2 != Tree.IS_CLEAN || updateTarget) {

                    likelihoodCore.setNodePartialsForUpdate(nodeIndex);
                    if (update >= Tree.IS_FILTHY) {
                        likelihoodCore.setNodeStatesForUpdate(nodeIndex);
                    }

                    if (m_siteModel.integrateAcrossCategories()) {
                        likelihoodCore.calculatePartials(neighbour1.getNr(), neighbour2.getNr(), nodeIndex);
                    } else {
                        throw new RuntimeException("Error TreeLikelihood 201: Site categories not supported");
                        //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                    }
                    update |= Tree.IS_DIRTY;
                }            	

	            if (origin == null) { // update3 != Tree.IS_CLEAN) {
	            	int rootIndex = treeInput.get().getRoot().getNr();
	                likelihoodCore.setNodePartialsForUpdate(rootIndex);
	                if (update >= Tree.IS_FILTHY) {
	                    likelihoodCore.setNodeStatesForUpdate(rootIndex);
	                }
	
	                if (m_siteModel.integrateAcrossCategories()) {
	                    likelihoodCore.calculatePartials(nodeIndex, neighbour3.getNr(), rootIndex);
	                } else {
	                    throw new RuntimeException("Error TreeLikelihood 201: Site categories not supported");
	                    //m_pLikelihoodCore->calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
	                }
	                update |= Tree.IS_DIRTY;
	            }
            }
            
            if (origin == null) {
            	int rootIndex = treeInput.get().getRoot().getNr();
                // No parent this is the root of the beast.tree -
                // calculate the pattern likelihoods
                final double[] frequencies = //m_pFreqs.get().
                        substitutionModel.getFrequencies();

                final double[] proportions = m_siteModel.getCategoryProportions(node);
                likelihoodCore.integratePartials(rootIndex, proportions, m_fRootPartials);

                if (getConstantPattern() != null) { // && !SiteModel.g_bUseOriginal) {
                    setProportionInvariant(m_siteModel.getProportionInvariant());
                    // some portion of sites is invariant, so adjust root partials for this
                    for (final int i : getConstantPattern()) {
                        m_fRootPartials[i] += getProportionInvariant();
                    }
                }

                likelihoodCore.calculateLogLikelihoods(m_fRootPartials, frequencies, patternLogLikelihoods);
            }

        }
        return update;		
	}

	
	public double [] getRootPartials() {
		DataType dataType = dataInput.get().getDataType();			
		double [] partials = new double[patternLogLikelihoods.length * dataType.getStateCount() * m_siteModel.getCategoryCount()];
		if (beagle != null) {			
			beagle.getLikelihoodCore().getNodePartials(treeInput.get().getRoot().getNr(), partials);
		} else {
			likelihoodCore.getNodePartials(treeInput.get().getRoot().getNr(), partials);
		}
		return partials;
	}
}
