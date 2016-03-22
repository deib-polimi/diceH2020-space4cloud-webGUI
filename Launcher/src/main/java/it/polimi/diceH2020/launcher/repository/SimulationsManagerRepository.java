package it.polimi.diceH2020.launcher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.model.SimulationsManager;

@Repository
public interface SimulationsManagerRepository extends JpaRepository<SimulationsManager, Long>{
	public List<SimulationsManager> findByType(String type);
}

