package it.polimi.diceH2020.launcher.service;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.SimulationsStates;
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
	
	@PostConstruct
	private void register(){
	    logger.info("[LOCKS] channel"+id+"-->"+port);
	    experiment = (Experiment) context.getBean("experiment",port);
		eventBus.on($("channel"+id), this); //registering the consumer
	}
	
	@Override
	public void accept(Event<InteractiveExperiment> ev) {
		InteractiveExperiment intExp = ev.getData();
	
		logger.info("[LOCKS] Exp"+intExp.getId()+" on thread"+id+"port: "+port+" has been inserted in the queue");
		if(intExp.getSimType().equals("WI")){
			if(experiment.launchWI(intExp)){
				intExp.getSimulationsManager().setNumCompletedSimulations(intExp.getSimulationsManager().getNumCompletedSimulations()+1);
				intExp.setState(SimulationsStates.COMPLETED);
			}else{
				intExp.getSimulationsManager().setNumFailedSimulations(intExp.getSimulationsManager().getNumFailedSimulations()+1);
				intExp.setState(SimulationsStates.ERROR);
			}
		}
		else if(intExp.getSimType().equals("Opt")){
			if(experiment.launchOpt(intExp)){
				intExp.getSimulationsManager().setNumCompletedSimulations(intExp.getSimulationsManager().getNumCompletedSimulations()+1);
				intExp.setState(SimulationsStates.COMPLETED);
			}else{
				intExp.getSimulationsManager().setNumFailedSimulations(intExp.getSimulationsManager().getNumFailedSimulations()+1);
				intExp.setState(SimulationsStates.ERROR);
			}
		}else{
			intExp.setState(SimulationsStates.ERROR);
			logger.info("Error for experiment"+intExp.getId()+", wrong type. It will not be launched.");
			return;
		}
		
//		if(intExp.getSimulationsManager().getNumFailedSimulations()+intExp.getSimulationsManager().getNumCompletedSimulations()==intExp.getSimulationsManager().getInputFiles().size()){
//			if(intExp.getSimulationsManager().getNumFailedSimulations()>0){
//				intExp.getSimulationsManager().setState(SimulationsStates.ERROR);
//			}else{
//				intExp.getSimulationsManager().writeFinalResults();
//				intExp.getSimulationsManager().setState(SimulationsStates.COMPLETED);
//			}
//		}else{
//			if(intExp.getSimulationsManager().getNumFailedSimulations()>0){
//				intExp.getSimulationsManager().setState(SimulationsStates.WARNING);
//			}else{
//				intExp.getSimulationsManager().setState(SimulationsStates.RUNNING);
//			}
//		}
		intExp.getSimulationsManager().refreshState();
		//ds.updateExp(intExp);
		ds.updateManager(intExp.getSimulationsManager());
		ds.updateBestChannel(this.id);  
	}
}
