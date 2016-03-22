package it.polimi.diceH2020.launcher.service;
 

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.Settings;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
//import it.polimi.diceH2020.launcher.utility.ExcelWriter;
import reactor.bus.Event;
import reactor.bus.EventBus;


@Service
public class DiceService {
 	//private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceService.class.getName());
	
	private List<ArrayList<Integer>> channelsUsageList; 
	private List<DiceConsumer> consumersList; 
	
 	@Autowired
 	private EventBus eventBus;
	
	@Autowired
	private SimulationsManagerRepository simManagerRepo;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private Settings settings;

	public void simulation(SimulationsManager simManager){
		simManagerRepo.saveAndFlush(simManager);
		String channel = "channel"+getBestChannel();
		System.out.println("Notify on "+channel);
		eventBus.notify(channel, Event.wrap(simManager));
	}
	
	public void updateSimManager(SimulationsManager simManager){
		simManagerRepo.saveAndFlush(simManager);
	}
	
	@PostConstruct
	private void setUpChannels(){
		createChannels();
	}
	
	public void createChannels(){
		channelsUsageList = new ArrayList<ArrayList<Integer>>();
		consumersList = new ArrayList<DiceConsumer>();
		
		channelsUsageList.add(new ArrayList<Integer>());
		for(int i=settings.getPorts().length-1; i >= 0; i--){
			DiceConsumer consumer = (DiceConsumer) context.getBean("diceConsumer",i,settings.getPorts()[i]);
			//consumer.register(i);
			consumersList.add(0,consumer);
			channelsUsageList.get(0).add(0,i);
		}
		
	}
	
	private synchronized int getBestChannel(){
		int bestChannel;
		int i=0;
		while(true){
			int currListSize = channelsUsageList.get(i).size();
			if(currListSize!=0){
				if(channelsUsageList.get(i).get(0)!=null||currListSize != 1){
					bestChannel = channelsUsageList.get(i).get(0);
					updateArr(i, 0, i+1);
					break;
				}//else (get(0)==null&&size==1) --> skip
			}
			i++;
		}
		return bestChannel;
	}
	
	public synchronized void updateBestChannel(int element){
		int i=0, j = 0;
		outerloop:
		while(true){
			j = 0;
			int currListSize = channelsUsageList.get(i).size();
			if(currListSize!=0){
				while(j<currListSize){
					if(channelsUsageList.get(i).get(j)!=null){
						if(element == channelsUsageList.get(i).get(j)){
							updateArr(i, j, i-1);
							break outerloop;
						}
					}
					j++;
				}
			}
			i++;
		}
	}
	
	private void updateArr(int i,int j, int newI){
		int element = channelsUsageList.get(i).get(j);
		
		if(channelsUsageList.get(i).size() == 1){
			channelsUsageList.get(i).set(j,null);
		}else{
			channelsUsageList.get(i).remove(j);
		}
		if(newI>channelsUsageList.size()-1){
			channelsUsageList.add(newI,new ArrayList<Integer>());
		}
		channelsUsageList.get(newI).add(element);
		if(channelsUsageList.get(newI).get(0)==null){
			channelsUsageList.get(newI).remove(0);
		}
		printStatus();
	}
	
	
	private void printStatus(){
		for(int i=0; i<channelsUsageList.size();i++){
			for(int j=0;j<channelsUsageList.get(i).size();j++){
				System.out.print(channelsUsageList.get(i).get(j));
				if(j!=channelsUsageList.get(i).size()-1){
					System.out.print(", ");
				}
			}
			System.out.print("\n");
		}
	}
	
	
}