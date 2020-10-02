package beastbooster.likelihood;


public class DuckLikelihoodCore4 extends DuckLikelihoodCore {

	public DuckLikelihoodCore4(int stateCount) {
		super(4);
		if (stateCount != 4) {
			throw new RuntimeException("Programmer error: DuckLikelihoodCore4 only works with 4 states");
		}
	}

	

	@Override
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

	                if (state1 < 4) {
	                	if (state2 < 4) {
		                	if (state3 < 4) {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
//			                    }
		                	} else {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices2[w + state2];
			                        v++;
			                        w += 4;
//			                    }
		                	}
	                	} else {
	                		if (state3 < 4) {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1] * matrices3[w + state3];
			                        v++;
			                        w += 4;
//			                    }
	                		} else {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices1[w + state1];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices1[w + state1];
			                        v++;
			                        w += 4;
//			                    }
	                		}
	                	}
	                } else {
	                	if (state2 < 4) {
		                	if (state3 < 4) {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2] * matrices3[w + state3];
			                        v++;
			                        w += 4;
//			                    }
		                	} else {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices2[w + state2];
			                        v++;
			                        w += 4;
//			                    }		                		
		                	}
	                	} else {
	                		if (state3 < 4) {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices3[w + state3];
			                        v++;
			                        w += 4;
			                        partials4[v] = matrices3[w + state3];
			                        v++;
			                        w += 4;
//			                    }
	                		} else {
//			                    for (int i = 0; i < 4; i++) {
			                        partials4[v] = 1.0;
			                        v++;
			                        partials4[v] = 1.0;
			                        v++;
			                        partials4[v] = 1.0;
			                        v++;
			                        partials4[v] = 1.0;
			                        v++;
//			                    }
	                		}
	                	}
	                }
	            }
	        }
	}

	@Override
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


                if (state1 < 4 && state2 < 4) {

                    for (int i = 0; i < 4; i++) {

                    	partials4[v] = matrices1[w + state1] * matrices2[w + state2];
                    	sum = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum += matrices3[w] * partials3[u];
                            w++;
                            sum += matrices3[w] * partials3[u + 1];
                            w++;
                            sum += matrices3[w] * partials3[u + 2];
                            w++;
                            sum += matrices3[w] * partials3[u + 3];
                            w++;
//                        }
                        partials4[v] *= sum;

                        v++;                        
                    }
                    u += 4;

                } else if (state1 < 4) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < 4; i++) {

                        partials4[v] = matrices1[w + state1];
                    	sum = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum += matrices3[w] * partials3[u];
                            w++;
                            sum += matrices3[w] * partials3[u + 1];
                            w++;
                            sum += matrices3[w] * partials3[u + 2];
                            w++;
                            sum += matrices3[w] * partials3[u + 3];
                            w++;
//                        }
                        partials4[v] *= sum;

                        v++;
                        // w += 4;
                    }
                    u += 4;
                } else if (state2 < 4) {
                    // child 2 has a gap or unknown state so treat it as unknown

                    for (int i = 0; i < 4; i++) {

                        partials4[v] = matrices2[w + state2];
                    	sum = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum += matrices3[w] * partials3[u];
                            w++;
                            sum += matrices3[w] * partials3[u + 1];
                            w++;
                            sum += matrices3[w] * partials3[u + 2];
                            w++;
                            sum += matrices3[w] * partials3[u + 3];
                            w++;
//                        }
                        partials4[v] *= sum;

                        v++;
                        // w += 4;
                    }
                    u += 4;
                } else {
                    // both children have a gap or unknown state so set partials to 1
                    for (int i = 0; i < 4; i++) {
                    	sum = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum += matrices3[w] * partials3[u];
                            w++;
                            sum += matrices3[w] * partials3[u + 1];
                            w++;
                            sum += matrices3[w] * partials3[u + 2];
                            w++;
                            sum += matrices3[w] * partials3[u + 3];
                            w++;
//                        }
                        partials4[v] = sum;
                        v++;
                    }
                    u += 4;
                }
            }
        }
	  
	}

	
    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
	@Override
	protected void calculateStatesPartialsPartialsPruning(int[] stateIndex1, double[] matrices1,
                                                  double[] partials2, double[] matrices2,
                                      			  double[] partials3, double[] matrices3, 
                                    			  double[] partials4) {

        double sum1, sum2, tmp;

        int u = 0;
        int v = 0;

        for (int l = 0; l < nrOfMatrices; l++) {
            for (int k = 0; k < nrOfPatterns; k++) {

                int state1 = stateIndex1[k];

                int w = l * matrixSize;

                if (state1 < 4) {


                    for (int i = 0; i < 4; i++) {

                        tmp = matrices1[w + state1];

                        sum1 = sum2 = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum1 += matrices2[w] * partials2[v];
                            sum2 += matrices3[w] * partials3[v];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 1];
                            sum2 += matrices3[w] * partials3[v + 1];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 2];
                            sum2 += matrices3[w] * partials3[v + 2];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 3];
                            sum2 += matrices3[w] * partials3[v + 3];
                            w++;
//                        }

                        partials4[u] = tmp * sum1 * sum2;
                        u++;
                    }

                    v += 4;
                } else {
                    // Child 1 has a gap or unknown state so don't use it

                    for (int i = 0; i < 4; i++) {

                        sum1 = sum2 = 0.0;
//                        for (int j = 0; j < 4; j++) {
                            sum1 += matrices2[w] * partials2[v];
                            sum2 += matrices3[w] * partials3[v];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 1];
                            sum2 += matrices3[w] * partials3[v + 1];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 2];
                            sum2 += matrices3[w] * partials3[v + 2];
                            w++;
                            sum1 += matrices2[w] * partials2[v + 3];
                            sum2 += matrices3[w] * partials3[v + 3];
                            w++;
//                        }

                        partials4[u] = sum1 * sum2;
                        u++;
                    }

                    v += 4;
                }
            }
        }
    }

	
	
	
	
	@Override
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

				for (int i = 0; i < 4; i++) {

					sum1 = sum2 = sum3 = 0.0;

//					for (int j = 0; j < 4; j++) {
						sum1 += matrices1[w] * partials1[v];
						sum2 += matrices2[w] * partials2[v];
						sum3 += matrices3[w] * partials3[v];
						w++;
						sum1 += matrices1[w] * partials1[v + 1];
						sum2 += matrices2[w] * partials2[v + 1];
						sum3 += matrices3[w] * partials3[v + 1];
						w++;
						sum1 += matrices1[w] * partials1[v + 2];
						sum2 += matrices2[w] * partials2[v + 2];
						sum3 += matrices3[w] * partials3[v + 2];
						w++;
						sum1 += matrices1[w] * partials1[v + 3];
						sum2 += matrices2[w] * partials2[v + 3];
						sum3 += matrices3[w] * partials3[v + 3];
						w++;
//					}

					partials4[u] = sum1 * sum2 * sum3;
					u++;
				}
				v += 4;
			}
		}
	}

}
