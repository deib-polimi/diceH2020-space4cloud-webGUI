package it.polimi.diceH2020.launcher.service;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import lombok.Data;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;


@Component
@Scope("prototype")
@Data
public class DiceConsumer implements Consumer<Event<InteractiveExperiment>>{

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
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
	
		logger.info("[LOCKS] Exp"+intExp.getId()+" on thread"+id+" port"+port+" has been inserted in the queue");
		if(experiment.launchOpt(intExp)){
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
		if(executedCorrectly) ds.dispatcher.notifyReadyChannel(this); 
	}
	
}
