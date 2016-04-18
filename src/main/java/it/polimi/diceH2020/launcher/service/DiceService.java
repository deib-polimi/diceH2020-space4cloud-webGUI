package it.polimi.diceH2020.launcher.service;
 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.Settings;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import reactor.bus.Event;
import reactor.bus.EventBus;


@Service
public class DiceService {
 	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
	private Map<DiceConsumer, ArrayList<InteractiveExperiment>> consumerExperimentsMap;
	
 	@Autowired
 	private EventBus eventBus;
	
	@Autowired
	private SimulationsManagerRepository simManagerRepo;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private Settings settings;

	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
	@Autowired
	private RestCommunicationWrapper restWrapper;
	
	public void simulation(SimulationsManager simManager){
		updateManager(simManager);
		//refreshChannelStatus();
		simManager.getExperimentsList().stream().forEach(e-> {
			DiceConsumer bestConsumer = getBestChannel();
			if(bestConsumer != null){
				int bestChannel = bestConsumer.getId();
				String channel = "channel"+bestChannel;
				int prevSize = consumerExperimentsMap.get(bestConsumer).size();
				consumerExperimentsMap.get(bestConsumer).add(e);
				System.out.println("[ASD]"+prevSize+", "+consumerExperimentsMap.get(bestConsumer).size());
				logger.info("[LOCKS] Exp"+e.getId()+" has been sent to queue on thread/"+channel);
				eventBus.notify(channel, Event.wrap(e));
			}else{
				logger.info("All WS aren't working.");
				setFailedManager(simManager);
				updateManager(simManager);
				return;
			}
		});
		printStatus();
	}
	
	private void setFailedManager(SimulationsManager simManager){
		simManager.getExperimentsList().stream().forEach(e-> {
			e.setState(States.ERROR);
		});
		simManager.refreshState();
	}
	
	public void simulation(InteractiveExperiment exp){
		//SimulationsManager simManager = exp.getSimulationsManager();
		//updateExp(exp);
		//refreshChannelStatus();
		DiceConsumer bestConsumer = getBestChannel();
		if(bestConsumer != null){
			int bestChannel = bestConsumer.getId();
			String channel = "channel"+bestChannel;
			consumerExperimentsMap.get(bestConsumer).add(exp);
			logger.info("[LOCKS] Exp"+exp.getId()+"(Releaunched) has been sent to queue on thread/"+channel);
			eventBus.notify(channel, Event.wrap(exp));
			printStatus();
		}else{
			logger.info("All WS aren't working.");
			exp.setState(States.ERROR);
			exp.getSimulationsManager().refreshState();
			updateManager(exp.getSimulationsManager());
			//updateExp(exp);
		}
		
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
		createChannels();
	}
	
	public void fixRunningSimulations(){
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
			//sm.refreshState();
			sm.setState(States.INTERRUPTED);
			updateManager(sm);
		}
	}
	
	private void createChannels(){
		//channelsUsageList = new ArrayList<ArrayList<Integer>>();
		consumerExperimentsMap = new HashMap<DiceConsumer,ArrayList<InteractiveExperiment>>();
		//consumersList = new ArrayList<DiceConsumer>();
		//channelsUsageList.add(new ArrayList<Integer>());
		for(int i=settings.getPorts().length-1; i >= 0; i--){
			DiceConsumer diceConsumer= (DiceConsumer)context.getBean("diceConsumer",i,settings.getPorts()[i]);
			//consumer.register(i);
			//consumersList.add(0,consumer);
			//channelsUsageList.get(0).add(0,i);
			consumerExperimentsMap.put(diceConsumer,new ArrayList<InteractiveExperiment>());
		}
	}
	
//	//need to be synchronized to avoid conflicts with getBestChannels and updateBestChannels
//	private synchronized void refreshChannelStatus(){
//		//List<InteractiveExperiment> pendingExperimentList = new ArrayList<InteractiveExperiment>(); //experiment in not working channels
//		List<Integer> channelIDToRemove = new ArrayList<Integer>(); //channels no more working ids
//		List<Integer> workingChannelsID = new ArrayList<Integer>(); //
//		
//		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
//			if(!entry.getKey().isWorking()){
//					//pendingExperimentList.addAll(entry.getValue());
//					channelIDToRemove.add(entry.getKey().getId());
//			}else{
//				workingChannelsID.add(entry.getKey().getId());
//			}
//		}
//		
//		List<Integer> currentlyUsedID = removeNotWorkingChannels(channelIDToRemove);
//		
//		workingChannelsID.removeAll(currentlyUsedID); //workingChannelsID--> missingWorkingChannelsID
//		if(!workingChannelsID.isEmpty()){
//			addMissingWorkingChannels(workingChannelsID);
//		}
//		//movePendingExperiments(pendingExperimentList);
//	}
	
//	private void movePendingExperiments(List<InteractiveExperiment> pendingExperimentList){
//		pendingExperimentList.stream().forEach(e-> {
//			String channel = "channel"+getBestChannel();
//			logger.info("[LOCKS] Exp"+e.getId()+" has been moved to queue on thread/"+channel);
//			eventBus.notify(channel, Event.wrap(e));
//		});
//	}

