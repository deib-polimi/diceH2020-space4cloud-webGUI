package it.polimi.diceH2020.launcher.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.model.Simulation;



/**
 * modelFile <-- model file to be modified to obtain input file
 * inputFile <-- modified model file for the simulator
 * 
 * modelFile's path !=  inputFile's path
 * inputFileDirectory the same of sol
 *  
 * It's prototype so is possible to have multiple bean instances
 */ 
@Component
@Scope("prototype")
public class DiceServiceImpl  {
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceServiceImpl.class.getName());
	private String host;
	private String user;
	private String pk;
	private String setKnownHosts;
	private String psw;
	
	protected void setSSHConnection(String host, String user, String pk, String setKnownHosts, String psw){
		this.host = host;
		this.user = user;
		this.pk = pk;
		this.psw = psw;
		this.setKnownHosts = setKnownHosts;
	}
	private final static String[] ordExtensions =  {".def",".dis",".net",".stat"}; //ordinary extensions required by ordinary simulator
	private final static String[] symExtensions =  {".def",".net"};//symbolic extensions required by symbolic simulator
	private String[] extensions; 
	//private final static String usrs = "_numUsers";
	//private final static String cores = "_numCores";
	private final static String remoteDirectory = "/home/user/Desktop/sim_j/";
	private  int accuracy;
	private static final String model_path = "PN_models/"; 
	private String fileNameWithoutExtension;
	private static String simulations_local_directory;
	//private final Logger logger = Logger.getLogger(DiceServiceImpl.class.getName());
	
	private String model;
	private Boolean erlang;
	private String simulator_type;
	private String modelFilesPathWithoutExtension;
	private String modelInitialFileName;
	private String localFilePathWithoutExtension;
	private String remoteFilePathWithouExtension;
	private String remoteFolder;

	public DiceServiceImpl(){
	}

	public double simfinale(double x,int n,double z){
		//logger.info("il throughput è: "+x);
		double r;
		r=n/x-z;
		return r;
	}
	
	public String setParams(String model,boolean erlang, int accuracy) {
		this.model = model;
		this.erlang = erlang;
		this.accuracy = accuracy;
		this.remoteFolder = "sim-"+new SimpleDateFormat("dd-MM-yyyy_HHmmss").format(new Date())+"/";

		//Useful if I've an ID
		String variables = new String(); //depending on the model I've different variables to set
		
		switch (model) {
			case "V7":
				modelInitialFileName  = "FIFO1"; //added for modularity, to avoid this I should rename all the models file in the same way
				variables ="_";//for simulation's folder name
			break;
			
			case "V10":
				modelInitialFileName  = "FIFO1"; //added for modularity, to avoid this I should rename all the models file in the same way
				variables ="_";//for simulation's folder name
				break;
			default:	
				modelInitialFileName = "FIFO1";
				model = "V10";
				variables = "_";
				break;
		}
		
		if(erlang){ 
			modelFilesPathWithoutExtension = "/Erlang/"; 
			simulator_type = "ord";
			
			this.extensions =  ordExtensions;
		}
		else{
			modelFilesPathWithoutExtension = "/"; 
			simulator_type = "sym";
			this.extensions =  symExtensions;
		}
		String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(new Date());
		simulations_local_directory = "simulations/"+simulator_type+"-sim"+model+variables+"_"+timeStamp+"/";
		return simulations_local_directory;
	}

	/**
	 * 
	 * @param size
	 * @param mapRate
	 * @param reduceRate
	 * @param thinkRate
	 */
	public void initializeSimulationsV10(int size, double[] mapRate, double[] reduceRate, double[] thinkRate){
		commonInitialization(size, mapRate, reduceRate, thinkRate);
 		createNetFile(modelFilesPathWithoutExtension,localFilePathWithoutExtension,String.valueOf(mapRate[0]),String.valueOf(reduceRate[0]),String.valueOf(thinkRate[0]));//creates .net local file);//creates .net local file
	}
	
	/**
	 * This class creates and sends DEF files, already created NET file[, STA file, DIS file] to remote simulator's simulations folder;
	 * executes the simulation, save STA solution file locally in the WS simulation folder.  
	 * To track the completion of threads this method has to return a Future (used for join threads after the fork)
	 * 
	 * @param simulation
	 * @return
	 */
	@Async("workExecutor")
	public Future<Float> startSimulationV10 (Simulation simulation){
		int map = this.getElement(simulation.getMap(),0);
		int reduce = this.getElement(simulation.getReduce(),0);
		int users = this.getElement(simulation.getUsers(),0);
		int cores = this.getElement(simulation.getCores(),0);
		float f = 0;
		GreatSPN petrinet = new GreatSPN(host,user,pk,setKnownHosts,psw);
		String folder = ""+simulation.getCounter();
		
		logger.info("Simulation with id"+folder+" is running on thread"+ Thread.currentThread().getName() );
		String remoteFilePath = remoteFilePathWithouExtension+folder+"/";
		try{
			petrinet.sendFile(localFilePathWithoutExtension+".net", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".net");
			if(erlang){				
					petrinet.sendFile(localFilePathWithoutExtension+".stat", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".stat");
					petrinet.sendFile(localFilePathWithoutExtension+".dis", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".dis");
			}
			//.def file the only one that varies
			String defLocalFilePathWithoutExtension = localFilePathWithoutExtension + "_" + users + "usrs_" + cores + "cores";
			createDefFile(modelFilesPathWithoutExtension,defLocalFilePathWithoutExtension, String.valueOf(cores), String.valueOf(users),String.valueOf(reduce),String.valueOf(map));
			petrinet.sendFile(defLocalFilePathWithoutExtension+".def", remoteFilePath+fileNameWithoutExtension+".def"); //sends .def local file to simulator
			
			double x=0;
			long startTime = System.currentTimeMillis();
			
			String batchString = "";
			if(simulation.getMinBatch() != 0 && simulation.getMaxBatch() != 0){
				batchString = "-m "+simulation.getMinBatch()+" -M "+simulation.getMaxBatch();
			}
			//execute simulator's script
			x=petrinet.runSimulation(simulator_type, remoteFilePath+fileNameWithoutExtension, accuracy,simulations_local_directory+"sim_users"+users+"_cores"+cores+".sta", batchString );
			
			simulation.setThroughputEnd(x);
			simulation.setResponseTime();
			
			long runTimeMillis = (System.currentTimeMillis() - startTime);
			double runTime=runTimeMillis/1000.0;
			simulation.setRuntime(runTime);
			
			f=(float) x;
			logger.info("Simulation with id "+folder+" finished! Thread"+ Thread.currentThread().getName()+" is free." );
			simulation.setState("completed");
		} catch (Exception e) {
			logger.error("Error, impossible to run simulation of type V10 (sending files or executing command)."+e.getMessage());
			simulation.setState("failed");
		}
		return new AsyncResult<Float>(f);
	}
	
	 /**
	  * creates .net file in netFileDestinationPath from the netFileSourcePath
	  * and defines in destination file undefined source params
	  * @param netFileSourcePath  input path
	  * @param netFileDestinationPath output path
	  */
	public void createNetFile(String netFileSourcePath, String netFileDestinationPath,String mapRate,String reduceRate, String thinkRate){
	    BufferedReader br = null;
	    BufferedWriter bw = null;
	    File tmpFile = null;
	    String line;
	    
	    try {
	    	br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(netFileSourcePath+".net")));
		    tmpFile = new File(netFileDestinationPath+".net");
			bw = new BufferedWriter(new FileWriter(tmpFile));
			while ((line = br.readLine()) != null) {
					    if (line.contains("param2"))//map rate
					          line = line.replace("param2", mapRate);//R1: 7.6402345345e-05  R6 2.44000000e-04
					    if (line.contains("param3"))//red rate
					           line = line.replace("param3",reduceRate);//R1: 1.8923434523e-04   R6: 1.94000000e-04
					    if (line.contains("param4"))//think rate
					           line = line.replace("param4", thinkRate);
					    bw.write(line+"\n");
			}
	     } catch (IOException e) {
	        	logger.error("Error creating .net local file for simulation V10 . "+e.getStackTrace());
	     }finally{
	        	if(br != null){
	        		try {
						br.close();
					} catch (IOException e) {}
	        	}
	        	if(bw != null){
	        		try {
						bw.close();
					} catch (IOException e) {}
	        	}	
	      }
	}
	
	/**
	 * Creates DEF file for V10 simulation. Each simulation has one DEF file (!= NET).
	 * This method receive in input 2 paths, the first is the path to the general PNmodel's DEF file that this class will modify, 
	 * the other is the path where the PNmodel's modified file will be saved.
	 * 
	 * @param defFileSourcePath
	 * @param defFileDestinationPath
	 * @param param1  cores
	 * @param param2  users == Coa
	 * @param param4  reduce
	 * @param param5  map
	 * @throws IOException
	 */
	public void createDefFile(String defFileSourcePath, String defFileDestinationPath, String param1, String param2, String param4, String param5){
		String oldFileName = defFileSourcePath+".def";
	    String tmpFileName = defFileDestinationPath+".def";
	    BufferedReader br = null;
	    BufferedWriter bw = null;
	    br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(oldFileName)));
	    try{
	    		bw = new BufferedWriter(new FileWriter(new File(tmpFileName)));
		        String line;
				while ((line = br.readLine()) != null) {
				    if (line.contains("param1")){
				       line = line.replace("param1", param1);// cores
				    }
				    if (line.contains("param2")){
				          line = line.replace("param2", param2);// job/users
				    }
				    if (line.contains("param3")){
				           line = line.replace("param3",param2);//Coa
				    }
				    if (line.contains("param4")){
				           line = line.replace("param4",param4);//reduce
				    }
				    if (line.contains("param5")){
				           line = line.replace("param5",param5);//map
				    }
				    bw.write(line+"\n");		         
                }
	    	} catch (Exception e) {
				logger.error("Error creating .def local file for simulation V10. "+e.getStackTrace());
	    	} finally {
	    		try {
	    			if(br != null)
	    				br.close();
	    		} catch (IOException e) {}
	    		try {
	    			if(bw != null)
	    				bw.close();
	    		} catch (IOException e) {}
	    	}
		}
	
	public void initializeSimulationsV7(int size, double[] mapRate, double[] reduceRate, double[] thinkRate){
		commonInitialization(size, mapRate, reduceRate, thinkRate);
		createNetFile2(modelFilesPathWithoutExtension,localFilePathWithoutExtension,String.valueOf(mapRate[0]),String.valueOf(mapRate[1]),String.valueOf(reduceRate[0]),String.valueOf(reduceRate[1]),String.valueOf(thinkRate[0]),String.valueOf(thinkRate[1]));
	}

	private void commonInitialization(int size, double[] mapRate, double[] reduceRate, double[] thinkRate) {
		
		fileNameWithoutExtension = model+modelInitialFileName; //V10ordFIFO1 <-- NB ~ same file name modelInitialFileName and fileName

		modelFilesPathWithoutExtension = model_path+model+modelFilesPathWithoutExtension;//+modelInitialFileName; //I have to add a /. I add it in the next if-else
		
		modelFilesPathWithoutExtension = modelFilesPathWithoutExtension+modelInitialFileName; //to look for the files into the project 
		
		localFilePathWithoutExtension = simulations_local_directory+fileNameWithoutExtension;
				
		localFilePathWithoutExtension = simulations_local_directory+fileNameWithoutExtension;
		
		remoteFilePathWithouExtension = remoteDirectory+remoteFolder;

		File  f = new File(simulations_local_directory);//create local directory
		f.mkdirs(); //mkdirs (with final s) creates all the relative subdirectories specified in path
		logger.info("Local simulations directory: "+simulations_local_directory);	
		
		DiceServiceImpl.class.getResourceAsStream("");
		if(erlang){ //create local files
			try {
				copyAsStream(modelFilesPathWithoutExtension+".stat",localFilePathWithoutExtension+".stat");
				copyAsStream(modelFilesPathWithoutExtension+".dis",localFilePathWithoutExtension+".dis");
			} catch (IOException e) {
				logger.error("Error copying static (.dis, .sta) files from internal directory to relative. "+e.getStackTrace());
			}
		}
		String stringForThreadDirectories = new String();
		//int size =  ((int)Math.floor((maxNumCores-minCores)/step)+1)*(maxNumUsers-minUsers+1);
		if(size>1){
			stringForThreadDirectories = "{";
			//Creating array that contains path to all subdirectories of remote simulation folder (1 subdirectory per thread)
		
			for (int i=1;i<=size;i++){ //cicle users/jobs
					stringForThreadDirectories += ""+i;
					if(i!=size){
						stringForThreadDirectories += ",";
					}
			}
			stringForThreadDirectories += "}";
		}else{
			stringForThreadDirectories ="1";
		}
 		
			//creating directory and subdirectories 
		if(remoteFilePathWithouExtension!=null&&remoteFilePathWithouExtension!="/"){
				logger.info("Remote simulations directory: "+remoteFilePathWithouExtension + stringForThreadDirectories);
				GreatSPN petrinet = new GreatSPN(host,user,pk,setKnownHosts,psw);
				//if directory already exist erease its content
				try {
					petrinet.getConnection().exec("if [ -d "+ remoteFilePathWithouExtension.substring(remoteFilePathWithouExtension.length()-1) +"  ]; then rm -rf "+remoteFilePathWithouExtension + "*; fi ");
					petrinet.getConnection().exec("mkdir -p "+remoteFilePathWithouExtension + stringForThreadDirectories);
				} catch (Exception e) {
					logger.error("Error, not possible to send a command to the remote server (for creating remote set of folders). "+e.getStackTrace());
				}
		}else{
			logger.error("Error, missing remote directory!");
		}
	}

	public int getElement(int[] arrayOfInts, int index) {
		return arrayOfInts[index];
	}
	
	/**
	 * This class creates and sends DEF files, already created NET file[, STA file, DIS file] to remote simulator's simulations folder;
	 * executes the simulation, save STA solution file locally in the WS simulation folder.  
	 * To track the completion of threads this method has to return a Future (used for join threads after the fork)
	 * @param simulation current simulation
	 * @return
	 */
	@Async("workExecutor")
	public Future<Float> startSimulationV7(Simulation simulation){
		
		float f=0;
		int map = this.getElement(simulation.getMap(),0);
		int map2 = this.getElement(simulation.getMap(),1);
		int reduce = this.getElement(simulation.getReduce(),0);
		int reduce2 = this.getElement(simulation.getReduce(),1);
		int users = this.getElement(simulation.getUsers(),0);
		int users2 = this.getElement(simulation.getUsers(),1);
		int cores = this.getElement(simulation.getCores(),0);
		int cores2 = this.getElement(simulation.getCores(),1);
		GreatSPN petrinet = new GreatSPN(host,user,pk,setKnownHosts,psw);
		String folder = ""+simulation.getCounter();
	    simulation.setState("running");
		logger.info("Simulation with id"+folder+" is running on thread"+ Thread.currentThread().getName() );
		
		String remoteFilePath = remoteFilePathWithouExtension+folder+"/";
		try {
			petrinet.sendFile(localFilePathWithoutExtension+".net", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".net");
			if(erlang){				
					petrinet.sendFile(localFilePathWithoutExtension+".stat", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".stat");
					petrinet.sendFile(localFilePathWithoutExtension+".dis", remoteFilePathWithouExtension+folder+"/"+fileNameWithoutExtension+".dis");
			}
			//.def file the only one that varies
			String defLocalFilePathWithoutExtension = localFilePathWithoutExtension + "_" + users + "usrs_" + cores + "cores_" + users2 + "usrs2_"+ cores2 + "cores2";
			createDefFile2(modelFilesPathWithoutExtension,defLocalFilePathWithoutExtension, String.valueOf(cores), String.valueOf(users),String.valueOf(reduce),String.valueOf(map), String.valueOf(users2), String.valueOf(cores2),  String.valueOf(reduce2),  String.valueOf(map2));
			petrinet.sendFile(defLocalFilePathWithoutExtension+".def", remoteFilePath+fileNameWithoutExtension+".def"); //sends .def local file to simulator
			
			//string to execute simulator command:  swn_sym_sim  or swn_ord_sim
			//path_to_script path_to_file_for_script -a accuracy -c 6
			//petrinet.setStrinvalue("/usr/local/GreatSPN/scripts/swn_"+simulator_type+"_sim /home/user/Desktop/"+remoteFilePathWithouExtension+" -a "+accuracy+" -c 6");
			 
			long startTime = System.currentTimeMillis();
			double[]x = new double[3];
			//execute simulator's script
			
			String batchString = "";
			if(simulation.getMinBatch() != 0 && simulation.getMaxBatch() != 0){
				batchString = "-m "+simulation.getMinBatch()+" -M "+simulation.getMaxBatch();
			}
			x=petrinet.runSimulation2(simulator_type, remoteFilePath+fileNameWithoutExtension, accuracy,simulations_local_directory+"sim_users"+users+"_cores"+cores+"_usrs2"+users2 + "_cores2"+cores2+".sta", batchString );
			
			simulation.setThroughputEnd(x[0]);
			double[] y = {x[1],x[2]};
			
			simulation.setThroughput(y.clone());
			simulation.setResponseTime();
			
			long runTimeMillis = (System.currentTimeMillis() - startTime);
			double runTime=runTimeMillis/1000.0;
			
			simulation.setRuntime(runTime);
			
			//renaming solution files
			f=(float) x[0];
			logger.info("Simulation with id "+folder+" finished! Thread"+ Thread.currentThread().getName()+" is free." );
			simulation.setState("completed");
		} catch (Exception e) {
			logger.error("Error, impossible to run simulation of type V7 (sending files or executing command)."+e.getMessage());
			simulation.setState("failed");
		}
		return new AsyncResult<Float>(f);
	}

	public void createNetFile2(String netFileSourcePath, String netFileDestinationPath,String mapRate, String mapRate2, String reduceRate, String reduceRate2, String thinkRate, String thinkRate2){
	    BufferedReader br = null;
	    BufferedWriter bw = null;
	    File tmpFile = null;
	    String line;
	    try {
	    	tmpFile = new File(netFileDestinationPath+".net");
	    	br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(netFileSourcePath+".net")));
			bw = new BufferedWriter(new FileWriter(tmpFile));
				
			while ((line = br.readLine()) != null) {
					   /* if (line.contains("param1"))//used in .net in V3 not in V10
					       line = line.replace("param1", "1.111" );*/
					    if (line.contains("parmr1"))//map rate
					          line = line.replace("parmr1", mapRate);//R1: 7.6402345345e-05  R6 2.44000000e-04
					    if (line.contains("parmr2"))//map rate
					          line = line.replace("parmr2", mapRate2);//R1: 7.6402345345e-05  R6 2.44000000e-04

					    if (line.contains("parrr1"))//red rate
					           line = line.replace("parrr1",reduceRate);//R1: 1.8923434523e-04   R6: 1.94000000e-04
					    if (line.contains("parrr2"))//red rate
					           line = line.replace("parrr2",reduceRate2);//R1: 1.8923434523e-04   R6: 1.94000000e-04
					    
					    if (line.contains("partr1"))//think rate
					           line = line.replace("partr1", thinkRate);
					    if (line.contains("partr2"))//think rate
					           line = line.replace("partr2", thinkRate2);
					    
					    bw.write(line+"\n");
				}
		} catch (Exception e) {
				logger.error("Error creating .net local file for simulation V7. "+e.getStackTrace());
		} finally {
		    	try {
		    		if(br != null)
		    			br.close();
		    	} catch (IOException e) {}
		    		try {
		    			if(bw != null)
		    				bw.close();
		    		} catch (IOException e) {}
		}
	}
	
	/**
	 * Creates DEF file for V7 simulation. Each simulation has one DEF file (!= NET).
	 * This method receive in input 2 paths, the first is the path to the general PNmodel's DEF file that this class will modify, 
	 * the other is the path where the PNmodel's modified file will be saved.
	 * 
	 * @param defFileSourcePath in PN_models 
	 * @param defFileDestinationPath current simulations' folder
	 * @param param1  cores1
	 * @param param2  users1 == Coa
	 * @param param4  reduce
	 * @param param5  map
	 * @param usr2    users2 == Cob
	 * @param cor2	  cores2
	 * @param red2    reduce2
	 * @param map2    map2
	 */
	public void createDefFile2(String defFileSourcePath, String defFileDestinationPath, String param1, String param2, String param4, String param5, String usr2, String cor2, String red2, String map2){
		String oldFileName = defFileSourcePath+".def";
	    String tmpFileName = defFileDestinationPath+".def";
	    BufferedReader br = null;
	    BufferedWriter bw = null;
		String line;
		try {
		    br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(oldFileName)));
			bw = new BufferedWriter(new FileWriter(new File(tmpFileName)));
			while ((line = br.readLine()) != null) {
				if (line.contains("parcor1")){
					line = line.replace("parcor1", param1);// cores
				}
				if (line.contains("parcor2")){
					line = line.replace("parcor2", cor2);// cores
				}
			    if (line.contains("parusr1")){
			          line = line.replace("parusr1", param2);// job/users
			    }
			    if (line.contains("parusr2")){
			          line = line.replace("parusr2", usr2);// job/users
			    }
			    if (line.contains("parcou1")){
			           line = line.replace("parcou1",param2);//Coa
			    }
			    if (line.contains("parcou2")){
			           line = line.replace("parcou2",usr2);//Coa
			    }
			    if (line.contains("parred1")){
			           line = line.replace("parred1",param4);//reduce
			    }
			    if (line.contains("parred2")){
			           line = line.replace("parred2",red2);//reduce
			    }
			    if (line.contains("parmap1")){
			           line = line.replace("parmap1",param5);//map
			    }
			    if (line.contains("parmap2")){
			           line = line.replace("parmap2",map2);//map
			    }
			    
			    bw.write(line+"\n");		         
			}
		} catch (Exception e) {
			logger.error("Error creating .def local file for simulation V7. "+e.getStackTrace());
    	} finally {
    		try {
    			if(br != null)
    				br.close();
    		} catch (IOException e) {}
    		try {
    			if(bw != null)
    				bw.close();
    		} catch (IOException e) {}
    	}
	}

	public String[] getExtensions() {
		return extensions;
	}

	public void setExtensions(String[] extensions) {
		this.extensions = extensions;
	}
	
	/**
	 * This method is essential, it works both for IDE and for .jar. 
	 * @throws IOException 
	 */
	public void copyAsStream(String from, String to) throws IOException{
			InputStream in;
			OutputStream out;
			in = this.getClass().getClassLoader().getResourceAsStream(from);
			out = new FileOutputStream(to);
			IOUtils.copy(in, out);
	}
	
	
}
