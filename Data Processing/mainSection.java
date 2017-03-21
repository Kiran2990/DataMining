/*
 * Data Mining Project 1: Data Preprocessing
 * Kiran Nanjundaswamy 
 * U00833551
 * 7-JUL- 2016
 * This code has been designed to input a text file of Gene data and preprocess the contents of the file.
 * Task 1: Designed to select K features using Remove Attribute Filter using weka packages
 * Task 2: Designed to compute the information gain of each feature and order them in decreasing order of info gain
 * 		   We also compute a discretized file of genes and a file for computation in task 3.
 * Task 3: Designed to compute the correlation coefficient between the Genes which are produced in task1 and task2 
 * 
 * The file has to be run with K value and the .txt file as command line inputs.
 * To compile: javac -cp ".:./lib/*" -d "./src" mainSection.java
 * To run: java -cp ".:./lib/*:./src"  mainSection 10 p1data.txt
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import weka.core.Instances;  
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;  
import weka.filters.unsupervised.attribute.Remove;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.opencsv.CSVReader;

/* List Classes to help maintain a part of the data set values
 * We are using List Classes with Comparator as we can maintain an array of Double values and String values 
 * And it is easy to sort in asscending or descending order */
class DataValue{
	  public DataValue(double d, String s) {
		  this.value = d;
		  this.classValue = s;
	}
	double value;
	  String classValue;
	
}

class Feature implements Comparator<DataValue>{
    @Override
    public int compare(DataValue d1, DataValue d2) {
        if(d1.value > d2.value){
            return 1;
        } else {
            return -1;
        }
    }
}

/* List Classes to help maintain the entire data set values */
class AllDataValues{
	  public AllDataValues(double d, double sV,double sV2,int an,String p) {
		  this.infoGainValue = d;
		  this.splitValue1 = sV;
		  this.splitValue2 = sV2;
		  this.attributeNum = an;
		  this.printLine = p;
	}
	  
	double infoGainValue =0;
	double splitValue1 = 0;	
	double splitValue2 = 0;
	int attributeNum = 0;
	String printLine = "";
}

class FeatureSort implements Comparator<AllDataValues>{
  @Override
  public int compare(AllDataValues d1, AllDataValues d2) {
      if(d1.infoGainValue < d2.infoGainValue){
          return 1;
      } else {
          return -1;
      }
  }
}


/* List Class to help maintain correlation data set values */
class CorrelationValue{
	  public CorrelationValue(double d, String s) {
		  this.value = d;
		  this.printString = s;
	}
	double value;
	String printString;
	
}

class correlationSort implements Comparator<CorrelationValue>{
  @Override
  public int compare(CorrelationValue d1, CorrelationValue d2) {
      if(d1.value > d2.value){
          return 1;
      } else {
          return -1;
      }
  }
}

public class mainSection
{
	static DataSource source =  null;
	static Instances inst = null;
	static Instances disInst= null;
	static Instances disInstBin= null;
	static Instances infoGainInst = null;
	static Instances removeInst = null;
	static Instances infoGainInstCorr = null;
	static double[][] instRankAttr = null;
	int countInstance = 0;
	int countAttribute = 0;
	public int K;
	
	mainSection(int kValue)
	{
		this.K = kValue;
	}
	
	final static Path currentRelativePath = Paths.get("");
	static String folder = currentRelativePath.toAbsolutePath().toString();
	final static Path path = Paths.get(folder);
    
    final static Path temp_csv = path.resolve("p1data_temp.csv");
    final static Path csv = path.resolve("p1data.csv");
    final static Path arf = path.resolve("p1data.arff");
	private static BufferedWriter out;
	private CSVReader csvReader;
	
	static String[][] dataArr=null;
	static String[][] finalDataArr=null;
	String [] headerArr = null;
	String[] initialArr=null;
	private Scanner scan;
	static String[][] discretizeArr=null; 
    static int[] rankArr;
	
	static List<AllDataValues> AllList = new ArrayList<AllDataValues>();
	static List<AllDataValues> FinalList = new ArrayList<AllDataValues>();
	static String topK_features1[][] = null;
 
