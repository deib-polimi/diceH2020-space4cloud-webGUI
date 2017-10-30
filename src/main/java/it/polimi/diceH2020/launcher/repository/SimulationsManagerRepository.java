/*
Copyright 2017 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.repository;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SimulationsManagerRepository extends JpaRepository<SimulationsManager, Long>{

	//@Query("select s from SimulationsManager s where s.id in ( select a.id from ( select min (u.id) id from Simulations_Manager u group by u.folder ) a order by a.id ASC )")
	@Query("SELECT MIN(id) from SimulationsManager where scenario.cloudType = PUBLIC GROUP BY folder ") //?1
	List<Long> findPublicSimManGroupedByFolders();

	@Query("SELECT MIN(id) from SimulationsManager where scenario.cloudType = PRIVATE GROUP BY folder ") //?1
	List<Long> findPrivateSimManGroupedByFolders();

	int countByFolder(String folder);

	List<SimulationsManager> findByIdInOrderByIdAsc(List<Long> SIM_MANAGERList);

	List<SimulationsManager> findByFolderOrderByIdAsc(String folder);

	@Query("SELECT state from SimulationsManager where folder = ?1") //?1
	List<States> findStatesByFolder(String folder);

	SimulationsManager findById(Long id);

	List<SimulationsManager> findByState(States state);

	@Transactional
	void deleteByFolder(String folder);
}
