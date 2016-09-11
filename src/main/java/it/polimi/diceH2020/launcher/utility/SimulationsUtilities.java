/*
Copyright 2016 Jacopo Rigoli

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
package it.polimi.diceH2020.launcher.utility;

import it.polimi.diceH2020.launcher.States;

import java.util.List;

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