	/* Method to help in conversion from .txt to .csv file
	 * We convert the text file to temporary csv file which makes it easier to load into an arry
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
	    
	    FileWriter writer = new FileWriter(folder+"/p1data_temp.csv");
		
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
		csvReader = new CSVReader(new FileReader(new File(folder+"/p1data_temp.csv")));
		List<String[]> list = csvReader.readAll();
		
		dataArr = new String[list.size()][];
		dataArr = list.toArray(dataArr);
		
		headerArr = new String[dataArr[0].length];
		
		for(int i=0;i<dataArr[0].length-1;i++)
			headerArr[i]="G"+(i+1);
		
		headerArr[headerArr.length-1] = "class";
		
		FileWriter writer = new FileWriter(folder+"/p1data.csv");
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
	
	
	public void convert2ARFF() throws IOException
	{
		CSVLoader loader = new CSVLoader();
	    loader.setSource(Files.newInputStream(Paths.get(folder).resolve("p1data.csv")));
	    Instances data = loader.getDataSet();
	    	    	    
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File(folder+"/p1data.arff"));
	    saver.setDestination(new File(folder+"/p1data.arff"));
	    saver.writeBatch();
	   
	}
	
	
	/*
	 * Method to help us use the Remove Attribute Filter of Weka.
	 * Remove helps us to choose the number of features we would like to remove. Henece we will be only left with the required data.
	 * This Filter does not create an form of reordering or processing to the data so we can have the RAW input data to work iwht.
	 */
	public static Instances removeInstData(Instances data,int kValue) throws Exception{
		String value = "";
		Remove removeAttr = new Remove();
		value = (kValue+1)+"-"+(data.numAttributes()-1);
		removeAttr.setAttributeIndices(value);
		removeAttr.setInputFormat(data);
		return Filter.useFilter(data, removeAttr);	
	}
	
	/*
	 * To find the correlation we are using a Package called PearsonsCorrelation which will help in computing the correlation 
	 * by accepting an array of x and y values and return the correlation coefficient
	 */
	public void findCorrelation() throws IOException
	{
		PearsonsCorrelation corr = new PearsonsCorrelation();
		BufferedWriter writer = new BufferedWriter(new FileWriter(folder+"/correlationgenes.txt"));
		String lineValue = null;
		List<CorrelationValue> list = new ArrayList<CorrelationValue>();

		double[] source1 = new double[dataArr.length];
		double[] source2 = new double[dataArr.length];
		double  Result = 0.0;
		for(int i=0;i<K;i++)
		{
			for(int x=0;x<topK_features1.length;x++)
				source1[x]= Double.parseDouble(topK_features1[x][i]);
			
			for(int j=0;j<K;j++)
				{
					for(int y=0;y<finalDataArr.length;y++)
						source2[y]= Double.parseDouble(finalDataArr[y][j]);
					Result = corr.correlation(source1, source2);
					lineValue="Correlation Coefficient of G"+(i+1)+" & G"+rankArr[j]+" : "+Result;
					list.add(new CorrelationValue(Result,lineValue));
				}
		}
		
		Collections.sort(list,new correlationSort());

	     for(CorrelationValue d:list){
	    	 	writer.write(d.printString);
				writer.newLine();
		 }
	     writer.flush();
	     writer.close();
		
	}
	 
	/*
	 * Method to just extract only the top K features of task 1
	 */
	  public void buildTopK(Instances data) throws IOException
	  {
		   topK_features1= new String[data.numInstances()][data.numAttributes()]; 
		  
		  		int countInstance = data.numInstances();
		  		int countAttribute = data.numAttributes();
		  		
		  		
		  		for(int i=0;i<countInstance;i++)
		  			{
		  			for (int j= 0;j<countAttribute-1;j++)
		  				{
		  				topK_features1 [i][j] = String.valueOf(data.instance(i).value(j));
		  				}
		  			topK_features1 [i][countAttribute-1]= data.instance(i).stringValue(countAttribute-1);
		  			}

		  		BufferedWriter out = new BufferedWriter(new FileWriter(folder+"/topkfeatures1.txt"));
		  		for(int i=0;i<countInstance;i++)
		  		{
		  			for (int j= 0;j<countAttribute-1;j++)
		  				{
		  				out.write(topK_features1 [i][j]);
		  				out.write(",");
		  				}
		  			out.write(topK_features1 [i][countAttribute-1]);
		  			out.newLine();
		  		}
		  		out.flush();  
		  		out.close(); 	  
	  }
	  
