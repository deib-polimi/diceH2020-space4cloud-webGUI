package it.polimi.diceH2020.launcher.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.reactor.JobsDispatcher;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;




@Service
public class DiceService{
 	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
	@Autowired
	private SimulationsManagerRepository simManagerRepo;
	
	@Autowired
	JobsDispatcher dispatcher;

	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
	public synchronized void simulation(SimulationsManager simManager){
		updateManager(simManager);
		simManager.getExperimentsList().stream().forEach(e-> {
			dispatcher.enqueueJob(e);
		});
	}
	
	public synchronized void simulation(InteractiveExperiment exp){
		dispatcher.enqueueJob(exp);
	}
	
	public synchronized void updateExp(InteractiveExperiment intExp){
		long startTime = System.currentTimeMillis();
		intExpRepo.saveAndFlush(intExp);
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		logger.info("[LOCKS] Exp"+intExp.getId()+"updated[state: "+intExp.getState()+"] in "+duration+" ");
	}
	
	public synchronized void updateManager(SimulationsManager simulationsManager){
		simManagerRepo.saveAndFlush(simulationsManager);
		logger.info("[LOCKS] SimManager"+simulationsManager.getId()+" has been updated.");
	}
	
	@PostConstruct
	private void setUpEnvironment(){
		fixRunningSimulations();
	}
	
	public void fixRunningSimulations(){
		List<InteractiveExperiment> previuoslyRunningExperiments = intExpRepo.findByStateOrderByIdAsc(States.RUNNING);
		List<InteractiveExperiment> previouslyReadyExperiments = intExpRepo.findByStateOrderByIdAsc(States.READY);//jobs in Q
		
		List<Long> managersToRefresh = new ArrayList<Long>();
		
		previuoslyRunningExperiments.addAll(previouslyReadyExperiments);
		
		for(int i=0;i<previuoslyRunningExperiments.size();i++){
			previuoslyRunningExperiments.get(i).setState(States.ERROR);
			Long smID = previuoslyRunningExperiments.get(i).getSimulationsManager().getId();
			updateExp(previuoslyRunningExperiments.get(i));
			if(!managersToRefresh.contains(smID)){
				managersToRefresh.add(smID);
			}
		}
		
		for(int i=0;i<managersToRefresh.size();i++){
			SimulationsManager sm = simManagerRepo.findById(managersToRefresh.get(i));
			sm.setState(States.INTERRUPTED);
			updateManager(sm);
		}
	}
	
	public Map<String,String> getWsStatus(){
		return dispatcher.getWsStatus();
	}
	
	public int getQueueSize(){
		return dispatcher.getQueueSize();
	}
	
	public int getPrivateQueueSize(){
		return dispatcher.getNumPrivateExperiments();
	}
	
	public void signalPrivateExperimentEnd(){
		dispatcher.signalPrivateExperimentEnd();
	}
	
	public void setChannelState(DiceConsumer consumer, States state){
		dispatcher.notifyChannelStatus(consumer, state);
	}
}