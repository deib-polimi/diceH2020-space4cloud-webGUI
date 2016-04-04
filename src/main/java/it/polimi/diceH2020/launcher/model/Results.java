package it.polimi.diceH2020.launcher.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
@Data
public class Results {
    @Id
    private long id;
    private int iteration;
    private String instanceName;

    @Column(length = 1000)
    @NotNull
    private String solution="";

}