	  /*
	   * Computing the information gain. We get a median value by computing the HIGH-LOW/2 for each gene
	   * Then we consider a split in the 1st half and the 2nd half.
	   * We calculate the information gain using split 1 and next with split 2.
	   * Information gain the weighted average of the 3 bins.
	   * Which ever split produces a better information gain will be considered as the information gain for the Gene 
	   */
	  public static double findIG(List<DataValue> list,double Interval1,double Interval2,double m)
	  {
		  double p_ST= 0,n_ST= 0,p_SB= 0,n_SB= 0,p_SM= 0,n_SM = 0;

		  List<DataValue> splitTop = new ArrayList<DataValue>();
		  List<DataValue> splitMid = new ArrayList<DataValue>();
		  List<DataValue> splitBottom = new ArrayList<DataValue>();	
		  
		  double pValue=0,nValue=0;
		 
		  /*
		   * Counting the number of Positive & Negative values in each split
		   */
			for(DataValue d:list){
				if(d.value<Interval2 && d.value<Interval1)
					{
					splitTop.add(d);
						if(d.classValue.equalsIgnoreCase("positive"))
							p_ST ++;
						else if(d.classValue.equalsIgnoreCase("negative"))
							n_ST ++;
					}
				else if(d.value>Interval1 && d.value>Interval2)
					{
					splitBottom.add(d);
						if(d.classValue.equalsIgnoreCase("positive"))
							p_SB ++;
						else if(d.classValue.equalsIgnoreCase("negative"))
							n_SB ++;
					}
				else if(d.value>Interval2 && d.value<Interval1)
					{
					splitMid.add(d);
						if(d.classValue.equalsIgnoreCase("positive"))
							p_SM ++;
						else if(d.classValue.equalsIgnoreCase("negative"))
							n_SM ++;
					}
			}
	         
			// Calculating the entorpy of each split
			double sum = p_ST+n_ST;
			pValue = p_ST/sum;
			nValue = n_ST/sum;
	
			double entropyS1 = -(pValue*Math.log(pValue)) - (nValue*Math.log(nValue));
			
			pValue = p_SM/(p_SM+n_SM);
			nValue = n_SM/(p_SM+n_SM);
	
			double entropyS2 = -(pValue*Math.log(pValue)) - (nValue*Math.log(nValue));
			
			pValue = p_SB/(p_SB+n_SB);
			nValue = n_SB/(p_SB+n_SB);
	
			double entropyS3 = -(pValue*Math.log(pValue)) - (nValue*Math.log(nValue));
			
			
			if(Double.isNaN(entropyS1))
				entropyS1 = 0;
			if(Double.isNaN(entropyS2))
				entropyS2 = 0;
			if(Double.isNaN(entropyS3))
				entropyS3 = 0;	
			
			//Calculating the information split
			double IS_S1S2 = (((p_ST+n_ST)/m)*entropyS1)+(((p_SM+n_SM)/m)*entropyS2)+(((p_SB+n_SB)/m)*entropyS3);
			
			return IS_S1S2;
	  }
	  
