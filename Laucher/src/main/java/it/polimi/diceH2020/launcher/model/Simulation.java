package it.polimi.diceH2020.launcher.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * 
 * single simulation that will be persisted in a transient DB
 */
@Entity
public class Simulation {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;
	private int counter;
	
	private int[] map;           //number of Maps
	private int[] reduce;        //number of Reduce
	
	private double[] mapRate;       // 1/(Avg duration of a map)
	private double[] reduceRate;    // 1/(Avg duration of a reduce)
	private double[] thinkRate;   	// 1/Z
	private int[] mapTime;
	private int[] reduceTime;
	private int[] thinkTime;
	
	private double[] throughput;
	private double throughputEnd;  //X
	
	private int[] responseTime;//R
	private int responseTimeEnd;//R
	
	private double runtime;
	
	private int[] cores;//num cores
	private int[] users;//num jobs/users
	
	private int accuracy;
	private Boolean erlang;
	private int minBatch;
	private int maxBatch;
	
	private String state = "ready"; //states:  ready to be executed, running, completed, failed
	
	public double[] getThinkRate() {
		return thinkRate;
	}
	public void setThinkRate(double[] thinkRate) {
		this.thinkRate = thinkRate;
	}
	public double[] getThroughput() {
		return throughput;
	}
	public void setThroughput(double[] throughput) {
		this.throughput = throughput;
	}
	public double getRuntime() {
		return runtime;
	}
	public void setRuntime(double runtime) {
		this.runtime = runtime;
	}
	public int[] getResponseTime() {
		return responseTime;
	}
	public int[] getCores() {
		return cores;
	}
	public void setCores(int[] cores) {
		this.cores = cores;
	}
	public int[] getUsers() {
		return users;
	}
	public void setUsers(int[] users) {
		this.users = users;
	}
	public double[] getMapRate() {
		return mapRate;
	}
	public void setMapRate(double[] mapRate) {
		this.mapRate = mapRate;
	}
	public double[] getReduceRate() {
		return reduceRate;
	}
	public void setReduceRate(double[] reduceRate) {
		this.reduceRate = reduceRate;
	}
	public int[] getMapTime() {
		return mapTime;
	}
	public void setMapTime(int[] mapTime) {
		this.mapTime = mapTime;
	}
	public int[] getReduceTime() {
		return reduceTime;
	}
	public void setReduceTime(int[] reduceTime) {
		this.reduceTime = reduceTime;
	}
	public int[] getThinkTime() {
		return thinkTime;
	}
	public void setThinkTime(int[] thinkTime) {
		this.thinkTime = thinkTime;
	}
	public int getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(int accuracy) {
		this.accuracy = accuracy;
	}
	public Boolean getErlang() {
		return erlang;
	}
	public void setErlang(Boolean erlang) {
		this.erlang = erlang;
	}
	public int[] getMap() {
		return map;
	}
	public void setMap(int[] map) {
		this.map = map;
	}
	public int[] getReduce() {
		return reduce;
	}
	public void setReduce(int[] reduce) {
		this.reduce = reduce;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public double getThroughputEnd() {
		return throughputEnd;
	}
	public void setThroughputEnd(double throughputEnd) {
		this.throughputEnd = throughputEnd;
	}
	public int getResponseTimeEnd() {
		return responseTimeEnd;
	}
	public void setResponseTimeEnd(int responseTimeEnd) {
		this.responseTimeEnd = responseTimeEnd;
	}
	
	public int getMinBatch() {
		return minBatch;
	}
	public void setMinBatch(int minBatch) {
		this.minBatch = minBatch;
	}
	public int getMaxBatch() {
		return maxBatch;
	}
	public void setMaxBatch(int maxBatch) {
		this.maxBatch = maxBatch;
	}
	public int getCounter() {
		return counter;
	}
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public SimulationsManager getSimulationsManager() {
		return simulationsManager;
	}
	public void setSimulationsManager(SimulationsManager simulationsManager) {
		this.simulationsManager = simulationsManager;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	/*
	 * TODO 
	 * caso 2classi <-- multiclasse
	 * responseTimeEnd = (numUsersClass1+NumUsersClass2)/throughputEnd - (thinkTime1*throughputJoin1+thinkTime2*throughputJoin2)/(throughputJoin1+throughputJoin2);
	 */
	public void setResponseTime(){
		if(users.length>1){
			this.responseTimeEnd = (int)((users[0]+users[1])/throughputEnd-((1/thinkRate[0])*throughput[0]+(1/thinkRate[1])*throughput[1])/(throughput[0]+throughput[1]));
			int[] a= new int[users.length]; 
			a[0]=(int)(users[0]/throughput[0]-1/thinkRate[0]);
			a[1]=(int)(users[1]/throughput[1]-1/thinkRate[1]);
			this.responseTime = a.clone();
		}else{
			this.responseTimeEnd = (int) (users[0]/throughputEnd-1/thinkRate[0]);
		}
		
	}
	
}
