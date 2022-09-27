package beastbooster.likelihood;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;

@Description("Real parameter that takes values averaging over a set of real parameters")
public class AveragedRealParameter extends RealParameter {
	public Input<List<RealParameter>> paramInput = new Input<>("p", "list of paramters that are averaged over", new ArrayList<>());

	List<RealParameter> param;
	
	@Override
	public void initAndValidate() {
		param = paramInput.get();
		
		// make sure all parameters have the same dimension
		for (int i = 1; i < param.size(); i++) {
			if (param.get(i).getDimension() != param.get(0).getDimension()) {
				throw new IllegalArgumentException("parameters differ in dimension");
			}
		}
		
		super.initAndValidate();
	}
	
	
	@Override
	public Double getValue() {		
		return getValue(0);
	}
	
	@Override
	public Double getValue(int i) {
		Double v = 0.0;
		for (RealParameter r : param) {
			v += r.getValue(i);
		}
		values[i] = v;
		return v / param.size();
	}

	@Override
	public int getDimension() {
		return param.get(0).getDimension();
	}
	
	@Override
	public boolean somethingIsDirty() {
		return isDirtyCalculation();
	}
	
    @Override
    protected boolean requiresRecalculation() {
//		for (RealParameter r : param) {
//			if (r.somethingIsDirty()) {
//				return true;
//			}
//		}
//		return false;
		return true;
    }

}