	  public void calculateInfoGain() throws IOException
	  {		
		  
			for(int z=0;z<dataArr[0].length-1;z++)
			{
			
				String infoGain [][] = new String[dataArr.length][2];
				
				int p=0,n=0; double IG=0;
		
				for(int i=0;i<dataArr.length;i++)	
					infoGain[i][0] = dataArr[i][z];
					
				for(int j=0;j<dataArr.length;j++)
					{
					infoGain[j][1] = dataArr[j][dataArr[0].length-1];	 
					if(infoGain[j][1].contains("positive"))
						p++;
					if(infoGain[j][1].contains("negative"))
						n++;
					}
							
				List<DataValue> list = new ArrayList<DataValue>();
				
				
				for(int i=0;i<dataArr.length;i++)
				list.add(new DataValue(Double.parseDouble(infoGain[i][0]),infoGain[i][1]));
						
				Collections.sort(list,new Feature());
				
				//Calculating the entropy of the entire Gene
				double m = dataArr.length;
		    	double pValue = p/m;
				double  nValue = n/m;
				double entropy_S = -(pValue*Math.log(pValue)) - (nValue*Math.log(nValue));
				double Interval2,Interval1,printInterval1,printInterval2;
				
				double splitInterval1 = (list.get(list.size()-1).value-list.get(0).value)/2;
				double splitInterval2 = (splitInterval1-list.get(0).value)/2;
				double splitInterval3 = splitInterval1+splitInterval2;
				
				double InfoGain1 = entropy_S - findIG(list,splitInterval1,splitInterval2,m);

				double InfoGain2 = entropy_S - findIG(list,splitInterval3,splitInterval1,m);

				//Determining the best split
				if(InfoGain1>InfoGain2){
					Interval1 = splitInterval1;Interval2 = splitInterval2;IG=InfoGain1;
					printInterval1 = splitInterval2;printInterval2 = splitInterval1;
					}
				else{
					Interval1 = splitInterval3;Interval2 = splitInterval1;IG=InfoGain2;
					printInterval1 = splitInterval1;printInterval2 = splitInterval3;
					}
				
				 List<DataValue> splitTop = new ArrayList<DataValue>();
				 List<DataValue> splitMid = new ArrayList<DataValue>();
				 List<DataValue> splitBottom = new ArrayList<DataValue>();	
				 
				 double p_ST= 0,n_ST= 0,p_SB= 0,n_SB= 0,p_SM= 0,n_SM = 0;

				
					for(DataValue d:list){
						if(d.value<Interval2 && d.value<Interval1)
							{
							splitTop.add(d);
								if(d.classValue.equalsIgnoreCase("positive"))
									p_ST ++;
								else if(d.classValue.equalsIgnoreCase("negative"))
									n_ST ++;
							}
						else if(d.value>Interval1 && d.value>Interval2)
							{
							splitBottom.add(d);
								if(d.classValue.equalsIgnoreCase("positive"))
									p_SB ++;
								else if(d.classValue.equalsIgnoreCase("negative"))
									n_SB ++;
							}
						else if(d.value>Interval2 && d.value<Interval1)
							{
							splitMid.add(d);
								if(d.classValue.equalsIgnoreCase("positive"))
									p_SM ++;
								else if(d.classValue.equalsIgnoreCase("negative"))
									n_SM ++;
							}
					}
				
				int colNum = z+1;
				
				DecimalFormat bin_df = new DecimalFormat("#.000");
				DecimalFormat info_df = new DecimalFormat("0.000000");

				//Building dataset which is required to be displayed in the output file 
				String bin1 =  "(-INF,"+bin_df.format(printInterval1)+"], " +p_ST+", "+n_ST+"; ";
				String bin2 =  "("+bin_df.format(printInterval1)+", "+bin_df.format(printInterval2)+"], " +p_SM+", "+n_SM+"; ";
				String bin3 =  "("+bin_df.format(printInterval2)+",+INF], " +p_SB+", "+n_SB+"; ";
				
				String dataLine = "G"+colNum+": Info Gain: "+info_df.format(IG)+" ; Bins: "+bin1+bin2+bin3;
			
				AllList.add(new AllDataValues(IG, Interval1,Interval2,colNum, dataLine));
			}
			
			Collections.sort(AllList,new FeatureSort());
			
			FinalList = AllList.subList(0, K);

			 BufferedWriter  writer;
		     writer = new BufferedWriter(new FileWriter(folder+"/entropybins.txt"));

		     for(AllDataValues d:FinalList){
		    	 	writer.write(d.printLine);
					writer.newLine();
			 }
		     writer.flush();
		     writer.close();
	  }
	  
