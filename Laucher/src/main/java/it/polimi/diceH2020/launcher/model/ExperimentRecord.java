package it.polimi.diceH2020.launcher.model;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Entity
@Data
public class ExperimentRecord {
	@Id
	@GeneratedValue
	private long myId;
	@NotNull
	private String  instanceName;
	@NotNull
	private int iteration;
	@NotNull
	private boolean done;

	
	public ExperimentRecord(Path name, int iter){
		this.instanceName = name.toString();
		this.iteration = iter;
		this.done = false;
	}
	
	public Path getInstanceName(){
		return Paths.get(this.instanceName);
	}
	
	public String getShortName(){
		Path p = Paths.get(instanceName);
		return p.getName(p.getNameCount() - 1).toString();
	}
	public ExperimentRecord(){}
	
	
}
