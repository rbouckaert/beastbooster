package beastbooster.operators;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.operator.kernel.BactrianNodeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;

@Description("Node operator that proposes node heights in around MRCA node. "
		+ "It randomly picks the MRCA node specied by a taxonset, or its parent or one of its children.")
public class MRCABactrianNodeOperator extends BactrianNodeOperator  {
	public Input<TaxonSet> taxonsetInput = new Input<>("taxonset", "specifies the set of taxa for which this operator moves the MRCA node", Validate.REQUIRED);
	
    boolean initialised = false;
    // array of indices of taxa
    int[] taxonIndex;
    // number of taxa in taxon set
    int nrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();
    Tree tree;
	
	@Override
	public void initAndValidate() {
		super.initAndValidate();
		tree = treeInput.get();        
	}

	@Override
	public double proposal() {
    	Node node = getCommonAncestor();
    	switch (Randomizer.nextInt(4)) {
    	case 0: 
    		node = node.getParent();
    		break;
    	case 1:
    		node = node.getLeft();
    		break;
    	case 2:
    		node = node.getRight();
    		break;
    	default:
    		// node = node;
    	}

        if (node == null|| node.isLeaf() || node.isRoot())
            return Double.NEGATIVE_INFINITY;
        
        double upper = node.getParent().getHeight();
        double lower = Math.max(node.getLeft().getHeight(), node.getRight().getHeight());
        
        double scale = kernelDistribution.getScaler(0, Double.NaN, scaleFactor);

        // transform value
        double value = node.getHeight();
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);
        
        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }
        
        node.setHeight(newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

    protected void initialise() {
        // determine which taxa are in the set
    	
        List<String> set = null;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
        }
        final List<String> taxaNames = new ArrayList<>();
        Tree tree = treeInput.get();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }
        
        nrOfTaxa = set.size();
        taxonIndex = new int[nrOfTaxa];
        isInTaxaSet.clear();
        int k = 0;
        for (final String taxon : set) {
            final int taxonIndex_ = taxaNames.indexOf(taxon);
            if (taxonIndex_ < 0) {
                throw new RuntimeException("Cannot find taxon " + taxon + " in data");
            }
            if (isInTaxaSet.contains(taxon)) {
                throw new RuntimeException("Taxon " + taxon + " is defined multiple times, while they should be unique");
            }
            isInTaxaSet.add(taxon);
            taxonIndex[k++] = taxonIndex_;
        }
        initialised = true;
 	} 
    
    boolean [] nodesTraversed;
    int nseen;

    protected Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        if( ! nodesTraversed[n1.getNr()] ) {
            nodesTraversed[n1.getNr()] = true;
            nseen += 1;
        }
        if( ! nodesTraversed[n2.getNr()] ) {
            nodesTraversed[n2.getNr()] = true;
            nseen += 1;
        }
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	            if( ! nodesTraversed[n1.getNr()] ) {
	                nodesTraversed[n1.getNr()] = true;
	                nseen += 1;
	            }
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	            if( ! nodesTraversed[n2.getNr()] ) {
	                nodesTraversed[n2.getNr()] = true;
	                nseen += 1;
	            }
	        } else {
	            //zero length branches hell
	            Node n;
	            double b1 = n1.getLength();
	            double b2 = n2.getLength();
	            if( b1 > 0 ) {
	                n = n2;
	            } else { // b1 == 0
	                if( b2 > 0 ) {
	                    n = n1;
	                } else {
	                    // both 0
	                    n = n1;
	                    while( n != null && n != n2 ) {
	                        n = n.getParent();
	                    }
	                    if( n == n2 ) {
	                        // n2 is an ancestor of n1
	                        n = n1;
	                    } else {
	                        // always safe to advance n2
	                        n = n2;
	                    }
	                }
	            }
	            if( n == n1 ) {
                    n = n1 = n.getParent();
                } else {
                    n = n2 = n.getParent();
                }
	            if( ! nodesTraversed[n.getNr()] ) {
	                nodesTraversed[n.getNr()] = true;
	                nseen += 1;
	            } 
	        }
        }
        return n1;
    }

    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    public Node getCommonAncestor() {
        if (!initialised) {
            initialise();
        }
        nodesTraversed = new boolean[tree.getNodeCount()];
        Node n = getCommonAncestorInternal();
        return n;
    }

    private Node getCommonAncestorInternal() {
        Node cur = tree.getNode(taxonIndex[0]);

        for (int k = 1; k < taxonIndex.length; ++k) {
            cur = getCommonAncestor(cur, tree.getNode(taxonIndex[k]));
        }
        return cur;
    }
    
    @Override
	public String getName() {
    	return "MRCABactrianNodeOperator(" + getID() + ")"; 
	}
}