	  public void discretizeDataValues() throws IOException
	  {
		  	discretizeArr = new String[finalDataArr.length][K];
		  	
		  	for(int i=0;i<K;i++)
		  	{
		  		for(int j=0;j<finalDataArr.length;j++)
		  		{
			  			if(Double.parseDouble(finalDataArr[j][i])<FinalList.get(i).splitValue1 && Double.parseDouble(finalDataArr[j][i])<FinalList.get(i).splitValue2)
			  		{
			  			discretizeArr[j][i] = "a";
			  		}
			  		else if(Double.parseDouble(finalDataArr[j][i])>FinalList.get(i).splitValue1 && Double.parseDouble(finalDataArr[j][i])>FinalList.get(i).splitValue2)
			  		{
			  			discretizeArr[j][i] = "c";
			  		}
			  		else
			  		{
			  			discretizeArr[j][i] = "b";
			  		}
		  		} 
		  	}
		  	
		  	BufferedWriter writer = new BufferedWriter(new FileWriter(folder+"/entropydata.txt"));
	    	
		    for(int i=0;i<discretizeArr.length;i++)
			{
				 String line = "";
					for (int j=0;j<discretizeArr[0].length;j++)
					{
						line +=discretizeArr[i][j]+",";
					}
					line +=finalDataArr[i][K];
					writer.write(line);
				writer.newLine();	
			}
		    
		    writer.flush();
		    writer.close();
	  }
	  
	  public void buildTopK2() throws IOException
	  {
			rankArr = new int[K];
		    for(int i=0;i<K;i++)
		    	{
		    	rankArr[i] = FinalList.get(i).attributeNum;
		    	}

		    finalDataArr= new String[dataArr.length][K+1];

		    for(int i=0;i<K;i++)
		    	for(int j=0;j<dataArr.length;j++)
		    	finalDataArr[j][i]=String.valueOf(Double.parseDouble(dataArr[j][rankArr[i]-1]));
		    
		    for(int i=0;i<dataArr.length;i++)
		    	finalDataArr[i][K] = dataArr[i][dataArr[0].length-1];
		    
		    BufferedWriter writer = new BufferedWriter(new FileWriter(folder+"/topkfeatures2.txt"));
		    for(int i=0;i<finalDataArr.length;i++)
			{
				 String line = "";
					for (int j=0;j<finalDataArr[0].length-1;j++)
					{
						line +=finalDataArr[i][j]+",";
					}
					line +=	finalDataArr[i][finalDataArr[0].length-1];
				writer.write(line);	
				writer.newLine();
			}
			
			writer.flush();
			writer.close();  
		    
	  }
	
public static void main(String args[]) throws Exception 
	{
	 	
	if(args.length < 2)
	    {
	        System.out.println("Proper Usage is: mainSection <K Value> <InputFile>");
	        System.out.println("Please specify the K value and Input File.");
	        System.exit(0);
	    }
	
		mainSection ms = new mainSection(Integer.parseInt(args[0]));
		System.out.println("Running with K="+args[0]+" on file "+args[1]);		
		
		/* Converting the inputted .txt file to .csv format as weka does not accept .txt files */
		ms.txtToCSV(args[1]);
		ms.csvAndArrayCreation();

		/* Converting the .csv file to .arff so it can be used with weka packages */
		ms.convert2ARFF();

		/* Loading the dataset into an instance */
		source =  new DataSource(folder+"/p1data.arff");
		inst = source.getDataSet();  
		inst.setClassIndex(inst.numAttributes()-1);

		/* Using Remove Attribute Filter to select K features for task 1 */
		removeInst = removeInstData(inst,ms.K);	
		ms.buildTopK(removeInst);
		
		System.out.println("Task 1 has been completed. topkfeatures1.txt has been generated");
		
		/*Calculation of Information Gain and Discretizing the data*/
		ms.calculateInfoGain();
	    ms.buildTopK2();
	    ms.discretizeDataValues();
	    
		System.out.println("Task 2 has been completed. Files topkfeatures2.txt, entropybins.txt, entropydata.txt has been generated");

		/* To compute the correlation between the data sets produced in task 1 and task 2.*/
	    ms.findCorrelation();
		 
		System.out.println("Task 3 has been completed. Correlationgenes.txt has been generated");

		/* Deleting unwanted temporary files */
	    Files.deleteIfExists(temp_csv);
		Files.deleteIfExists(csv);
		Files.deleteIfExists(arf); 
	     	     
	    System.out.println("All Tasks completed. Please check the output files.");  
	}
}