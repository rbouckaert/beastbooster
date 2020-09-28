package beastbooster.operators;

import beast.core.Description;
import beast.core.Operator;
import beast.core.OperatorSchedule;

@Description("Operator schedule that recognises MultiStepOperators and selects them for the desired "
		+ "number of steps consecutively")
public class MultiStepOperatorSchedule extends OperatorSchedule {

	private int stepCount, currentStep;
	private MultiStepOperator multiStepOperator = null;
	
	@Override
	public Operator selectOperator() {
		if (multiStepOperator != null) {
			currentStep++;
			multiStepOperator.setStepNr(currentStep);
			if (currentStep < stepCount) {
				return (Operator) multiStepOperator;
			} else {
				multiStepOperator = null;
			}
		}
		
		Operator operator = super.selectOperator();
		if (operator instanceof MultiStepOperator) {
			multiStepOperator = (MultiStepOperator) operator;
			currentStep = 0;
			multiStepOperator.setStepNr(currentStep);
			stepCount = multiStepOperator.stepCount();
		}
		return operator;
    }

}
