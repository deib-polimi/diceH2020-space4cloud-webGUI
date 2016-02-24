package it.polimi.diceH2020.launcher.service;
 

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.model.Simulation;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsRepository;
import it.polimi.diceH2020.launcher.utility.ExcelWriter;
import it.polimi.diceH2020.launcher.utility.ExcelWriter2;


@Service
@EnableAsync
public class DiceService {
 	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
	@Value("${gspnHost}") 
	private String host;
	@Value("${gspnUser}") 
	private String user;
	/*@Value("${psw}") 
	private String psw;*/
	@Value("${gspnUserPrivateKeyFile}") 
	private String pk;
	@Value("${gspnSetKnownHosts}") 
	private String setKnownHosts;
	//@Value("${gspnPassphrase}") 
	private String psw;

	@Autowired
	private SimulationsManagerRepository simulationsManagerRepository;
	
	@Value("${pool.size}")    //inject value from properties, default is 10
	private int poolSize;
	@Value("${queue.capacity:10000}")//inject value from properties, default is 1000
	private int queueCapacity; 
	
	@Autowired
	private SimulationsRepository simulationsRepository;
	
	
	/*
	 * it's prototype cannot have multiple instances from singleton bean in this way
	@Autowired 
	DiceServiceImpl ds;
	
	so I've to add
	 */ 
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private ExcelWriter2 excelFile2;
	
	@Autowired
	private ExcelWriter excelFile;
	
	//handle a pool of threads (they have to implement Runnable to be executed) != @component

//	@Bean(name="workExecutor")
//	public TaskExecutor taskExecutor(){
//		SshConnector_useles sshConnection = new SshConnector_useles(host, user, psw, pk, setKnownHosts);
//		List<String> list;
//		int maxPoolSize = 2;
//		
//		try {
//			list = sshConnection.exec("cat /proc/cpuinfo | grep processor | tail -n 1 | awk '{ print $NF + 1 }'");
//			maxPoolSize = Integer.valueOf(list.get(0).replaceAll("[^\\d]", ""));
//		} catch (Exception e) {
//			logger.error("Error, impossible to send the initial command to the simulator remote machine. "+e.getStackTrace());
//		}
//		
//		logger.info("maxPoolSize set to "+maxPoolSize);
//		
//		if(poolSize > maxPoolSize){
//			poolSize = maxPoolSize;
//		}
//	    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//	    taskExecutor.setMaxPoolSize(poolSize);
//	    taskExecutor.setCorePoolSize(poolSize);
//	    taskExecutor.setThreadNamePrefix("");
//	    taskExecutor.afterPropertiesSet();
//		logger.info("poolSize set to "+poolSize);
//
//	    return taskExecutor;
//	}
	
