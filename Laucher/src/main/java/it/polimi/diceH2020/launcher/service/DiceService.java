package it.polimi.diceH2020.launcher.service;
 

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import it.polimi.diceH2020.launcher.utility.ExcelWriter;
import reactor.bus.Event;
import reactor.bus.EventBus;


@Service
public class DiceService {
 	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
 	@Autowired
 	private EventBus eventBus;
	
	
	@Autowired
	private SimulationsManagerRepository simManagerRepo;

	
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
		
		simManagerRepo.saveAndFlush(simManager);
		eventBus.notify("evaluate", Event.wrap(simManager));
		
	}
	

}