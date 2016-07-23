package it.polimi.diceH2020.launcher;


public enum States {
	READY,
	RUNNING,
	WARNING,
	ERROR,
	COMPLETED,
	INTERRUPTED
}
//usage in thymeleaf th:if="*{#strings.toString(manager.state)}==COMPLETED"