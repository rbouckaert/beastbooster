package test.beastbooster.likelihood;

import java.util.concurrent.Executors;

import beastfx.app.beast.BeastMCMC;
import beast.base.core.ProgramStatus;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beastbooster.likelihood.DuckThreadedTreeLikelihood;

//all test cases are in beast.evolution.likelihood.DuckTreeLikelihoodTest
public class DuckThreadedTreeLikelihoodTest extends DuckTreeLikelihoodTest {

    public DuckThreadedTreeLikelihoodTest() {
		super();
		ProgramStatus.m_nThreads = 2;
		ProgramStatus.g_exec = Executors.newFixedThreadPool(ProgramStatus.m_nThreads);
	}

    @Override
    protected GenericTreeLikelihood newTreeLikelihood() {
    	System.setProperty("java.only","false");
        return new DuckThreadedTreeLikelihood();
    }
    
    @Override
    public void testJC69LikelihoodWithUncertainCharacters() throws Exception {
    	// ThreadedTreeLikelihood does not handle uncertain characters, so skip this test
    }
    
    @Override
    public void testAscertainedJC69Likelihood() throws Exception {
    	// ThreadedTreeLikelihood does not handle ascertainment correction, so skip this test
    }
    
} // class DuckThreadedTreeLikelihoodTest