//	private List<Integer> removeNotWorkingChannels(List<Integer>idToRemove){
//		List<Integer> tmpChannels = new ArrayList<Integer>();
//
//		for(int i=0; i< channelsUsageList.size();i++){
//			int currListSize = channelsUsageList.get(i).size();
//			if(currListSize!=0){//TODO remove?
//				if(channelsUsageList.get(i).get(0)!=null||currListSize != 1){
//					if(!idToRemove.isEmpty()){ //I need to run this method also if idToRemove is empty to retrieve currentlyUsedID 
//						channelsUsageList.get(i).removeAll(idToRemove);
//					}
//					tmpChannels.addAll(channelsUsageList.get(i));	//add to currentlyUsedID
//				}//else (get(0)==null&&size==1) --> skip
//			}
//		}
//		return tmpChannels;
//	}
	
//	private void addMissingWorkingChannels(List<Integer> currentlyUsedID ){
//		if(channelsUsageList.get(0).get(0)==null){
//			channelsUsageList.get(0).addAll(1,currentlyUsedID);
//			channelsUsageList.get(0).remove(0);
//		}else{
//			channelsUsageList.get(0).addAll(0,currentlyUsedID);
//		}
//		
//	}
	
	private synchronized DiceConsumer getBestChannel(){
		int bestChannelSize = Integer.MAX_VALUE;
		DiceConsumer dc = new DiceConsumer();
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			if(entry.getKey().isWorking()){
				if(entry.getValue().size()<bestChannelSize){
					bestChannelSize = entry.getValue().size();
					dc = entry.getKey();
				}
			}
		}
		
		if(bestChannelSize == Integer.MAX_VALUE){
			return null;
		}else{
			return dc;
		}
	}
	
	@Scheduled(fixedDelay = 300000, initialDelay = 5000)
	public void checkWSAvailability(){
		String res, message;
		message = new String();
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			DiceConsumer dc =  entry.getKey();
			try{
				res = restWrapper.getForObject(settings.getFullAddress() + dc.getPort() + "/state", String.class);
			}catch(Exception e){
				dc.setState(States.INTERRUPTED);
				message += dc.getPort()+": NOT working, ";
				continue;
			}
			if(res.equals("ERROR")){
				dc.setState(States.ERROR);
				message += dc.getPort()+": NOT working, ";
			}else{
				dc.setState(States.COMPLETED);
				message += dc.getPort()+": working, ";
			}
		}
		logger.info(message);
	}
	
//	public synchronized void updateBestChannel(int element){
//		int i=0, j = 0;
//		outerloop:
//		while(true){
//			j = 0;
//			int currListSize = channelsUsageList.get(i).size();
//			if(currListSize!=0){
//				while(j<currListSize){
//					if(channelsUsageList.get(i).get(j)!=null){
//						if(element == channelsUsageList.get(i).get(j)){
//							updateArr(i, j, i-1);
//							break outerloop;
//						}
//					}
//					j++;
//				}
//			}
//			i++;
//		}
//	}
	
	public synchronized void updateBestChannel(Integer id, InteractiveExperiment exp){
		DiceConsumer dc = getConsumer(id);
		
		
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			if(entry.getKey().equals(dc)){
				int prevSize = entry.getValue().size();
				entry.getValue().remove(exp);
				System.out.println("[DSA]"+prevSize+", "+entry.getValue().size());
			}
		}
		
		
		
		printStatus();
	}
	
//	private void updateArr(int i,int j, int newI){
//		int element = channelsUsageList.get(i).get(j);
//		
//		if(channelsUsageList.get(i).size() == 1){
//			channelsUsageList.get(i).set(j,null);
//		}else{
//			channelsUsageList.get(i).remove(j);
//		}
//		if(newI>channelsUsageList.size()-1){
//			channelsUsageList.add(newI,new ArrayList<Integer>());
//		}
//		channelsUsageList.get(newI).add(element);
//		if(channelsUsageList.get(newI).get(0)==null){
//			channelsUsageList.get(newI).remove(0);
//		}
//		printStatus();
//	}
	
	private DiceConsumer getConsumer(Integer id){
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			if(entry.getKey().getId()==id){
					return entry.getKey();
			}
		}
		return null;
	}
	
//	private void printStatus(){
//		String queueStatus ="[Q-STATUS]";
//		for(int i=0; i<channelsUsageList.size();i++){
//			queueStatus +=i+": "; 
//			for(int j=0;j<channelsUsageList.get(i).size();j++){
//				queueStatus += channelsUsageList.get(i).get(j);
//				if(j!=channelsUsageList.get(i).size()-1){
//					queueStatus +=","; 
//				}
//			}
//			queueStatus +=(" || ");
//		}
//		System.out.println(queueStatus);
//	}
	public Map<String,String> getWsStatus(){
		Map<String,String> status = new HashMap<String,String>();
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			status.put(entry.getKey().getPort(), entry.getKey().getState().toString());
		}
		return status;
	}
	
	private void printStatus(){
		String queueStatus ="[Q-STATUS] ";
		
		for (Map.Entry<DiceConsumer,ArrayList<InteractiveExperiment> > entry : consumerExperimentsMap.entrySet()) {
			queueStatus += entry.getKey().getPort()+": "+entry.getValue().size()+" || ";
		}
		System.out.println(queueStatus);
	}
}