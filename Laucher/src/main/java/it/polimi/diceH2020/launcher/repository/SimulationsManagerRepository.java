package it.polimi.diceH2020.launcher.repository;



import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.model.SimulationsManager;

@Repository
public interface SimulationsManagerRepository extends CrudRepository<SimulationsManager, Long>{
}

