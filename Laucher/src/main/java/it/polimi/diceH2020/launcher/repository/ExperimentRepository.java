package it.polimi.diceH2020.launcher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.model.ExperimentRecord;


@Repository
public interface ExperimentRepository extends JpaRepository<ExperimentRecord, Long>{

	List<ExperimentRecord> findByDone(boolean done);

}
