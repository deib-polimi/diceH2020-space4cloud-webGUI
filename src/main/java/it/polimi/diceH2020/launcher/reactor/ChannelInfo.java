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

