package it.polimi.diceH2020.launcher.reactor;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;

@Service
public class JobsDispatcher extends QueueHandler<InteractiveExperiment> {
	
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JobsDispatcher.class.getName());
	
	@Autowired
	private RestCommunicationWrapper restWrapper;
	
	public int getQueueSize(){
		return jobsQueue.size();
	}
	
	public synchronized void notifyReadyChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.COMPLETED);
		//System.out.println(channelsInfoList.stream().filter(channelInfo -> channelInfo.getConsumer().equals(consumer)).findFirst().get().getConsumer().getId() + " is ready");
		sendJobsToFreeChannels();
	}
	
	public synchronized void notifyErrorChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.ERROR);
	}
	
	public synchronized void notifyInterruptedChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.INTERRUPTED);
	}
	
	public synchronized void notifyChannelStatus(DiceConsumer consumer, States state ){
		channelsInfoList.stream().filter(channelInfo -> channelInfo.getConsumer().equals(consumer)).findFirst().get().setState(state);
		printStatus();
	}
	
	@Scheduled(fixedDelay = 600000, initialDelay = 5000)
	public void checkWSAvailability(){
		String res, message;
		message = res = new String();
		for(ChannelInfo channel : channelsInfoList){
			DiceConsumer dc = channel.getConsumer();
			try{
				res = restWrapper.getForObject(set.getFullAddress() + dc.getPort() + "/state", String.class);
			}catch(Exception e){
				channel.setState(States.INTERRUPTED);
				message += dc.getPort()+": NOT working, ";
				continue;
			}
			if(res.equals("ERROR")){
				channel.setState(States.ERROR);
				message += dc.getPort()+": NOT working, ";
			}else{
				channel.setState(States.COMPLETED);
				message += dc.getPort()+": working, ";
			}
		};
		logger.info(message);
		sendJobsToFreeChannels();
	}
	
	public Map<String,String> getWsStatus(){
		Map<String,String> status = new HashMap<String,String>();
		for(ChannelInfo channel : channelsInfoList){
			status.put(channel.getConsumer().getPort(), channel.getState().toString());
		}
		return status;
	}
	
	
	public synchronized void dequeue(){ 
//		 for (Iterator<InteractiveExperiment> iterator = jobsQueue.iterator(); iterator.hasNext();) {
//			 	InteractiveExperiment exp = iterator.next();
//				if(exp.isDone()){
//					iterator.remove();
//		 		}
//		 }
	}
	
}
