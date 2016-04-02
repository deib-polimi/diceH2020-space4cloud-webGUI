package it.polimi.diceH2020.launcher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.model.SimulationsManager;

@Repository
public interface SimulationsManagerRepository extends JpaRepository<SimulationsManager, Long>{
		public List<SimulationsManager> findByType(String type);
		//@Query("select s from SimulationsManager s where s.id in ( select a.id from ( select min (u.id) id from Simulations_Manager u group by u.folder ) a order by a.id ASC )")
		@Query("SELECT MIN(u.id) from SimulationsManager u where u.type = 'Opt' GROUP BY u.folder ") //?1
		public List<Long> findSimManagerGroupedByFolders();
		
		public List<SimulationsManager> findByIdIn(List<Long> SIM_MANAGERList);
		
		public List<SimulationsManager> findByFolder(String folder);
		
		public SimulationsManager findById(Long id);
}

