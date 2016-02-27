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
import org.springframework.scheduling.annotation.EnableAsync;
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
	

	public void simulation(SimulationsManager simManager){
		
		long startTime = System.currentTimeMillis();
		DiceServiceImpl ds =(DiceServiceImpl) context.getBean("diceServiceImpl");
		ds.setSSHConnection(host, user, pk, setKnownHosts,psw);
		String path =  ds.setParams(simManager.getAccuracy());
		simManager.setFolderPath(path); 
		simManager.setNumCompletedSimulations(0);
		//COMMON PARAMS
		int num =  simManager.getClassList().size(); //new int [num]
		List<Future<Float>> objectiv= new ArrayList<Future<Float>>(); //necessary for join
		
    	int[] map=new int[num], reduce=new int [num], mapTime=new int[num], reduceTime=new int[num], thinkTime=new int[num];
    	double[] mapRate=new double[num], reduceRate=new double[num], thinkRate=new double[num];
    	for(int i=0;i<num;i++){
    		thinkTime[i] = simManager.getClassList().get(i).getThinkTime();
    	}
    	int stepVMs = simManager.getStepVMs();
		int stepUsrs = simManager.getStepUsers();
		int classes_num = simManager.getClassList().size();
		int[] minVMs = new int[classes_num];
		int[] maxVMs = new int[classes_num];
		int[] minUsers = new int[classes_num];
		int[] maxUsers = new int[classes_num];
		
		int[] usersLoop = new int[classes_num];
		int[] coresLoop = new int[classes_num];
		
		for(int i=0;i<classes_num;i++){
			usersLoop[i] = minUsers[i];
			coresLoop[i] = minVMs[i];
		}
		
    	//END OF COMMON PARAMS
		

			int totalNumOfSimulations = simManager.getSize();
			simManager.setState("running");
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
			    				if(coresLoop[i]+stepVMs<=maxVMs[i]){
			    					coresLoop[i] += stepVMs;
			    					break breakLoop;
			    				}else{
			    					coresLoop[i] = minVMs[i];
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
			    	setSimulation(s,sim+1, simManager.getAccuracy(), users, cores, map,reduce,mapTime,reduceTime,thinkTime,mapRate,reduceRate,thinkRate);
			    	
			    	s.setSimulationsManager(simManager);
			    	simManager.getSimulationsList().add(s);
					//simulationsRepository.save(s);
	
			    	sim++;
			   }
				simulationsManagerRepository.save(simManager);
	
				int sim2 = 0;
				while(sim2<totalNumOfSimulations){
					Simulation currentSimulation = simManager.getSimulationsList().get(sim2);
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
							simManager.setNumCompletedSimulations(simId);
							simulationsRepository.save(simManager.getSimulationsList().get(sim3));
							//double thr = sim_manager.getSimulationsList().get(sim).getThroughput();
							sim3++;
				}
				long totalRunTimeMillis = (System.currentTimeMillis() - startTime);
				double totalRunTime=totalRunTimeMillis/1000.0;
				
				simManager.setTotalRuntime((int)totalRunTime);
				simManager.setState("completed");
				simulationsManagerRepository.save(simManager);
				
				excelFile2.writeListToExcel(simManager.getSimulationsList(),simManager.getFolderPath(),totalRunTime);
				
				logger.info(sim+" simulations completed in "+totalRunTime);
				logger.info("FINISHED");
	    	} catch (IOException e) { //if initialize simulation throws exception
				logger.error("Impossible writing Excel file. "+e.getStackTrace());
				simManager.setState("failed");
			}
	}
	
	private void setSimulation(Simulation s, int count,int accuracy, int[] loopUsers, int[] loopCores, int[] map, int[] reduce,int[] mapTime,int[] reduceTime,int[] thinkTime,double[] mapRate,double[] reduceRate,double[] thinkRate){
		s.setCounter(count);
		s.setMap(map);
		s.setReduce(reduce);
		s.setCores(loopCores);
		s.setUsers(loopUsers);
		
		s.setAccuracy(accuracy);
		
		s.setMapTime(mapTime);
		s.setReduceTime(reduceTime);
		s.setThinkTime(thinkTime);
		
		s.setMapRate(mapRate);
		s.setReduceRate(reduceRate);
		s.setThinkRate(thinkRate);
		
	}

}