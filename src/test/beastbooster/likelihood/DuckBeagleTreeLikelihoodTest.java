package test.beastbooster.likelihood;

import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beastbooster.likelihood.DuckBeagleTreeLikelihood;

// all test cases are in beast.evolution.likelihood.DuckTreeLikelihoodTest
public class DuckBeagleTreeLikelihoodTest extends DuckTreeLikelihoodTest {

    public DuckBeagleTreeLikelihoodTest() {
        super();
    }

    @Override
    protected GenericTreeLikelihood newTreeLikelihood() {
    	System.setProperty("java.only","false");
        return new DuckBeagleTreeLikelihood();
    }

} // class DuckBeagleTreeLikelihoodTest
