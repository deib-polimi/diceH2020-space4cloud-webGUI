/*
Copyright 2016 Michele Ciavotta
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

import it.polimi.diceH2020.launcher.Settings;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

public abstract class QueueHandler<T>{

	private final Logger logger = Logger.getLogger(QueueHandler.class.getName());

	@Autowired
	private EventBus eventBus;

	@Autowired
	private ApplicationContext context;

	@Autowired
	protected Settings set;

	protected List<ChannelInfo> channelsInfoList;  //ChannelState local information about consumer's status.
	protected List<T> jobsQueue;

	@PostConstruct
	private void setUpEnvironment(){
		createChannels();
	}

	private void createChannels(){
		channelsInfoList = new ArrayList<ChannelInfo>();
		jobsQueue = new ArrayList<>();
		for(int i=0;i<set.getPorts().length;  i++){
			channelsInfoList.add( new ChannelInfo((DiceConsumer)context.getBean("diceConsumer",i,set.getPorts()[i])));
		}
	}

	public void enqueueJob(T job){ //TODOsynchronized??? for the Q
		jobsQueue.add(job);
		printStatus();
		sendJobsToFreeChannels();
	}

	public synchronized void sendJobsToFreeChannels(){
		channelsInfoList.stream().filter(channelInfo -> channelInfo.getState().equals(States.COMPLETED)).forEach(channelInfo->sendJob(channelInfo));
	}

	private synchronized void sendJob(ChannelInfo info){ //synch for the Q
		int nextJob = getJobToSend();
		if(!jobsQueue.isEmpty() && info.getState().equals(States.COMPLETED) && nextJob != -1 ){
			String channel = "channel"+ info.getConsumer().getId();
			info.setState(States.RUNNING);
			eventBus.notify(channel, Event.wrap(jobsQueue.remove(nextJob)));
			logger.debug("|Q-STATUS| job sent to " + channel );
		}
	}

	protected int getJobToSend(){
		int nextJob = 0;
		return nextJob;
	}

	public synchronized void notifyChannelStatus(DiceConsumer consumer, States state ){
		channelsInfoList.stream().filter(channelInfo -> channelInfo.getConsumer().equals(consumer)).findFirst().get().setState(state);
		printStatus();
	}

	protected void printStatus(){
		String queueStatus ="|Q-STATUS|";
		queueStatus += " (# jobs in Q: "+jobsQueue.size()+") ";
		for(int i=0; i<channelsInfoList.size();i++){
			queueStatus += i + ": ";
			queueStatus += channelsInfoList.get(i).getState();
			queueStatus += (" || ");
		}
		logger.debug(queueStatus+"\n");
	}

}
