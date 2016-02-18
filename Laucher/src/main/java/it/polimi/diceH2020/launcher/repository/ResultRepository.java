package it.polimi.diceH2020.launcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.polimi.diceH2020.launcher.model.Results;

@Repository
public interface ResultRepository extends JpaRepository<Results, Long> {

}
