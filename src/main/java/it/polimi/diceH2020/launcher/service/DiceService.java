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
	
	//private Map<DiceConsumer, ArrayList<InteractiveExperiment>> consumerExperimentsMap;
	
	@Autowired
	private SimulationsManagerRepository simManagerRepo;
	
	@Autowired
	JobsDispatcher dispatcher;
	
//	@Autowired
//	private ApplicationContext context;
//	
//	@Autowired
//	private Settings settings;

	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
//	@Autowired
//	private RestCommunicationWrapper restWrapper;
	
	public void simulation(SimulationsManager simManager){
		updateManager(simManager);
		simManager.getExperimentsList().stream().forEach(e-> {
			dispatcher.enqueueJob(e);
			//DiceConsumer bestConsumer = getBestChannel();
			//if(bestConsumer != null){
				//int bestChannel = bestConsumer.getId();
				//String channel = "channel"+bestChannel;
				//int prevSize = consumerExperimentsMap.get(bestConsumer).size();
				//consumerExperimentsMap.get(bestConsumer).add(e);
				//logger.info("[LOCKS] Exp"+e.getId()+" has been sent to queue on thread/"+channel);
				//eventBus.notify(channel, Event.wrap(e));
//			}else{
//				logger.info("All WS aren't working.");
//				setFailedManager(simManager);
//				updateManager(simManager);
//				return;
//			}
		});
		//printStatus();
	}
	
	
//	private void setFailedManager(SimulationsManager simManager){
//		simManager.getExperimentsList().stream().forEach(e-> {
//			e.setState(States.ERROR);
//		});
//		simManager.refreshState();
//	}
	
	public void simulation(InteractiveExperiment exp){
		dispatcher.enqueueJob(exp);
//		DiceConsumer bestConsumer = getBestChannel();
//		if(bestConsumer != null){
//			int bestChannel = bestConsumer.getId();
//			String channel = "channel"+bestChannel;
//			consumerExperimentsMap.get(bestConsumer).add(exp);
//			logger.info("[LOCKS] Exp"+exp.getId()+"(Releaunched) has been sent to queue on thread/"+channel);
//			eventBus.notify(channel, Event.wrap(exp));
//			printStatus();
//		}else{
//			logger.info("All WS aren't working.");
//			exp.setState(States.ERROR);
//			exp.getSimulationsManager().refreshState();
//			updateManager(exp.getSimulationsManager());
//			//updateExp(exp);
//		}
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
		//createChannels();
	}
	
	public void fixRunningSimulations(){
		//TODO update also job in queue
		List<InteractiveExperiment> previuoslyRunningExperiments = intExpRepo.findByState(States.RUNNING);
		List<Long> managersToRefresh = new ArrayList<Long>();
		
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
	
//	private void createChannels(){
//		consumerExperimentsMap = new HashMap<DiceConsumer,ArrayList<InteractiveExperiment>>();
//		for(int i=settings.getPorts().length-1; i >= 0; i--){
//			DiceConsumer diceConsumer= (DiceConsumer)context.getBean("diceConsumer",i,settings.getPorts()[i]);
//			consumerExperimentsMap.put(diceConsumer,new ArrayList<InteractiveExperiment>());
//		}
//	}
	
//	private synchronized DiceConsumer getBestChannel(){
//		int bestChannelSize = Integer.MAX_VALUE;
//		DiceConsumer dc = new DiceConsumer();
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			if(entry.getKey().isWorking()){
//				if(entry.getValue().size()<bestChannelSize){
//					bestChannelSize = entry.getValue().size();
//					dc = entry.getKey();
//				}
//			}
//		}
//		
//		if(bestChannelSize == Integer.MAX_VALUE){
//			return null;
//		}else{
//			return dc;
//		}
//	}
	
	
	
//	public synchronized void updateBestChannel(Integer id, InteractiveExperiment exp){
//		DiceConsumer dc = getConsumer(id);
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			if(entry.getKey().equals(dc)){
//				int prevSize = entry.getValue().size();
//				entry.getValue().remove(exp);
//			}
//		}
//		printStatus();
//	}
	
//	private DiceConsumer getConsumer(Integer id){
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			if(entry.getKey().getId()==id){
//					return entry.getKey();
//			}
//		}
//		return null;
//	}
	public Map<String,String> getWsStatus(){
		return dispatcher.getWsStatus();
	}
	
	public int getQueueSize(){
		return dispatcher.getQueueSize();
	}
	
	public void setChannelState(DiceConsumer consumer, States state){
		dispatcher.notifyChannelStatus(consumer, state);
	}
//	public Map<String,String> getWsStatus(){
//		Map<String,String> status = new HashMap<String,String>();
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			status.put(entry.getKey().getPort(), entry.getKey().getState().toString());
//		}
//		return status;
//	}
	
//	private void printStatus(){
//		String queueStatus ="[Q-STATUS] ";
//		
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			queueStatus += entry.getKey().getPort()+": "+entry.getValue().size()+" || ";
//		}
//		System.out.println(queueStatus);
//	}
}