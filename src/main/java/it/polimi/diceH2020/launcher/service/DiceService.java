/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Michele Ciavotta

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
package it.polimi.diceH2020.launcher.service;

import it.polimi.diceH2020.launcher.States;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.PendingSubmission;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.reactor.JobsDispatcher;
import it.polimi.diceH2020.launcher.repository.InteractiveExperimentRepository;
import it.polimi.diceH2020.launcher.repository.PendingSubmissionRepository;
import it.polimi.diceH2020.launcher.repository.SimulationsManagerRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DiceService{
	private final Logger logger = Logger.getLogger (getClass ());

	@Setter(onMethod = @__(@Autowired))
	private SimulationsManagerRepository simManagerRepo;

	@Getter(AccessLevel.PACKAGE)
	@Setter(onMethod = @__(@Autowired))
	private JobsDispatcher dispatcher;

	@Setter(onMethod = @__(@Autowired))
	private InteractiveExperimentRepository intExpRepo;

	@Setter(onMethod = @__(@Autowired))
	private PendingSubmissionRepository pendingSubmissionRepository;

	public synchronized void simulation(SimulationsManager simManager) {
		updateManager(simManager);
		simManager.getExperimentsList().forEach(e -> dispatcher.enqueueJob(e));
	}

	public synchronized void simulation(InteractiveExperiment exp) {
		dispatcher.enqueueJob(exp);
	}

	private synchronized void updateExp(InteractiveExperiment intExp) {
		long startTime = System.currentTimeMillis();
		intExpRepo.saveAndFlush(intExp);
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		logger.debug("[LOCKS] Exp"+intExp.getId()+"updated[state: "+intExp.getState()+"] in "+duration+" ");
	}

	public synchronized void updateManager(SimulationsManager simulationsManager) {
		simManagerRepo.saveAndFlush(simulationsManager);
		logger.debug("[LOCKS] SimManager"+simulationsManager.getId()+" has been updated.");
	}

	public synchronized void updateSubmission(PendingSubmission submission) {
		pendingSubmissionRepository.saveAndFlush (submission);
		logger.debug(String.format ("[LOCKS] Pending submission %d has been updated.", submission.getId()));
	}

	@PostConstruct
	private void setUpEnvironment() {
		fixRunningSimulations();
	}

	private void fixRunningSimulations() {
		List<InteractiveExperiment> previouslyRunningExperiments = intExpRepo.findByStateOrderByIdAsc(States.RUNNING);
		List<InteractiveExperiment> previouslyReadyExperiments = intExpRepo.findByStateOrderByIdAsc(States.READY);//jobs in Q

		List<Long> managersToRefresh = new ArrayList<>();

		previouslyRunningExperiments.addAll(previouslyReadyExperiments);

		for (InteractiveExperiment previouslyRunningExperiment : previouslyRunningExperiments) {
			previouslyRunningExperiment.setState (States.ERROR);
			Long smID = previouslyRunningExperiment.getSimulationsManager ().getId ();
			updateExp (previouslyRunningExperiment);
			if (!managersToRefresh.contains (smID)) {
				managersToRefresh.add (smID);
			}
		}

		for (Long aManagersToRefresh : managersToRefresh) {
			SimulationsManager sm = simManagerRepo.findById (aManagersToRefresh);
			sm.setState (States.INTERRUPTED);
			updateManager (sm);
		}
	}

	public Map<String,String> getWsStatus() {
		return dispatcher.getWsStatus();
	}

	public int getQueueSize() {
		return dispatcher.getQueueSize();
	}

	public int getPrivateQueueSize() {
		return dispatcher.getNumPrivateExperiments();
	}

	void signalPrivateExperimentEnd() {
		dispatcher.signalPrivateExperimentEnd();
	}

	public void setChannelState(DiceConsumer consumer, States state) {
		dispatcher.notifyChannelStatus(consumer, state);
	}
}
