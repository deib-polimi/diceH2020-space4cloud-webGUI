package it.polimi.diceH2020.launcher.repository;

import org.springframework.data.repository.CrudRepository;

import it.polimi.diceH2020.launcher.model.Simulation;
import it.polimi.diceH2020.launcher.model.SimulationsManager;


public interface SimulationsRepository extends CrudRepository<Simulation, Long> {
	Iterable<Simulation> findBySimulationsManager(SimulationsManager simulationsManager);
}
