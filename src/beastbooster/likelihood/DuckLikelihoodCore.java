package beastbooster.likelihood;

import beast.base.evolution.likelihood.BeerLikelihoodCore;

public class DuckLikelihoodCore extends BeerLikelihoodCore {

	public DuckLikelihoodCore(int nrOfStates) {
		super(nrOfStates);
	}

	/**
	 * Calculates partial likelihoods at a node.
	 *
	 * @param nodeIndex1
	 *            the 'child 1' node
	 * @param nodeIndex2
	 *            the 'child 2' node
	 * @param nodeIndex3
	 *            the 'child 3' node
	 * @param nodeIndex4
	 *            the 'parent' node
	 */
	public void calculatePartials(int nodeIndex1, int nodeIndex2, int nodeIndex3, int nodeIndex4) {
		if (states[nodeIndex1] != null) {
			if (states[nodeIndex2] != null) {
				if (states[nodeIndex3] != null) {
					calculateStatesStatesStatesPruning(states[nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1], states[nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2], states[nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);
				} else {
					calculateStatesStatesPartialsPruning(states[nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1], states[nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex3]][nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);
				}
			} else {
				if (states[nodeIndex3] != null) {
					calculateStatesStatesPartialsPruning(states[nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1], states[nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex2]][nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);

				} else {
					calculateStatesPartialsPartialsPruning(states[nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1],
							partials[currentPartialsIndex[nodeIndex2]][nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex3]][nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);
				}
			}

		} else {
			if (states[nodeIndex2] != null) {
				if (states[nodeIndex3] != null) {
					calculateStatesStatesPartialsPruning(states[nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2], states[nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex1]][nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);
				} else {
					calculateStatesPartialsPartialsPruning(states[nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex1]][nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1],
							partials[currentPartialsIndex[nodeIndex3]][nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);

				}
			} else {
				if (states[nodeIndex3] != null) {
					calculateStatesPartialsPartialsPruning(states[nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex1]][nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1],
							partials[currentPartialsIndex[nodeIndex2]][nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);

				} else {
					calculatePartialsPartialsPartialsPruning(partials[currentPartialsIndex[nodeIndex1]][nodeIndex1],
							matrices[currentMatrixIndex[nodeIndex1]][nodeIndex1],
							partials[currentPartialsIndex[nodeIndex2]][nodeIndex2],
							matrices[currentMatrixIndex[nodeIndex2]][nodeIndex2],
							partials[currentPartialsIndex[nodeIndex3]][nodeIndex3],
							matrices[currentMatrixIndex[nodeIndex3]][nodeIndex3],
							partials[currentPartialsIndex[nodeIndex4]][nodeIndex4]);
				}
			}
		}

		if (useScaling) {
			scalePartials(nodeIndex3);
		}
	}

	protected void calculateStatesStatesStatesPruning(
			int[] stateIndex1, double[] matrices1,
            int[] stateIndex2, double[] matrices2,
			int[] stateIndex3, double[] matrices3, 
			double[] partials4) {
		   // should never get here for binary tree?
		
	       int v = 0;

	        for (int l = 0; l < nrOfMatrices; l++) {

	            for (int k = 0; k < nrOfPatterns; k++) {

	                int state1 = stateIndex1[k];
	                int state2 = stateIndex2[k];
	                int state3 = stateIndex3[k];

	                int w = l * matrixSize;

	                if (state1 < nrOfStates) {
	                	if (state2 < nrOfStates) {
		                	if (state3 < nrOfStates) {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += nrOfStates;
			                    }
		                	} else {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2];
			                        v++;
			                        w += nrOfStates;
			                    }
		                	}
	                	} else {
	                		if (state3 < nrOfStates) {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += nrOfStates;
			                    }
	                		} else {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices1[w + state1];
			                        v++;
			                        w += nrOfStates;
			                    }
	                		}
	                	}
	                } else {
	                	if (state2 < nrOfStates) {
		                	if (state3 < nrOfStates) {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += nrOfStates;
			                    }
		                	} else {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices2[w + state2];
			                        v++;
			                        w += nrOfStates;
			                    }		                		
		                	}
	                	} else {
	                		if (state3 < nrOfStates) {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = matrices3[w + state3];
			                        v++;
			                        w += nrOfStates;
			                    }
	                		} else {
			                    for (int i = 0; i < nrOfStates; i++) {
			                        partials4[v] = 1.0;
			                        v++;
			                    }
	                		}
	                	}
	                }
	            }
	        }
	}

	protected void calculateStatesStatesPartialsPruning(
			  int[] stateIndex1, double[] matrices1,
              int[] stateIndex2, double[] matrices2,
			  double[] partials3, double[] matrices3, 
			  double[] partials4) {

		double sum;
        int v = 0, u = 0;

        for (int l = 0; l < nrOfMatrices; l++) {

            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = stateIndex1[k];
                int state2 = stateIndex2[k];

                int w = l * matrixSize;


                if (state1 < nrOfStates && state2 < nrOfStates) {

                    for (int i = 0; i < nrOfStates; i++) {

                    	partials4[v] = matrices1[w + state1] * matrices2[w + state2];
                    	sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices3[w] * partials3[u + j];
                            w++;
                        }
                        partials4[v] *= sum;

                        v++;                        
                    }
                    u += nrOfStates;

                } else if (state1 < nrOfStates) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < nrOfStates; i++) {

                        partials4[v] = matrices1[w + state1];
                    	sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices3[w] * partials3[u + j];
                            w++;
                        }
                        partials4[v] *= sum;

                        v++;
                        // w += nrOfStates;
                    }
                    u += nrOfStates;
                } else if (state2 < nrOfStates) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < nrOfStates; i++) {

                        partials4[v] = matrices2[w + state2];
                    	sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices3[w] * partials3[u + j];
                            w++;
                        }
                        partials4[v] *= sum;

                        v++;
                        // w += nrOfStates;
                    }
                    u += nrOfStates;
                } else {
                    // both children have a gap or unknown state so set partials to 1
                    for (int i = 0; i < nrOfStates; i++) {
                    	sum = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices3[w] * partials3[u + j];
                            w++;
                        }
                        partials4[v] = sum;
                        v++;
                    }
                    u += nrOfStates;
                }
            }
        }
	  
	}

	
    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected void calculateStatesPartialsPartialsPruning(int[] stateIndex1, double[] matrices1,
                                                  double[] partials2, double[] matrices2,
                                      			  double[] partials3, double[] matrices3, 
                                    			  double[] partials4) {

        double sum, sum2, tmp;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {
            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = stateIndex1[k];

                int w = l * matrixSize;

                if (state1 < nrOfStates) {


                    for (int i = 0; i < nrOfStates; i++) {

                        tmp = matrices1[w + state1];

                        sum = sum2 = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            sum2 += matrices3[w] * partials3[v + j];
                            w++;
                        }

                        partials4[u] = tmp * sum * sum2;
                        u++;
                    }

                    v += nrOfStates;
                } else {
                    // Child 1 has a gap or unknown state so don't use it

                    for (int i = 0; i < nrOfStates; i++) {

                        sum = sum2 = 0.0;
                        for (int j = 0; j < nrOfStates; j++) {
                            sum += matrices2[w] * partials2[v + j];
                            sum2 += matrices3[w] * partials3[v + j];
                            w++;
                        }

                        partials4[u] = sum * sum2;
                        u++;
                    }

                    v += nrOfStates;
                }
            }
        }
    }

	
	
	
	
	protected void calculatePartialsPartialsPartialsPruning(
			double[] partials1, double[] matrices1, 
			double[] partials2, double[] matrices2, 
			double[] partials3, double[] matrices3, 
			double[] partials4) {
		double sum1, sum2, sum3;

		int u = 0;
		int v = 0;

		for (int l = 0; l < nrOfMatrices; l++) {

			for (int k = 0; k < nrOfPatterns; k++) {

				int w = l * matrixSize;

				for (int i = 0; i < nrOfStates; i++) {

					sum1 = sum2 = sum3 = 0.0;

					for (int j = 0; j < nrOfStates; j++) {
						sum1 += matrices1[w] * partials1[v + j];
						sum2 += matrices2[w] * partials2[v + j];
						sum3 += matrices3[w] * partials3[v + j];

						w++;
					}

					partials4[u] = sum1 * sum2 * sum3;
					u++;
				}
				v += nrOfStates;
			}
		}
	}

}
