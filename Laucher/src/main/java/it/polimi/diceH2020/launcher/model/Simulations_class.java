package it.polimi.diceH2020.launcher.model;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Currently we have only 2 class 
 * But if it will be supported by the simulator this WS architecture (except DiceServiceImpl) is pretty modular and can
 * handle a multiple class set of simulations
 */
public class Simulations_class {
	
	private  Integer id;              //number of Maps
	
	@NotNull
	@Min(1)
	private  Integer map;              //number of Maps
	@NotNull
	@Min(1)
	private  Integer reduce;           //number of Reduce
	
	private  double thinkRate;     // 1/Z
	
	@NotNull
	@Min(1)
	private  Integer thinkTime;     // 1/Z
	@NotNull
	@Min(1)
	private  Integer minNumUsers;	
	@NotNull
	@Min(1)
	private  Integer minNumVMs;	
	@NotNull
	@Min(1)
	private  Integer maxNumUsers;	
	@NotNull
	@Min(1)
	private  Integer maxNumVMs;	 
   	
	private  Integer class_number; 	
	
	private String tabID; //for session navigation used in the sequence of forms in V7. See the controller doc for other information
	
	  public String getTabID() {
		return tabID;
	}

	public void setTabID(String tabID) {
		this.tabID = tabID;
	}
	
	public  Integer getId() {
		return id;
	}

	public  void setId(Integer id) {
		this.id = id;
	}

	public Integer getClass_number() {
		return class_number;
	}

	public void setClass_number(Integer class_number) {
		this.class_number = class_number;
	}


	public Simulations_class(){
	}
	
	public Integer getMap() {
		return map;
	}

	public void setMap(Integer map) {
		this.map = map;
	}

	public Integer getReduce() {
		return reduce;
	}

	public void setReduce(Integer reduce) {
		this.reduce = reduce;
	}

	public double getThinkRate() {
		return thinkRate;
	}

	public void setThinkRate(double thinkRate) {
		this.thinkRate = thinkRate;
	}


	public Integer getThinkTime() {
		return thinkTime;
	}

	public void setThinkTime(Integer thinkTime) {
		this.thinkTime = thinkTime;
	}

	public Integer getMinNumUsers() {
		return minNumUsers;
	}

	public void setMinNumUsers(Integer minNumUsers) {
		this.minNumUsers = minNumUsers;
	}

	public Integer getMinNumVMs() {
		return minNumVMs;
	}

	public void setMinNumVMs(Integer minNumCores) {
		this.minNumVMs = minNumCores;
	}

	public Integer getMaxNumUsers() {
		return maxNumUsers;
	}

	public void setMaxNumUsers(Integer maxNumUsers) {
		this.maxNumUsers = maxNumUsers;
	}

	public Integer getMaxNumCores() {
		return maxNumVMs;
	}

	public void setMaxNumCores(Integer maxNumCores) {
		this.maxNumVMs = maxNumCores;
	}

}
