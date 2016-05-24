package it.polimi.diceH2020.launcher.reactor;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.service.DiceConsumer;

public class ChannelInfo{

	private DiceConsumer consumer;
	private States state;
	
	
	public ChannelInfo(DiceConsumer consumer){
		this.consumer = consumer;
		this.state = States.COMPLETED; //COMPLETED, ERROR, INTERRUPTED
	}
	
	public boolean isWorking(){
		if(state.equals(States.COMPLETED)){
			return true;
		}
		return false;
	}
	
	public States getState(){
		return state;
	}
	
	public void setState(States state){
		this.state = state;
	}
	
	public DiceConsumer getConsumer(){
		return consumer;
	}
}

