package it.polimi.diceH2020.launcher.reactor;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JobsDispatcher extends QueueHandler<InteractiveExperiment> {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JobsDispatcher.class.getName());

	@Autowired
	private RestCommunicationWrapper restWrapper;

	public int getQueueSize(){
		return jobsQueue.size();
	}

	public void notifyReadyChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.COMPLETED);
		//System.out.println(channelsInfoList.stream().filter(channelInfo -> channelInfo.getConsumer().equals(consumer)).findFirst().get().getConsumer().getId() + " is ready");
		sendJobsToFreeChannels();
	}

	public void notifyErrorChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.ERROR);
	}

	public void notifyInterruptedChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.INTERRUPTED);
	}

	public void notifyRunningChannel(DiceConsumer consumer){
		notifyChannelStatus(consumer,States.RUNNING);
	}

	@Scheduled(fixedDelay = 600000, initialDelay = 5000)
	public void checkWSAvailability(){
		String res, message;
		message = res = new String();
		for(ChannelInfo channel : channelsInfoList){
			DiceConsumer dc = channel.getConsumer();
			States channelOldState = channel.getState();
			try{
				res = restWrapper.getForObject(set.getFullAddress() + dc.getPort() + "/state", String.class);
			}catch(Exception e){
				notifyInterruptedChannel(dc);
				message += dc.getPort()+": NOT working, ";
				continue;
			}
			if(res.equals("ERROR")){
				notifyErrorChannel(dc);
				message += dc.getPort()+": NOT working, ";
			}else{
				if(!channelOldState.equals(States.COMPLETED)&&!channelOldState.equals(States.RUNNING)) channel.setState(States.COMPLETED);
				message += dc.getPort()+":"+channel.getState()+",";
			}
		};
		logger.info( message.substring(0,message.length()-2));
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
