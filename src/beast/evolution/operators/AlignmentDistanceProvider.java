package beast.evolution.operators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.distance.Distance;
import beast.evolution.alignment.distance.JukesCantorDistance;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import mdsj.ClassicalScaling;

@Description("Provide distance between sequnces in allignment after multi-dimensional scaling -- to be used by AttachOperator")
public class AlignmentDistanceProvider extends BEASTObject implements DistanceProvider {
	enum Method {
		DISTANCE("distance"),
		SQRT("sqrt"),
		ARC("arc");

		Method(final String name) {
			this.ename = name;
		}

		public String toString() {
			return ename;
		}

		private final String ename;
	}
    public Input<TreeInterface> treeInput = new Input<>("tree", "phylogenetic beast.tree with sequence data in the leafs", Validate.REQUIRED);
    public Input<Alignment> dataInput = new Input<>("data", "sequence data for the beast.tree", Validate.REQUIRED);
    public Input<Distance> distanceInput = new Input<>("distance", "method for calculating distance between two sequences (default Jukes Cantor)", new JukesCantorDistance());

    
	public Input<Method> distMethod = new Input<>("method", "for calculating distance between clade positions (for operator weights). sqrt takes " +
	         "square root of distance (default distance)",  Method.DISTANCE, Method.values());

    public Input<File> svgOuputInput = new Input<>("svg", "if specifies, mds map is written to file in svg with this file name");
    public Input<Integer> dimensionInput = new Input<>("dimension", "number of dimensions to use for mds", 3);
	
	int DIM = 3;
	
	private TreeInterface tree;
	private double [][] position;
    private Method distanceMethod;

	@Override
	public void initAndValidate() {
		DIM = dimensionInput.get();
		distanceMethod = distMethod.get();
		tree = treeInput.get();
		
        Distance distance = distanceInput.get();
        if (distance == null) {
            distance = new JukesCantorDistance();
        }
        if (distance instanceof Distance.Base){
        	if (dataInput.get() == null) {
        		// Distance requires an alignment?
        	}
        	((Distance.Base) distance).setPatterns(dataInput.get());
        }

        Log.warning.print("Calculating distances");
        int n = tree.getLeafNodeCount();
        double [][] input = new double[n][n];
        int ticks = Math.max(1, n/25);
        double min = 1000, max = 0;
        for (int i = 0; i < n; i++) {
        	input[i][i] = 0;
            for (int j = i + 1; j < n; j++) {
            	input[i][j] = distance.pairwiseDistance(i, j);// + 1/1000;
            	input[j][i] = input[i][j];
            	min = Math.min(min,  input[i][j]);
            	max = Math.max(max,  input[i][j]);
            }
            if (i%ticks == 0) {Log.warning.print('.');}
        }
        Log.warning.print("\nMulti dimensional scaling...");

        double [][] output = new double[DIM][input.length];
        double[] result = ClassicalScaling.fullmds(input, output); // apply MDS
        Log.warning.println("Done");

        position = new double[tree.getNodeCount()][DIM];
        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
        	for (int j = 0; j < DIM; j++) {
        		position[i][j] = output[j][i];
        	}
        }

        if (svgOuputInput.get() != null) {
			try {
				double minx = 0, maxx = 0;
				double miny = 0, maxy = 0;
		        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
		        	minx = Math.min(minx, position[i][0]);
		        	maxx = Math.max(maxx, position[i][0]);
		        	miny = Math.min(miny, position[i][1]);
		        	maxy = Math.max(maxy, position[i][1]);
		        }
		        
	        	PrintStream svg;
				svg = new PrintStream(svgOuputInput.get());
				svg.println("<svg class=\"chart\" width=\"1240\" height=\"1040\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">");
				svg.println("<g transform=\"translate(100,10)\">");
				
				// axes
				svg.println("<rect x='1' y ='1' width=\"1018\" height=\"1018\" style=\"fill:none;stroke-width:1;stroke:rgb(0,0,0)\"/>");
				
				svg.println("<g transform=\"translate(10,10)\">");
				// bars
				double dx = 1000.0 ;
				double dy = 1000.0 ;
				// permissable area

		        for (int i = 0; i < tree.getLeafNodeCount(); i++) {
					int x = (int)((position[i][0] - minx) * 1000 / (maxx - minx));
					int y = (int)((position[i][1] - miny) * 1000 / (maxy - miny));
					// System.out.println("x=" + x + " y=" + y + " " + tree.getTaxonset().getTaxonId(i));
					svg.println("<circle cx=\"" + x + "\" cy=\"" + y + "\" r=\"4\" style=\"stroke:blue;opacity:0.5;\" />");
					svg.println("<text text-anchor=\"middle\" style=\"stroke:black;opacity:0.5;\" x='"+(x+5) + "' y='"+(y-5)+"'>" + tree.getTaxonset().getTaxonId(i) + "</text>");
		        }
				svg.println("</g>");
				svg.println("</g>");
				
				svg.println("</svg>");
	        	svg.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
        }
        
        
        
	}
	
	class LocationData implements DistanceProvider.Data {
		final static int MAX_ITER = 0;
		final static double MIN_EPSILON = 0.001;

		double[] position;
		int weight;

		public LocationData(double[] pos) {
			position = pos;
			weight = 1;
		}
		public LocationData() {
			position = new double[DIM];
	        weight = 0;
		}
	}
	
	@Override
	public Map<String, DistanceProvider.Data> init(Set<String> taxa) {
		final HashMap<String, DistanceProvider.Data> m = new HashMap<>();
		int count = 0;
		for (int i = 0; i < tree.getLeafNodeCount(); i++) {
			Node n = tree.getNode(i);
			final String taxon = n.getID();
			if( taxa.contains(taxon) ) {
				final double[] xyz = position[i];
				m.put(taxon, new LocationData(xyz));
				count += 1;
			}
		}
		if( count != taxa.size() ) {
			return null;
		}
		return m;
	}

    @Override
    public Data empty() {
        return new LocationData();
    }

    @Override
    public void clear(Data d) {
        ((LocationData)d).weight = 0;
    }

    @Override
    public void update(Data info, Data with) {
        LocationData d1 = (LocationData) info;
        LocationData d2 = (LocationData) with;
        assert d1.weight >= 0 &&  d2.weight > 0;

        if( d1.weight == 0 ) {
            System.arraycopy(d2.position, 0, d1.position, 0, DIM);
            d1.weight = d2.weight;
        } else {
            final int w = d1.weight + d2.weight;
            for (int i = 0; i < DIM; ++i) {
                d1.position[i] = (d1.position[i] * d1.weight + d2.position[i] * d2.weight) / w;
            }
            d1.weight = w;
        }
        assert d1.weight > 0;
    }

    @Override
    public double dist(DistanceProvider.Data info1, DistanceProvider.Data info2) {
        LocationData d1 = (LocationData) info1;
        LocationData d2 = (LocationData) info2;
        double s = 0;
        for(int k = 0; k < DIM; ++k) {
            double x = (d1.position[k] - d2.position[k]);
            s += x*x;
        }
        s = (s == 0) ? 1e-8 : s;
        switch (distanceMethod) {
            case DISTANCE: break;
            case SQRT: s = Math.sqrt(s); break;
            case ARC: s =  FastMath.asin(FastMath.sqrt(s) / 2); break;
        }
        return s;
    }

}
