/*
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.reactor;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.Settings;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobsDispatcher extends QueueHandler<InteractiveExperiment> {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JobsDispatcher.class.getName());

	@Autowired
	private RestCommunicationWrapper restWrapper;

	@Autowired
	private Settings settings;

	private AtomicInteger numPrivateConcurrentExp = new AtomicInteger(0);
	private int maxNumPrivateConcurrentExp;

	public int getQueueSize(){
		return jobsQueue.size();
	}

	@PostConstruct
	private void setupEnvironment(){
		maxNumPrivateConcurrentExp = settings.getPrivateConcurrentExperiments() ;
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
		logger.info("Cron Job executed. Queue length: "+getQueueSize()+" "+message.substring(0,message.length()-1));
		sendJobsToFreeChannels();
	}

	public Map<String,String> getWsStatus(){
		Map<String,String> status = new HashMap<String,String>();
		for(ChannelInfo channel : channelsInfoList){
			status.put(channel.getConsumer().getPort(), channel.getState().toString());
		}
		return status;
	}

	public int getNumPrivateExperiments(){
		return numPrivateConcurrentExp.get();
	}

	@Override
	protected int getJobToSend(){
		for(int nextJob=0; nextJob<jobsQueue.size();nextJob++){
			if(jobsQueue.get(nextJob).getSimulationsManager().getScenario().getCloudType().equals(CloudType.PRIVATE)){
				if(numPrivateConcurrentExp.get() < maxNumPrivateConcurrentExp){
					numPrivateConcurrentExp.incrementAndGet();
					return nextJob;
				}
			}else{
				return nextJob;
			}
		}
		return -1;
	}

	public void signalPrivateExperimentEnd(){
		numPrivateConcurrentExp.decrementAndGet();
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
