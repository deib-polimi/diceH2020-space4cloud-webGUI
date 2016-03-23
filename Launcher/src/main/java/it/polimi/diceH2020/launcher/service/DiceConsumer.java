package it.polimi.diceH2020.launcher.service;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import lombok.Data;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;


@Component
@Scope("prototype")
@Data
public class DiceConsumer implements Consumer<Event<InteractiveExperiment>>{

	//private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceConsumer.class.getName());

	@Autowired
	private EventBus eventBus;
	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
	@Autowired
	private SimulationsManagerRepository simManRepo;
	
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
	    System.out.println("channel"+id+"-->"+port);
	    experiment = (Experiment) context.getBean("experiment",port);
		eventBus.on($("channel"+id), this); //registering the consumer
	}
	
	@Override
	public void accept(Event<InteractiveExperiment> ev) {
		InteractiveExperiment intExp = (InteractiveExperiment) ev.getData();
		System.out.println("Accepted a "+intExp.getSimType()+" on "+port);
		if(intExp.getSimType() == "WI"){
			//SimulationsWIManager simManager =(SimulationsWIManager) ev.getData();
			if(experiment.launchWI(intExp)){
				intExp.getSimulationsManager().setNumCompletedSimulations(intExp.getSimulationsManager().getNumCompletedSimulations()+1);
				intExp.setState("completed");
			}else{
				intExp.setState("error");
			}
			intExpRepo.save(intExp);
			System.out.println(intExp.getSimulationsManager().getSize()+" "+intExp.getSimulationsManager().getNumCompletedSimulations());
			if(intExp.getSimulationsManager().getNumCompletedSimulations() == intExp.getSimulationsManager().getSize() ){
				intExp.getSimulationsManager().writeFinalResults();
				intExp.getSimulationsManager().setState("completed");
				simManRepo.save(intExp.getSimulationsManager());
			}
			ds.updateBestChannel(this.id);  
		}
		else if(intExp.getSimType() == "Opt"){
			if(experiment.launchOpt(intExp)){
				intExp.getSimulationsManager().setNumCompletedSimulations(intExp.getSimulationsManager().getNumCompletedSimulations()+1);
				intExp.setState("completed");
			}else{
				intExp.setState("error");
			}
			intExpRepo.save(intExp);
			if(intExp.getSimulationsManager().getNumCompletedSimulations() == intExp.getSimulationsManager().getSize() ){
				//intExp.getSimulationsManager().writeFinalResults();
				intExp.getSimulationsManager().setState("completed");
				simManRepo.save(intExp.getSimulationsManager());
			}
			ds.updateBestChannel(this.id);  
		}
	}
}
