package beastbooster.seqgen;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.branchratemodel.StrictClockModel;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Randomizer;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLProducer;
import beastbooster.likelihood.TreeLikelihoodF;
import beastfx.app.seqgen.MergeDataWith;
import beastfx.app.seqgen.SequenceSimulator;



/**
 * @author remco@cs.waikato.ac.nz
 */
@Description("Performs random sequence generation for a given site model. " +
        "Sequences for the leave nodes in the tree are returned as an alignment.")
public class SequenceSimulatorF extends SequenceSimulator {
	final public Input<List<Frequencies>> freqListInput = new Input<>("f", "stationary frequencies at root, one for each category", new ArrayList<>(), Validate.REQUIRED);

	/**
     * list of root frequencies, one for each category
     */
    List<Frequencies> freqList;


    public SequenceSimulatorF(Alignment data, Tree tree, SiteModel pSiteModel, BranchRateModel pBranchRateModel,
			int replications, List<Frequencies> freqList) {
		initByName("data", data, "tree", tree, "siteModel", pSiteModel, "branchRateModel", pBranchRateModel, 
				"sequencelength", replications, "f", freqList);
	}

	public SequenceSimulatorF(Alignment data, Tree tree, SiteModel sitemodel, StrictClockModel clockmodel,
			Integer replications, List<Frequencies> freqList, MergeDataWith  mergewith) {
		initByName("data", data, "tree", tree, "siteModel", sitemodel, "branchRateModel", clockmodel, 
				"sequencelength", replications, "f", freqList, "merge", mergewith);
	}

	@Override
    public void initAndValidate() {
    	super.initAndValidate();
        freqList = freqListInput.get();
    }

    /**
     * perform the actual sequence generation
     *
     * @return alignment containing randomly generated sequences for the nodes in the
     *         leaves of the tree
     * @
     */
    public Alignment simulate()  {
        Node root = m_tree.getRoot();


        double[] categoryProbs = m_siteModel.getCategoryProportions(root);
        int[] category = new int[m_sequenceLength];
        for (int i = 0; i < m_sequenceLength; i++) {
            category[i] = Randomizer.randomChoicePDF(categoryProbs);
        }

        int[] seq = new int[m_sequenceLength];
        for (int i = 0; i < m_sequenceLength; i++) {
            double[] frequencies = freqList.get(category[i]).getFreqs();
            seq[i] = Randomizer.randomChoicePDF(frequencies);
        }


        Alignment alignment = new Alignment();
        alignment.userDataTypeInput.setValue(m_data.get().getDataType(), alignment);
        alignment.setID("SequenceSimulator");

        traverse(root, seq, category, alignment);


        return alignment;
    } // simulate


    /**
     * helper method *
     */
    public static void printUsageAndExit() {
        System.out.println("Usage: java " + SequenceSimulatorF.class.getName() + " <beast file> <nr of instantiations> [<output file>]");
        System.out.println("simulates from a treelikelihood specified in the beast file.");
        System.out.println("<beast file> is name of the path beast file containing the treelikelihood.");
        System.out.println("<nr of instantiations> is the number of instantiations to be replicated.");
        System.out.println("<output file> optional name of the file to write the sequence to. By default, the sequence is written to standard output.");
        System.exit(0);
    } // printUsageAndExit


    protected static TreeLikelihoodF getTreeLikelihood(BEASTInterface beastObject)  {
        for (BEASTInterface beastObject2 : beastObject.listActiveBEASTObjects()) {
            if (beastObject2 instanceof TreeLikelihoodF) {
                return (TreeLikelihoodF) beastObject2;
            } else {
                TreeLikelihoodF likelihood = getTreeLikelihood(beastObject2);
                if (likelihood != null) {
                    return likelihood;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        try {
            // parse arguments
            if (args.length < 2) {
                printUsageAndExit();
            }
            String fileName = args[0];
            int replications = Integer.parseInt(args[1]);
            PrintStream out = System.out;
            if (args.length == 3) {
                File file = new File(args[2]);
                out = new PrintStream(file);
            }

            // grab the file
            String xml = "";
            BufferedReader fin = new BufferedReader(new FileReader(fileName));
            while (fin.ready()) {
                xml += fin.readLine();
            }
            fin.close();

            // parse the xml
            XMLParser parser = new XMLParser();
            BEASTInterface beastObject = parser.parseFragment(xml, true);

            // find relevant objects from the model
            TreeLikelihoodF treeLikelihood = getTreeLikelihood(beastObject);
            if (treeLikelihood == null) {
                throw new IllegalArgumentException("No treelikelihood found in file. Giving up now.");
            }
            List<Frequencies> freqList = treeLikelihood.freqListInput.get();
            Alignment data = ((Input<Alignment>) treeLikelihood.getInput("data")).get();
            Tree tree = ((Input<Tree>) treeLikelihood.getInput("tree")).get();
            SiteModel pSiteModel = ((Input<SiteModel>) treeLikelihood.getInput("siteModel")).get();
            BranchRateModel pBranchRateModel = ((Input<BranchRateModel>) treeLikelihood.getInput("branchRateModel")).get();


            // feed to sequence simulator and generate leaves
            SequenceSimulatorF treeSimulator = new SequenceSimulatorF(data, tree, pSiteModel, pBranchRateModel, replications, freqList);
            XMLProducer producer = new XMLProducer();
            Alignment alignment = treeSimulator.simulate();
            xml = producer.toRawXML(alignment);
            out.println("<beast version='2.6'>");
            out.println(xml);
            out.println("</beast>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // main

} // class SequenceSimulator

