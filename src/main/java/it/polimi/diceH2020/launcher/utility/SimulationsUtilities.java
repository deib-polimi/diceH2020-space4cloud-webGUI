package it.polimi.diceH2020.launcher.utility;

import java.util.List;

import it.polimi.diceH2020.launcher.States;



public class SimulationsUtilities {
	
	public static States getStateFromList(List<States> statesList){
		int completed = 0;
		int error = 0;
		int running = 0;
		int interrupted = 0;
		for(States state : statesList){
			if(state.equals(States.COMPLETED)){
				completed++;
			}
			if(state.equals(States.ERROR)){
				error++;
			}
			if(state.equals(States.RUNNING)){
				running++;
			}
			if(state.equals(States.INTERRUPTED)){
				interrupted++;
			}
		}
		if(interrupted == statesList.size()) return States.INTERRUPTED;
		if(error > 0){
			if(running == 0){ // error+completed == expSize
				return States.ERROR;
			}else{
				return States.WARNING;
			}
		}else{
			if(running == 0){
				if(completed == statesList.size()){// completed == expSize
					return States.COMPLETED;
				}else{
					return States.READY;
				}
			}else{
				return States.RUNNING;
			}
		}
	}
}
