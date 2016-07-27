package it.polimi.diceH2020.launcher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.SimulationsManager;

@Repository
public interface SimulationsManagerRepository extends JpaRepository<SimulationsManager, Long>{

	//@Query("select s from SimulationsManager s where s.id in ( select a.id from ( select min (u.id) id from Simulations_Manager u group by u.folder ) a order by a.id ASC )")
		@Query("SELECT MIN(id) from SimulationsManager where scenario = ?1 or scenario = ?2 GROUP BY folder ") //?1
		public List<Long> findPublicSimManGroupedByFolders(Scenarios s1,Scenarios s2);
		
		@Query("SELECT MIN(id) from SimulationsManager where scenario = ?1 or scenario = ?2  or scenario = ?3 GROUP BY folder ") //?1
		public List<Long> findPrivateSimManGroupedByFolders(Scenarios s1,Scenarios s2,Scenarios s3);
		
		public int countByFolder(String folder);
		
		public List<SimulationsManager> findByIdInOrderByIdAsc(List<Long> SIM_MANAGERList);
		
		public List<SimulationsManager> findByFolderOrderByIdAsc(String folder);
		
		@Query("SELECT state from SimulationsManager where folder = ?1") //?1
		public List<States> findStatesByFolder(String folder);
		
		public SimulationsManager findById(Long id);
		
		@Transactional
		public void deleteByFolder(String folder);
}

