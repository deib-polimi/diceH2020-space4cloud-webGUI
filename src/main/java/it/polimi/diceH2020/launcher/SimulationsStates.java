package it.polimi.diceH2020.launcher;

public enum SimulationsStates {
	READY,
	RUNNING,
	WARNING,
	ERROR,
	COMPLETED,
	INTERRUPTED
}
//usage in thymeleaf th:if="*{#strings.toString(manager.state)}==COMPLETED"