	public void simulation(SimulationsManager sim_manager){
		
		long startTime = System.currentTimeMillis();
		DiceServiceImpl ds =(DiceServiceImpl) context.getBean("diceServiceImpl");
		ds.setSSHConnection(host, user, pk, setKnownHosts,psw);
		String path =  ds.setParams(sim_manager.getModel(),sim_manager.isErlang(),sim_manager.getAccuracy());
		sim_manager.setFolderPath(path); 
		sim_manager.setNum_of_completed_simulations(0);
		//COMMON PARAMS
		int num =  sim_manager.getClassList().size(); //new int [num]
		List<Future<Float>> objectiv= new ArrayList<Future<Float>>(); //necessary for join
		
    	int[] map=new int[num], reduce=new int [num], mapTime=new int[num], reduceTime=new int[num], thinkTime=new int[num];
    	double[] mapRate=new double[num], reduceRate=new double[num], thinkRate=new double[num];
    	for(int i=0;i<num;i++){
    		map[i] = sim_manager.getClassList().get(i).getMap();
    		reduce[i] = sim_manager.getClassList().get(i).getReduce();
    		mapTime[i] = sim_manager.getClassList().get(i).getMapTime();
    		reduceTime[i] = sim_manager.getClassList().get(i).getReduceTime();
    		thinkTime[i] = sim_manager.getClassList().get(i).getThinkTime();
    		mapRate[i] = sim_manager.getClassList().get(i).getMapRate();
    		reduceRate[i] = sim_manager.getClassList().get(i).getReduceRate();
    		thinkRate[i] = sim_manager.getClassList().get(i).getThinkRate();
    	}
    	int stepCores = sim_manager.getStepCores();
		int stepUsrs = sim_manager.getStepUsrs();
		int classes_num = sim_manager.getClassList().size();
		int[] minCores = new int[classes_num];
		int[] maxCores = new int[classes_num];
		int[] minUsers = new int[classes_num];
		int[] maxUsers = new int[classes_num];
		
		int[] usersLoop = new int[classes_num];
		int[] coresLoop = new int[classes_num];
		
		for(int i=0;i<classes_num;i++){
			minCores[i] = sim_manager.getClassList().get(i).getMinNumCores();
			maxCores[i] = sim_manager.getClassList().get(i).getMaxNumCores();
			minUsers[i] = sim_manager.getClassList().get(i).getMinNumUsers();
			maxUsers[i] = sim_manager.getClassList().get(i).getMaxNumUsers();
			usersLoop[i] = minUsers[i];
			coresLoop[i] = minCores[i];
		}
		
    	//END OF COMMON PARAMS
		

			int totalNumOfSimulations = sim_manager.getSize();
			sim_manager.setState("running");
	    	try {
				ds.initializeSimulationsV7(totalNumOfSimulations,mapRate,reduceRate,thinkRate);
				int sim = 0;
				while(sim<totalNumOfSimulations){
					//logger.info("Users: "+usersLoop[0]+" "+usersLoop[1]+" cores "+coresLoop[0]+" "+coresLoop[1]);
					//logger.info(sim+": "+Arrays.toString(coresLoop)+Arrays.toString(usersLoop));
			    	if(sim !=0){
			    		breakLoop:
			    		while(true){
			    			for(int i=classes_num-1;i>=0;i--){ //setting up cores[] and users[] for the specific simulation
			    				if(coresLoop[i]+stepCores<=maxCores[i]){
			    					coresLoop[i] += stepCores;
			    					break breakLoop;
			    				}else{
			    					coresLoop[i] = minCores[i];
			    				}
			    			}
			    			for(int i=classes_num-1;i>=0;i--){
			    				if(usersLoop[i]+stepUsrs<=maxUsers[i]){
			    					usersLoop[i] += stepUsrs;
			    					break breakLoop;
			    				}
			    				else{
			    					usersLoop[i] = minUsers[i];
			    				}
			    			}
			    		}
			    	}
			    	int[] users = usersLoop.clone();
					int[] cores = coresLoop.clone();
					
					
					Simulation s = new Simulation(); 
			    	setSimulation(s,sim+1, sim_manager.getAccuracy(), sim_manager.isErlang(), users, cores, map,reduce,mapTime,reduceTime,thinkTime,mapRate,reduceRate,thinkRate, sim_manager.getMinNumOfBatch() ,sim_manager.getMaxNumOfBatch());
			    	
			    	s.setSimulationsManager(sim_manager);
			    	sim_manager.getSimulationsList().add(s);
					//simulationsRepository.save(s);
	
			    	sim++;
			   }
				simulationsManagerRepository.save(sim_manager);
	
				int sim2 = 0;
				while(sim2<totalNumOfSimulations){
					Simulation currentSimulation = sim_manager.getSimulationsList().get(sim2);
					currentSimulation.setState("running");
					objectiv.add(ds.startSimulationV7(currentSimulation));
	
			    	sim2++;
				}
			    /*Before I've set up the simulations and I've only sent the execution command to the simulator(trough @asynch tasks)
			     * now I've to join these threads. 
			     */
				int sim3 = 0;
				while(sim3<totalNumOfSimulations){
							int simId = sim3 +1;
							logger.info("Waiting for simulation id "+simId+" result");
							
							try {
								objectiv.get(sim3).get();//Join (Synchronize threads) 
							} catch (InterruptedException e) {
								logger.error("Error in simulation V7 threads join. "+e.getStackTrace());
							} catch (ExecutionException e) {
								logger.error("Error in simulation V7 threads join. "+e.getStackTrace());
							}
							sim_manager.setNum_of_completed_simulations(simId);
							simulationsRepository.save(sim_manager.getSimulationsList().get(sim3));
							//double thr = sim_manager.getSimulationsList().get(sim).getThroughput();
							sim3++;
				}
				long totalRunTimeMillis = (System.currentTimeMillis() - startTime);
				double totalRunTime=totalRunTimeMillis/1000.0;
				
				sim_manager.setTotalRuntime((int)totalRunTime);
				sim_manager.setState("completed");
				simulationsManagerRepository.save(sim_manager);
				
				excelFile2.writeListToExcel(sim_manager.getSimulationsList(),sim_manager.getFolderPath(),totalRunTime);
				
				logger.info(sim+" simulations completed in "+totalRunTime);
				logger.info("FINISHED");
	    	} catch (IOException e) { //if initialize simulation throws exception
				logger.error("Impossible writing Excel file. "+e.getStackTrace());
				sim_manager.setState("failed");
			}
	}
	
