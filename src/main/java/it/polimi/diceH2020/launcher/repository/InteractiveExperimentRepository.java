package it.polimi.diceH2020.launcher.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;

@Repository
public interface InteractiveExperimentRepository extends JpaRepository<InteractiveExperiment, Long> {
	public List<InteractiveExperiment> findBySimulationsManager(SimulationsManager simManager);
	
	public List<InteractiveExperiment> findByState(States state);
	
	public InteractiveExperiment findById(Long id);
	
	
}
