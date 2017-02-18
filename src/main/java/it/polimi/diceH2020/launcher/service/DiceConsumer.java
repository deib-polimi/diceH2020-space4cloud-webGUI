/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

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
package it.polimi.diceH2020.launcher.service;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import lombok.Data;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.annotation.PostConstruct;

import static reactor.bus.selector.Selectors.$;

@Component
@Scope("prototype")
@Data
public class DiceConsumer implements Consumer<Event<InteractiveExperiment>>{

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private EventBus eventBus;

	private Experiment experiment;

	@Autowired
	private DiceService ds;

	@Autowired
	private ApplicationContext context;

	private String port;

	private int id;


	public DiceConsumer(int num, String port) {
		this.id = num;
		this.port = port;
	}

	public DiceConsumer() {
	}

	@PostConstruct
	private void register(){
		logger.info("[LOCKS] channel"+id+"-->"+port);
		experiment = (Experiment) context.getBean("experiment",this);
		eventBus.on($("channel"+id), this); //registering the consumer
	}

	@Override
	public void accept(Event<InteractiveExperiment> ev) {
		InteractiveExperiment intExp = ev.getData();
		boolean executedCorrectly = false;

		logger.debug("[LOCKS] Exp"+intExp.getId()+" on thread"+id+" port"+port+" has been inserted in the queue");
		if(experiment.launch(intExp)){
			intExp.getSimulationsManager().setNumCompletedSimulations(intExp.getSimulationsManager().getNumCompletedSimulations()+1);
			if(intExp.isError()) intExp.setState(States.ERROR);
			else intExp.setState(States.COMPLETED);
			executedCorrectly = true;
		}else{
			intExp.getSimulationsManager().setNumFailedSimulations(intExp.getSimulationsManager().getNumFailedSimulations()+1);
			intExp.setState(States.ERROR);
		}
		if(intExp.getSimulationsManager().getScenario().getCloudType().equals(CloudType.Private)){
			ds.signalPrivateExperimentEnd();
		}
		intExp.getSimulationsManager().refreshState();
		ds.updateManager(intExp.getSimulationsManager());
		if(executedCorrectly) ds.getDispatcher ().notifyReadyChannel(this);
	}

}
