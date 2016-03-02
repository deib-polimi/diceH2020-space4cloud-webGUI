package it.polimi.diceH2020.launcher.service;

import static reactor.bus.selector.Selectors.$;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;


 
@Service
public class DiceConsumer implements Consumer<Event<SimulationsManager>>{

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DiceConsumer.class.getName());

	@Autowired
	private EventBus eventBus;
	@Autowired
	private InteractiveExperimentRepository intExpRepo;
	
	@Autowired
	private SimulationsManagerRepository simManRepo;
	
	@Autowired
	private Experiment experiment;
	
	@PostConstruct
	private void register(){
		eventBus.on($("evaluate"), this); //registering the consumer
	}

	@Override
	public void accept(Event<SimulationsManager> ev) {
		SimulationsManager simManager = ev.getData();
		experiment.init(simManager);
		simManager.getClassList().stream().forEach(e-> {
			experiment.launch(e);
			intExpRepo.saveAndFlush(e);
		});
		try {
			simManager.writeResultOnExcel();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		simManager.setState("completed");
		simManRepo.saveAndFlush(simManager);
		
	}

}
