package it.polimi.diceH2020.launcher.service;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.model.SimulationsOptManager;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;


@Component
@Scope("prototype")
public class DiceConsumer implements Consumer<Event<SimulationsManager>>{

	//private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceConsumer.class.getName());

	@Autowired
	private EventBus eventBus;
	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
	@Autowired
	private SimulationsManagerRepository simManRepo;
	
	@Autowired
	private Experiment experiment;
	
	@Autowired
	private DiceService ds;
	
	private int id;
	
	public DiceConsumer(int num) {
		this.id = num;
	}
	
	@PostConstruct
	private void register(){
		
	    System.out.println("channel"+id);
		eventBus.on($("channel"+id), this); //registering the consumer
	}

	@PreDestroy
	private void customDestroy(){
		System.out.println("DELETED");		
	}
	
	
	@Override
	public void accept(Event<SimulationsManager> ev) {
		if(ev.getData() instanceof SimulationsWIManager){
			SimulationsWIManager simManager =(SimulationsWIManager) ev.getData();
			experiment.init(simManager);
			simManager.getExperimentsList().stream().forEach(e-> {
				experiment.launchWI(e);
				intExpRepo.saveAndFlush(e);
			});
			simManager.writeFinalResults();
			simManager.setState("completed");
			simManRepo.saveAndFlush(simManager);
		}
		if(ev.getData() instanceof SimulationsOptManager){
			SimulationsOptManager simManager =(SimulationsOptManager) ev.getData();
			experiment.init(simManager);
			simManager.getExperimentsList().stream().forEach(e-> {
				experiment.launchOpt(e);
				intExpRepo.saveAndFlush(e);
			});
			//simManager.writeFinalResults();
			simManager.setState("completed");
			simManRepo.saveAndFlush(simManager);
		}
	 ds.updateBestChannel(this.id);   	
	}
}
