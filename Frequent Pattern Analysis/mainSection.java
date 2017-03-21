/*
 * Data Mining Project 2: Frequent Pattern Analysis
 * Kiran Nanjundaswamy 
 * U00833551
 * 22-JUL- 2016
 * 
 * The file has to be run with the .txt file as command line inputs.
 * To compile: javac -cp ".:./lib/*" -d "./src" mainSection.java
 * To run: java -cp ".:./lib/*:./src"  mainSection  BANK_MARKET.txt
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RemoveByName;

import com.opencsv.CSVReader;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPClose;

class ClosedEmergingPatternJaccard{
	  public ClosedEmergingPatternJaccard(double s,ArrayList<Integer> list,String[] sj) {
		  this.objFunc = s;
		  this.closedPatternlist = list;
		  this.closedPatternJaccard = sj;
	}
	double objFunc;
	ArrayList<Integer> closedPatternlist;
	String[] closedPatternJaccard;
}

class CEP_Sort implements Comparator<ClosedEmergingPatternJaccard>{
    @Override
    public int compare(ClosedEmergingPatternJaccard d1, ClosedEmergingPatternJaccard d2) {
        if(d1.objFunc < d2.objFunc){
            return 1;
        } else {
            return -1;
        }
    }
}

class BitSetPatterns{
	  public BitSetPatterns(String cp,String bitset) {
		  this.closedPattern = cp;
		  this.BitsetValue = bitset;
	}
	String closedPattern;
	String BitsetValue;
	
}

class ClosedEmergingPatternGrowthRate{
	  public ClosedEmergingPatternGrowthRate(String s,double d,double SC1,double SC2) {
		  this.growthRate = d;
		  this.CEP = s;
		  this.suppC1 = SC1;
		  this.suppC2 = SC2;
		  
	}
	String CEP;
	double growthRate;
	double suppC1;
	double suppC2;
}


public class mainSection
{

	final static Path currentRelativePath = Paths.get("");
	static String folder = currentRelativePath.toAbsolutePath().toString();
	final static Path path = Paths.get(folder);
	private CSVReader csvReader;

	static String[][] dataArr=null;
	static String[][] finalDataArr=null;
	static int[][] disDataArr = null;
	String [] headerArr = null;
	String[] initialArr=null;
	private Scanner scan;
	static String[][] discretizeArr=null; 
    static int[] rankArr;
    public static int binNum = 0;
	static AlgoFPClose algo = new AlgoFPClose();
    public static String[] closedPattern = null;
    public static String[] closedPatternArr = null;
    public static double[] growthRate = null;
    static double growthThreshold = 2;
	public static ArrayList<Integer> list = new ArrayList<Integer>();
	public static List<ClosedEmergingPatternGrowthRate> CEP_GR_List = new ArrayList<ClosedEmergingPatternGrowthRate>();
	public static List<ClosedEmergingPatternJaccard> CEP_JC_List = new ArrayList<ClosedEmergingPatternJaccard>();
    public static HashMap<String, String> CP_BS_List = new HashMap<String, String>();
	public static ArrayList<Integer> closedClassifierlist = new ArrayList<Integer>();
    public static HashMap<String, Double> CP_ER_List = new HashMap<String, Double>();
    public static List<ClosedEmergingPatternJaccard> CEP_JC_ER_List = new ArrayList<ClosedEmergingPatternJaccard>();

	/* Method to help in conversion from .txt to .csv file
	 * We convert the text file to temporary csv file which makes it easier to load into an array
	 * We need to provide headers to the attribute, so we build an attribute header array.
	 * We write the attribute header array and data set array to .CSV file to be used in weka packages.
	 */
	public void txtToCSV(String textFile) throws IOException
	{
	    final Charset utf8 = Charset.forName("UTF-8");
	    final Path txt = path.resolve(textFile);
	    
	    scan = new Scanner(Files.newBufferedReader(txt, utf8));
	    
	    List<String> dataSet = new ArrayList<String>();
	    while (scan.hasNextLine()) {
	    	dataSet.add(scan.nextLine());
	    }

	    initialArr = new String[dataSet.size()];
	    initialArr = dataSet.toArray(initialArr);    	    
	    
	    FileWriter writer = new FileWriter(folder+"/p2data_temp.csv");
		
	    String line = "";
	    for(int i=0;i<initialArr.length;i++)
		{
			 line = "";
			 line +=initialArr[i].toString();
			 writer.write(line);
			 writer.append("\n");
		}
		
		writer.flush();
		writer.close();  
		  
	}

	public void csvAndArrayCreation() throws IOException
	{
		csvReader = new CSVReader(new FileReader(new File(folder+"/p2data_temp.csv")));
		List<String[]> list = csvReader.readAll();
		
		dataArr = new String[list.size()][];
		dataArr = list.toArray(dataArr);
		
		headerArr = new String[dataArr[0].length];
		
		for(int i=1;i<dataArr[0].length;i++)
			headerArr[i]="G"+(i);
		
		headerArr[0] = "class";
		
		FileWriter writer = new FileWriter(folder+"/p2data.csv");
		String line = "";
				for (int j=0;j<headerArr.length-1;j++)
				{
					line +=headerArr[j].toString()+",";
				}
				line += headerArr[headerArr.length-1].toString();
				writer.write(line);
				writer.append("\n");
		
		
		for(int i=0;i<dataArr.length;i++)
		{
			 line = "";
				for (int j=0;j<dataArr[0].length-1;j++)
				{
					line +=dataArr[i][j].toString()+",";
				}
				line +=dataArr[i][dataArr[0].length-1].toString();
				writer.write(line);
				writer.append("\n");
		}
		
		writer.flush();
		writer.close();  
	}	

	//This function helps us to convert the .CSV to .ARFF which can be used by Weka packages
	public void convert2ARFF() throws IOException
	{
		CSVLoader loader = new CSVLoader();
	    loader.setSource(Files.newInputStream(Paths.get(folder).resolve("p2data.csv")));
	    Instances data = loader.getDataSet();
	    	    	    
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File(folder+"/p2data.arff"));
	    saver.setDestination(new File(folder+"/p2data.arff"));
	    saver.writeBatch();
	   
	}
	
	//The below function is used in discretizating the data. Here we have enable bin numbers 
	public static Instances discretizeDataWidth(Instances data,boolean binCondition) throws Exception{
		 Discretize filter=new Discretize();
		 filter.setBins(5);
		 filter.setUseBinNumbers(true);
		 filter.setUseEqualFrequency(binCondition);
		 filter.setIgnoreClass(true);
	     filter.setInputFormat(data);
	     return Filter.useFilter(data, filter);
	}
	
	//The below function is also used in discretizating the data. Here we get the clear mapping of data to the bins  
    public static Instances discretizeDataBinWidth(Instances data,boolean binCondition) throws Exception{
		 Discretize filter=new Discretize();
		 filter.setBins(5);
		 filter.setUseEqualFrequency(binCondition);
		 filter.setIgnoreClass(true);
	     filter.setInputFormat(data);
	     return Filter.useFilter(data, filter);
	}
	
	public static void save(Instances data, String filename) throws Exception {
		     BufferedWriter  writer;
		     writer = new BufferedWriter(new FileWriter(filename));
		     writer.write(data.toString());
		     writer.newLine();
		     writer.flush();
		     writer.close();
	  }
	  	  
	
	//This function is used to help in building the discretized data set which will used for further computation
	public static void buildData(Instances data)
	  {
		  disDataArr = new int[data.numInstances()][data.numAttributes()-1]; 
			for (int i=0;i<data.numInstances()-1;i++)
			{
				binNum =0;
				for (int j=1;j<data.numAttributes();j++)
				{
					disDataArr[i][j-1] = Integer.parseInt(data.instance(i).stringValue(j).substring(2,3))+ binNum;
					binNum +=5;
				}
			}
		}
	  
	
	//This functions helps to print the Discretized Data and it's mapping to DiscretizationMap.csv file
	public static void printDiscretizeMap(Instances data) throws IOException
	  {
		  int binCount = 1;
		  BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/DiscretizationMap.csv"));
		  
			for(int i=1;i<data.numAttributes();i++)
			{
			  for(int j=0;j<data.attribute(i).numValues();j++)
				  {
				  out.write(data.attribute(i).name()+","+data.attribute(i).value(j)+","+(binCount++)+",");
				  out.newLine();
				  }
			}
		    
			out.flush();
			out.close();
	  }
	    
	
	//This functions helps to print the Discretized Data to DiscretizationData.csv file
	public static void printDiscretizeData(Instances data) throws IOException
		{
		  	String binValue = null;
		  	BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/DiscretizedData.csv"));

			for (int i=0;i<data.numInstances()-1;i++)
			{
				for (int j=0;j<data.numAttributes()-1;j++)
				{
					binValue= Integer.toString(disDataArr[i][j]);
					out.write(binValue);
					out.write(", ");
				}
				out.newLine();
			}
			out.flush();
			out.close();
		}

	
	
	//Task 2 : To find the Bitwise computations of the closed patterns and also the Jaccard similarity between Closed Patterns
	//This function outputs 2 files, BitSetOperation.txt containing the Bitset representation of the Closed Patterns
	//And BitSetJaccardSimilarity.txt which finds the Jaccard similarity between pairs of Closed Patterns 
	public static void bitsetOperations(Instances data) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/BitSetOperation.txt"));
		String[] bitset = new String[algo.closedArr.size()];
		String buffer = "";
		String[] arr = new String[100];	
		int value =0,intersection,union;
		BitSet pattern1,pattern2;
		double jaccard;
		DecimalFormat df = new DecimalFormat("#.##");      
		
		for (int j=0;j<algo.closedArr.size();j++)
		{
			bitset[j] = ""; buffer = "";
			arr = algo.closedArr.get(j).split(" ");
			for (int i=0;i<data.numInstances()-1;i++)
			{
				for(int k=0;k<arr.length;k++)
				{
				 if(Arrays.binarySearch(disDataArr[i], Integer.parseInt(arr[k]))>0)	
					 value = 1;
				 else
					 value = 0;
				}
				bitset[j] += Integer.toString(value);
			}
			buffer = "{" + algo.closedArr.get(j) + "}\t" + bitset[j];
			CP_BS_List.put(algo.closedArr.get(j),bitset[j]);
			out.write(buffer);
			out.newLine();		
		}
		out.flush();
		out.close();
		
		out = new BufferedWriter(new FileWriter(folder+"/BitSetJaccardSimilarity.txt"));
		
		for (int j=0;j<algo.closedArr.size()-1;j++)
		{
			out.write("{"+algo.closedArr.get(j)+"} & {"+algo.closedArr.get(j+1)+"} : "+df.format(computeJaccardSimilarity(bitset[j],bitset[j+1])));
			out.newLine();
		}
		out.flush();
		out.close();
	}
	
	

	//Task 2 : This function finds the Jaccard Similarity of 2 Closed Patterns
	public static Double computeJaccardSimilarity(String CEP1,String CEP2)
	{
		Double jaccard = 0.0;
		BitSet pattern1,pattern2;
		int intersection,union;
		
		pattern1= createFromString(CEP1);
		pattern2  = createFromString(CEP2);
		
		pattern1.and(pattern2);
		intersection = pattern1.cardinality();
		
		pattern1.or(pattern2);
		union = pattern1.cardinality();
		
		jaccard = (double)intersection/(double)union;
		
		return jaccard;	
	}
	
	//This function is used to generate all possible combinations of K set of values which is to be used in Task 4
	public static void subset(int[] A, int k, int start, int currLen, boolean[] used) {
		
		if (currLen == k) {
			for (int i = 0; i < A.length; i++) {
				if (used[i] == true) {
					list.add(A[i]);
				}
			}
			return;
		}
		if (start == A.length) {
			return;
		}
		used[start] = true;
		subset(A, k, start + 1, currLen + 1, used);
		used[start] = false;
		subset(A, k, start + 1, currLen, used);
	}
		
	//Task 3 : Computing the growth rate and their supports of closed patterns with respect to the data
	public static void emergingPattern(Instances mainData) throws IOException
	{
		int countC1 =0,countC2=0,value=0,countClosedC1=0,countClosedC2=0,index=0;
		double C1=0,C2=0,localgrowthRate;
		DecimalFormat df = new DecimalFormat("#.###");      
		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/GrowthRate.txt"));
		String[] arr = new String[100];	
		ArrayList<String> closedlist = new ArrayList<String>();
		ArrayList<Double> growthlist = new ArrayList<Double>();
		
		for (int i=0;i<mainData.numInstances()-1;i++)
		{
				if(mainData.instance(i).value(0)==0)
					countC1++;
				else 
					countC2++;
		}
		
		for (int j=0;j<algo.closedArr.size();j++)
		{
			countClosedC1=0;countClosedC2=0;
			arr = algo.closedArr.get(j).split(" ");
			for (int i=0;i<mainData.numInstances()-1;i++)
			{
				for(int k=0;k<arr.length;k++)
				{
				 if(Arrays.binarySearch(disDataArr[i], Integer.parseInt(arr[k]))>0)	
					 value = 1;
				 else
					 value = 0;
				}
				if(value==1)
				{	
					if(mainData.instance(i).value(0)==0)
						countClosedC1++;
					else if (mainData.instance(i).value(0)==1)
						countClosedC2++;
				}
			}
			C1 = (double)countClosedC1/(double)countC1;
			C2 = (double)countClosedC2/(double)countC2;
			localgrowthRate = Math.max(C1/C2,C2/C1);
			
			if(localgrowthRate>growthThreshold)
			{
				closedlist.add(algo.closedArr.get(j));
				growthlist.add(localgrowthRate);
				out.write("{"+algo.closedArr.get(j)+"} Growth Rate :"+df.format(localgrowthRate));
				out.newLine();
				CEP_GR_List.add(new ClosedEmergingPatternGrowthRate(algo.closedArr.get(j),Double.parseDouble(df.format(localgrowthRate)),C1,C2));
			}
		}
		out.flush();
		out.close();
		closedPattern = closedlist.toArray(new String[closedlist.size()]);
		growthRate = growthlist.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
	//This function helps in creating a BitSet to help in finding the Jaccard Similarity
	public static BitSet createFromString(String s) {
	    BitSet t = new BitSet(s.length());
	    int lastBitIndex = s.length() - 1;

	    for (int i = lastBitIndex; i >= 0; i--) {
	        if ( s.charAt(i) == '1'){
	            t.set(lastBitIndex - i);                            
	        }               
	    }

	    return t;
	}
	
	
	//Task 4 : 
	public static void closedEmergeingPatterns()
	{
		int i,j;
		String[] arrClosedEP = new String[growthRate.length];
		double growthAvg;double jaccard;double avgJaccard;double objectiveFunc;
		DecimalFormat df = new DecimalFormat("#.###");      

		for(i=0;i<growthRate.length-1;i++)
		{
			growthAvg = 0.0;jaccard=0.0;avgJaccard =0.0;
			for(j=0;j<=i+1;j++)
			{
				growthAvg += growthRate[j];
			}
			growthAvg = growthAvg/(j);
			int[] range = IntStream.rangeClosed(0, j-1).toArray();
			boolean[] checker = new boolean[range.length];
			list.clear();
			subset(range, 2, 0, 0, checker);
			for(int k=0;k<list.size();k=k+2)
			{
				jaccard =  computeJaccardSimilarity(CP_BS_List.get(closedPattern[list.get(k)]),CP_BS_List.get(closedPattern[list.get(k+1)]));
				avgJaccard += jaccard;
				arrClosedEP[i] = "{"+closedPattern[list.get(k)]+"} , {"+closedPattern[list.get(k+1)]+"} , Jaccard Similarity: "+ df.format(jaccard); 
			}
			objectiveFunc = growthAvg * (1-(avgJaccard/(list.size()/2)));
			CEP_JC_List.add(new ClosedEmergingPatternJaccard(objectiveFunc,list,arrClosedEP));
		}
		Collections.sort(CEP_JC_List,new CEP_Sort());
	}
	
	public static void storeComputedPS() throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/PSkEPs.csv"));
		ArrayList<Integer> gr_list = new ArrayList<Integer>();
		DecimalFormat df = new DecimalFormat("#.###");      

		gr_list = CEP_JC_List.get(0).closedPatternlist;
		Set<Integer> unique_gr_list = new HashSet<Integer>(gr_list);
		
		out.write("Objective Function Result: "+ CEP_JC_List.get(0).objFunc);
		out.newLine();
		
		for(int i : unique_gr_list)
		{
			out.write("{" + CEP_GR_List.get(i).CEP  +"} ,GrowthRate: "+CEP_GR_List.get(i).growthRate+
					",SuppC1: "+df.format(CEP_GR_List.get(i).suppC1) + ",SuppC2: "+df.format(CEP_GR_List.get(i).suppC2));
			out.newLine();
		}
		out.flush();
		out.close();
		
		out = new BufferedWriter(new FileWriter(folder+"/PSkEPJaccard.csv"));
		
		String[] arrCEP = CEP_JC_List.get(0).closedPatternJaccard;
		
		for(String s: arrCEP)
		{
			if(s!=null) 
				{
				out.write(s.toString());
				out.newLine();
				}
		}
		out.flush();
		out.close();
	} 
	
	
	//Task 5 :
	public static void closedPatternClassifier(Instances data) throws Exception
	{
		String[] arr = new String[100];	
		int value=0;
		ArrayList[] closedtable = new ArrayList[algo.closedArr.size()];
		String attributeName = "";
		for (int j=0;j<algo.closedArr.size();j++)
			{	
			 FastVector fvNominalVal = new FastVector(20);
			 FastVector fvWekaAttributes = new FastVector(21);
			 
			 Classifier cModel = (Classifier)new NaiveBayes();
			 
			 FastVector fvClassVal = new FastVector(2);
			 fvClassVal.addElement("1");
			 fvClassVal.addElement("0");
			 
			 fvWekaAttributes.addElement(new Attribute("Attribute0",fvClassVal));
			 for(int i=1;i<data.numAttributes();i++)
				 {		 	
				 attributeName = "Attribute"+i;
				 fvWekaAttributes.addElement(new Attribute(attributeName));
				 }
	
			 Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, data.numInstances()+1);
			 isTrainingSet.setClassIndex(0);
	
				arr = algo.closedArr.get(j).split(" ");
				closedtable[j] = new ArrayList();
				for (int i=0;i<data.numInstances()-1;i++)
				{
					for(int k=0;k<arr.length;k++)
					{
					 if(Arrays.binarySearch(disDataArr[i], Integer.parseInt(arr[k]))>0)	
						 value = 1;
					 else
						 value = 0;
					}
					if(value==1)
					{	
						Instance iExample = new DenseInstance(data.numAttributes()+1);		 
						iExample.setValue((Attribute)fvWekaAttributes.elementAt(0),dataArr[i][0]);
						for(int m=1;m<data.numAttributes();m++)
							{
							iExample.setValue((Attribute)fvWekaAttributes.elementAt(m),disDataArr[i][m-1]);
							}
						isTrainingSet.add(iExample);
					}
				}
			
		 		isTrainingSet.remove(isTrainingSet.numInstances()-1);
		 		
				NaiveBayes model = new NaiveBayes();
				model.buildClassifier(isTrainingSet);
				
				Evaluation eTest = new Evaluation(isTrainingSet);
				eTest.evaluateModel(model, isTrainingSet);
				CP_ER_List.put(algo.closedArr.get(j),eTest.errorRate());
			}
	}
	
	public static void closedPatternsErrorRateJaccard()
	{
		int i,j,counter=0;
		String[] arrClosedEP = new String[CP_ER_List.size()]; 
		double errorAvg;double jaccard;double avgJaccard;double objectiveFunc;
		DecimalFormat df = new DecimalFormat("#.###");      

		closedPatternArr = new String[algo.closedArr.size()];

		for(String s: algo.closedArr)
		{
			closedPatternArr[counter++] = s;
		}
		
		for(i=0;i<closedPatternArr.length-2;i++)
		{
			errorAvg = 0.0;jaccard=0.0;avgJaccard =0.0;
			for(j=0;j<=i+1;j++)
			{
				errorAvg += CP_ER_List.get(closedPatternArr[j]);
			}
			errorAvg = errorAvg/(j);
			int[] range = IntStream.rangeClosed(0, j-1).toArray();
			boolean[] checker = new boolean[range.length];
			list.clear();
			subset(range, 2, 0, 0, checker);
			for(int k=0;k<list.size();k=k+2)
			{
				jaccard =  computeJaccardSimilarity(CP_BS_List.get(closedPatternArr[list.get(k)]),CP_BS_List.get(closedPatternArr[list.get(k+1)]));
				avgJaccard += jaccard;
				arrClosedEP[i] = "{"+closedPatternArr[list.get(k)]+"} , {"+closedPatternArr[list.get(k+1)]+"} , Jaccard Similarity: "+ df.format(jaccard); 
			}
			objectiveFunc = errorAvg * (1-(avgJaccard/(list.size()/2)));
			CEP_JC_ER_List.add(new ClosedEmergingPatternJaccard(objectiveFunc,list,arrClosedEP));
		}
		Collections.sort(CEP_JC_ER_List,new CEP_Sort());
	}
	
	public static void storeComputedErrorPS() throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/PSkCPsWithError.csv"));
		ArrayList<Integer> gr_list = new ArrayList<Integer>();
		DecimalFormat df = new DecimalFormat("#.###");      

		gr_list = CEP_JC_ER_List.get(0).closedPatternlist;
		Set<Integer> unique_gr_list = new HashSet<Integer>(gr_list);
		
		out.write("Objective Function Result: "+ CEP_JC_ER_List.get(0).objFunc);
		out.newLine();
		
		for(int i : unique_gr_list)
		{
			out.write("{" + closedPatternArr[i]  +"} ,ErrorRate: "+CP_ER_List.get(closedPatternArr[i]));
			out.newLine();
		}
		out.flush();
		out.close();
		
		out = new BufferedWriter(new FileWriter(folder+"/PSkCPwithErrorJaccard.csv"));
		
		String[] arrCEP = CEP_JC_ER_List.get(0).closedPatternJaccard;
		
		for(String s: arrCEP)
		{
			if(s!=null) 
				{
				out.write(s.toString());
				out.newLine();
				}
		}
		out.flush();
		out.close();
	} 
	
	//Task 6 :
	public static void otherDetection(Instances data,Instances Disdata) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/LinerRegression.txt"));
		LinearRegression model = new LinearRegression();
		model.setOutputAdditionalStats(true);
		model.buildClassifier(data);
		out.write(model.toString());
		out.flush();
		out.close();
		
		InterquartileRange filter = new InterquartileRange();
	    filter.setInputFormat(data);
	    Instances filteredData = Filter.useFilter(data, filter);
	    save(filteredData, folder+"/OutlierDetection.txt");

	    CSVSaver csv = new CSVSaver();
	    csv.setFile(new File(folder+"/OutlierDetection.csv"));
	    csv.setInstances(filteredData);
	    csv.writeBatch();
	    
	    out = new BufferedWriter(new FileWriter(folder+"/Clustering.txt"));
	    Instances dataClusterer = null;
	    Remove rmfilter = new Remove();
	    rmfilter.setAttributeIndices("" + (data.classIndex() + 1));
	    rmfilter.setInputFormat(data);
	    dataClusterer = Filter.useFilter(data, rmfilter);
    
	    SimpleKMeans kmeans = new SimpleKMeans();   
		kmeans.setSeed(10);
		kmeans.setNumClusters(5);
		kmeans.buildClusterer(dataClusterer);
	    
		out.write(kmeans.toString());
		out.flush();
		out.close();
	}
	  
	
	public static void main(String args[]) throws Exception
	{
		
		if(args.length < 1)
	    {
	        System.out.println("Proper Usage is: mainSection <InputFile>");
	        System.out.println("Please specify the Input File.");
	        System.exit(0);
	    }
		
		DataSource source =  null;
		Instances inst = null;
		Instances disInst= null;
		Instances disInstBin= null;
		int selection = 0;
		boolean binCondition= false;
		String input = folder+"/DiscretizedData.csv";  // the database
		String output = folder+"/ClosedAndMG.csv";  // the path for saving the frequent itemsets found
		double minsup = 0.5;
		
		mainSection ms = new mainSection();

		System.out.println("Running on file "+args[0]);
		
		/* Converting the inputed .txt file to .csv format as weka does not accept .txt files */
		ms.txtToCSV(args[0]);
		ms.csvAndArrayCreation();

		/* Converting the .csv file to .arff so it can be used with weka packages */
		ms.convert2ARFF();
		
		source =  new DataSource(folder+"/p2data.arff");
		inst = source.getDataSet();  
		inst.setClassIndex(inst.numAttributes()-1);
		
		if(selection==1)
			binCondition = true;
		else
			binCondition = false; 
		
		disInst = discretizeDataWidth(inst,binCondition);
		save(disInst, folder+"/p2_discretize.txt");
				
		disInstBin = discretizeDataBinWidth(inst,binCondition);
		save(disInstBin, folder+"/p2_discretize_bins.txt");
		
		System.out.println("Building the Discritized Data Set");
		buildData(disInst);
		printDiscretizeMap(disInstBin);
		printDiscretizeData(disInst);
	
		System.out.println("Task 1: Computing the Frequent Patterns along with Closed Patterns and Minimal Generators");
		algo.runAlgorithm(input, output, minsup);
		
		System.out.println("Task 2: Computing the BitSet representations for the Closed patterns");
		bitsetOperations(disInst);
		
		System.out.println("Task 3: Computing the Emerging Patterns and their Growth Rates");
		emergingPattern(inst);
		
		System.out.println("Task 4: Computing the Objective Function for diversified set PS of K Closed Emerging Patterns");
		closedEmergeingPatterns();
		
		System.out.println("Task 4: Printing the results");
		storeComputedPS();
		
		System.out.println("Task 5: Computing the Error Rate of Closed patterns using Naive Bayes Classifier");
		closedPatternClassifier(disInst);
		
		System.out.println("Task 5: Computing the Objective Function for diversified set PS of K Closed patterns with Error Rate");
		closedPatternsErrorRateJaccard();
		
		System.out.println("Task 5: Printing the results");
		storeComputedErrorPS();
		
		System.out.println("Task 6: Performing Liner Regression, Clustering using Simple K Means & Outlier Detection");
		otherDetection(inst,disInst);
		
		System.out.println("\nDone..!! Please Check the output files");
	}
}