	private void setSimulation(Simulation s, int count,int accuracy,boolean erlang, int[] loopUsers, int[] loopCores, int[] map, int[] reduce,int[] mapTime,int[] reduceTime,int[] thinkTime,double[] mapRate,double[] reduceRate,double[] thinkRate,int minBatch, int maxBatch){
		s.setCounter(count);
		s.setMap(map);
		s.setReduce(reduce);
		s.setCores(loopCores);
		s.setUsers(loopUsers);
		
		s.setAccuracy(accuracy);
		s.setErlang(erlang);
		
		s.setMapTime(mapTime);
		s.setReduceTime(reduceTime);
		s.setThinkTime(thinkTime);
		
		s.setMapRate(mapRate);
		s.setReduceRate(reduceRate);
		s.setThinkRate(thinkRate);
		
		s.setMinBatch(minBatch);
		s.setMaxBatch(maxBatch);
	}
	
	public void simulationV10(SimulationsManager sim_manager){
		List<Future<Float>> objectiv= new ArrayList<Future<Float>>(); //used to join the @asynch threads after the fork
		long startTime = System.currentTimeMillis();
		DiceServiceImpl ds =(DiceServiceImpl) context.getBean("diceServiceImpl");
		ds.setSSHConnection(host, user, pk, setKnownHosts, psw);
		String path = ds.setParams(sim_manager.getModel(),sim_manager.isErlang(), sim_manager.getAccuracy()); 
		sim_manager.setFolderPath(path); 
		sim_manager.setNum_of_completed_simulations(0);


		
		//COMMON PARAMS
		int num =  1; //class list size
    	int[] map=new int[num], reduce=new int [num], mapTime=new int[num], reduceTime=new int[num], thinkTime=new int[num];
    	double[] mapRate=new double[num], reduceRate=new double[num], thinkRate=new double[num];
    	for(int i=0;i<num;i++){
    		map[i] = sim_manager.getClassList().get(i).getMap();
    		reduce[i] = sim_manager.getClassList().get(i).getReduce();
    		mapTime[i] = sim_manager.getClassList().get(i).getMapTime();
    		reduceTime[i] = sim_manager.getClassList().get(i).getReduceTime();
    		thinkTime[i] = sim_manager.getClassList().get(i).getThinkTime();
    		mapRate[i] = sim_manager.getClassList().get(i).getMapRate();
    		reduceRate[i] = sim_manager.getClassList().get(i).getReduceRate();
    		thinkRate[i] = sim_manager.getClassList().get(i).getThinkRate();
    	}
    	int stepCores = sim_manager.getStepCores();
		int stepUsrs = sim_manager.getStepUsrs();
		int classes_num = 1;
		int[] minCores = new int[classes_num];
		int[] maxCores = new int[classes_num];
		int[] minUsers = new int[classes_num];
		int[] maxUsers = new int[classes_num];
		
		int[] usersLoop = new int[classes_num];
		int[] coresLoop = new int[classes_num];
		
		for(int i=0;i<classes_num;i++){
			minCores[i] = sim_manager.getClassList().get(i).getMinNumCores();
			maxCores[i] = sim_manager.getClassList().get(i).getMaxNumCores();
			minUsers[i] = sim_manager.getClassList().get(i).getMinNumUsers();
			maxUsers[i] = sim_manager.getClassList().get(i).getMaxNumUsers();
			usersLoop[i] = minUsers[i];
			coresLoop[i] = minCores[i];
		}
    	//END OF COMMON PARAMS
    		try{
	    	ds.initializeSimulationsV10(sim_manager.getSize(),mapRate,reduceRate,thinkRate);
			int sim = 0;
			int totalNumOfSimulations = sim_manager.getSize();
			while(sim<totalNumOfSimulations){
		    	
					//logger.info("Users: "+usersLoop[0]+" "+usersLoop[1]+" cores "+coresLoop[0]+" "+coresLoop[1]);
					//logger.info(sim+": "+Arrays.toString(coresLoop)+Arrays.toString(usersLoop));
			    	if(sim !=0){
			    		breakLoop:
			    		while(true){
			    			for(int i=classes_num-1;i>=0;i--){ //setting up cores[] and users[] for the specific simulation
			    				if(coresLoop[i]+stepCores<=maxCores[i]){
			    					coresLoop[i] += stepCores;
			    					break breakLoop;
			    				}else{
			    					coresLoop[i] = minCores[i];
			    				}
			    			}
			    			for(int i=classes_num-1;i>=0;i--){
			    				if(usersLoop[i]+stepUsrs<=maxUsers[i]){
			    					usersLoop[i] += stepUsrs;
			    					break breakLoop;
			    				}
			    				else{
			    					usersLoop[i] = minUsers[i];
			    				}
			    			}
			    		}
			    	}
			    	int[] users = usersLoop.clone();
					int[] cores = coresLoop.clone();
					
					
					Simulation s = new Simulation(); 
			    	setSimulation(s,sim+1, sim_manager.getAccuracy(), sim_manager.isErlang(), users, cores, map,reduce,mapTime,reduceTime,thinkTime,mapRate,reduceRate,thinkRate, sim_manager.getMinNumOfBatch() ,sim_manager.getMaxNumOfBatch());
			    	s.setSimulationsManager(sim_manager);
			    	sim_manager.getSimulationsList().add(s);
			    	//objectiv.add(ds.startSimulationV10(s));
			    	sim++;
		   }
			
			simulationsManagerRepository.save(sim_manager);
			int sim2 = 0;
			while(sim2<totalNumOfSimulations){
				
				objectiv.add(ds.startSimulationV10(sim_manager.getSimulationsList().get(sim2)));
		    	sim2++;
			}
		    
			/*Before I've set up the simulations and I've only sent the execution command to the simulator(trough @asynch tasks)
		     * now I've to join these threads. 
		     */
			int sim3 = 0;
			while(sim3<totalNumOfSimulations){
						int simId = sim3 +1;
						logger.info("Waiting for simulation id "+simId+" result");
						try {
							objectiv.get(sim3).get();
						} catch (InterruptedException e) {
							logger.error("Error in simulation V10 threads join. "+e.getStackTrace());
						} catch (ExecutionException e) {
							logger.error("Error in simulation V10 threads join. "+e.getStackTrace());
						}//Join (Synchronize threads) 
						//double thr = sim_manager.getSimulationsList().get(sim).getThroughput();
						sim_manager.setNum_of_completed_simulations(simId);
						simulationsRepository.save(sim_manager.getSimulationsList().get(sim3));

						sim3++;
			}
			long totalRunTimeMillis = (System.currentTimeMillis() - startTime);
			double totalRunTime=totalRunTimeMillis/1000.0;
			sim_manager.setTotalRuntime((int)totalRunTime);
			sim_manager.setState("completed");
			simulationsManagerRepository.save(sim_manager);
			
			excelFile.writeListToExcel(sim_manager.getSimulationsList(),sim_manager.getFolderPath(),totalRunTime);
			logger.info(sim2+" simulations completed in "+totalRunTime);
			
    		} catch (Exception e) {
    			sim_manager.setState("failed");
				logger.error("Error in simulation V10 excel creation. "+e.getStackTrace());
    		}
       logger.info("FINISHED");
	}
	
}