/*
Copyright 2017 Eugenio Gianniti
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
package it.polimi.diceH2020.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsManager;
import it.polimi.diceH2020.launcher.service.DiceConsumer;
import it.polimi.diceH2020.launcher.service.DiceService;
import it.polimi.diceH2020.launcher.service.RestCommunicationWrapper;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Scope("prototype")
@Component
public class Experiment {
    public static final List<String> EXTENSIONS =
            Arrays.asList (".txt", ".def", ".net", ".stat", ".jsimg");

    private final Logger logger = Logger.getLogger(getClass());

    private String EVENT_ENDPOINT;
    private String INPUT_DATA_ENDPOINT;
    private String RESULT_FOLDER;
    private String SOLUTION_ENDPOINT;
    private String STATE_ENDPOINT;
    private String UPLOAD_ENDPOINT;
    private String port;

    private DiceConsumer consumer;

    @Setter(onMethod = @__(@Autowired))
    private Settings settings;

    @Setter(onMethod = @__(@Autowired))
    private ObjectMapper mapper;

    @Setter(onMethod = @__(@Autowired))
    private DiceService diceService;

    @Setter(onMethod = @__(@Autowired))
    private RestCommunicationWrapper restWrapper;

    @Setter(onMethod = @__(@Autowired))
    private FileService fileService;

    public Experiment(DiceConsumer diceConsumer) {
        port = diceConsumer.getPort();
        consumer = diceConsumer;
    }

    @PostConstruct
    private void init() throws IOException {
        INPUT_DATA_ENDPOINT = settings.getFullAddress() + port  + "/inputdata";
        EVENT_ENDPOINT = settings.getFullAddress() + port + "/event";
        STATE_ENDPOINT = settings.getFullAddress() + port + "/state";
        UPLOAD_ENDPOINT = settings.getFullAddress() + port + "/upload";
        SOLUTION_ENDPOINT = settings.getFullAddress() + port + "/solution";
        Path result = Paths.get(settings.getResultDir());
        if (! Files.exists (result)) Files.createDirectory(result);
        RESULT_FOLDER = result.toAbsolutePath().toString();
    }

    private synchronized boolean sendFiles(@NotNull List<String> folders, @NotNull String extension,
                                           @NotNull String errorMessage) {
        boolean success = true;
        try {
            Iterator<Map<String, String>> files = fileService.getFiles (folders, extension).iterator ();
            while (success && files.hasNext ()) {
                Map<String, String> fileInfo = files.next ();
                success = send(fileInfo.get ("name"), cleanContent (fileInfo.get ("content")));
            }
        } catch (IOException e) {
            logger.error(errorMessage, e);
            success = false;
        }
        return success;
    }

    private boolean send (String filename, String content) {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("name", filename);
        map.add("filename", filename);

        try {
            ByteArrayResource contentsAsResource = new ByteArrayResource(content.getBytes("UTF-8")) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            map.add("file", contentsAsResource);

            makePostCall (UPLOAD_ENDPOINT, map, String.format ("sending '%s'", filename));
        } catch (UnsupportedEncodingException e) {
            logger.error (String.format ("error trying to send '%s'", filename), e);
            return false;
        }
        return true;
    }

    private <T> Optional<String> makePostCall (@NotNull String endpoint, @NotNull T object,
                                               @NotNull String doingWhat) {
        Optional<String> result = Optional.empty ();

        try {
            String response = restWrapper.postForObject (endpoint, object, String.class);
            result = Optional.ofNullable (response);
        } catch (ResourceAccessException e) {
            logger.error (String.format ("WS unreachable when %s", doingWhat), e);
            notifyUnreachableWs ();
        } catch (RestClientException e) {
            logger.error (String.format ("Unspecified exception while %s", doingWhat), e);
        }

        return result;
    }

    private <T> Optional<T> makeGetCall (@NotNull String endpoint, @NotNull Class<T> clazz,
                                         @NotNull String doingWhat) {
        Optional<T> result = Optional.empty ();

        try {
            T response = restWrapper.getForObject (endpoint, clazz);
            result = Optional.ofNullable (response);
        } catch (ResourceAccessException e) {
            logger.error (String.format ("WS unreachable when %s", doingWhat), e);
            notifyUnreachableWs ();
        } catch (RestClientException e) {
            logger.error (String.format ("Unspecified exception while %s", doingWhat), e);
        }

        return result;
    }

    private void notifyUnreachableWs () {
        diceService.setChannelState (consumer, States.INTERRUPTED);
        logger.info ("WS unreachable. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
    }

    private void notifyWsErrorState (String res) {
        if (res.equals ("ERROR")) {
            diceService.setChannelState (consumer, States.ERROR);
            logger.info("WS is in error state. (channel id: "+consumer.getId()+" port:"+consumer.getPort()+")");
        }
    }

    private String cleanContent (@NotNull String compressedContent) throws IOException {
        String content = Compressor.decompress (compressedContent);
        String[] writtenLines = content.split ("\\R+");
        return String.join ("\n", writtenLines);
    }

    private boolean checkWsTransition (@NotNull Events event, @NotNull String source, @NotNull String target) {
        Optional<String> maybeState = makePostCall (EVENT_ENDPOINT, event,
                String.format ("sending event: %s", event.toString ()));

        while (maybeState.isPresent () && source.equals (maybeState.get ())) {
            try {
                Thread.sleep (2000);
            } catch (InterruptedException e) {
                // no op
            }

            maybeState = makeGetCall (STATE_ENDPOINT, String.class, "getting WS state");
        }

        boolean success = maybeState.isPresent () && target.equals (maybeState.get ());

        if (! success) maybeState.ifPresent (this::notifyWsErrorState);

        return success;
    }

    private Optional<Solution> retrieveSolution () {
        return makeGetCall (SOLUTION_ENDPOINT, Solution.class, "fetching Solution");
    }

    public synchronized boolean launch(InteractiveExperiment e) {
        logger.trace("Experiment::launch");
        e.setState(States.RUNNING);
        e.getSimulationsManager().refreshState();
        diceService.updateManager(e.getSimulationsManager());

        String expInfo = String.format("|%s| ", Long.toString(e.getId()));
        String baseErrorString = expInfo+"Error! ";

        logger.info(String.format("%s-> {Exp:%s  port:%s,  provider:\"%s\" scenario:\"%s\"}",
                expInfo, Long.toString(e.getId()), port, e.getProvider(),
                e.getSimulationsManager().getScenario().toString()));
        logger.info(String.format("%s---------- Starting optimization ----------", expInfo));


        boolean idle = checkWSIdle();
        if (!idle) {
            logger.info(baseErrorString + " Service not idle");
            return false;
        }

        logger.info(String.format("%sAttempt to send .json",expInfo));
        boolean chargedInputData = sendInputData(e.getInputData());
        if (! chargedInputData) {
            logger.error (baseErrorString + " Sending input data");
            return false;
        }
        logger.info(String.format("%s.json has been correctly sent",expInfo));


        logger.info(String.format("%sAttempt to send JMT replayers files",expInfo));

        if (! initialize (e)) {
            logger.error (baseErrorString+" Problem with JMT replayers files");
            return false;
        }
        logger.info(expInfo+"JMT replayers files have been correctly sent");

        boolean chargedInitialSolution = generateInitialSolution();
        if (! chargedInitialSolution) {
            logger.error (baseErrorString + " Generation of the initial solution");
            return false;
        }
        logger.info(String.format("%s---------- Initial solution correctly generated",expInfo));

        boolean evaluatedInitialSolution = evaluateInitSolution();
        if (! evaluatedInitialSolution) {
            logger.error (baseErrorString + " Evaluating the initial solution");
            return false;
        }
        logger.info(String.format("%s---------- Initial solution correctly evaluated",expInfo));

        boolean savedInitialSolution = saveInitSolution();
        if (! savedInitialSolution) {
            logger.error (baseErrorString + " Getting or saving initial solution");
            return false;
        }
        logger.info(String.format("%s---------- Initial solution correctly saved",expInfo));


        logger.info(String.format("%s---------- Starting hill climbing", expInfo));
        boolean finish = executeLocalSearch();

        if (! finish) {
            logger.error (baseErrorString + " Local search");
            return false;
        }

        boolean savedFinalSolution = saveFinalSolution(e, expInfo);
        if (! savedFinalSolution) {
            logger.error (baseErrorString + " Getting or saving final solution");
            return false;
        }
        logger.info(String.format("%s---------- Finished hill climbing", expInfo));

        boolean backToIdle = checkWsTransition (Events.MIGRATE, "FINISHED", "IDLE");
        if (! backToIdle) {
            logger.error (baseErrorString + " WS did not become Idle after completing");
            return false;
        }
        logger.info(String.format("%s---------- Finished optimization ----------", expInfo));

        logger.debug("[LOCKS] Exp"+e.getId()+" on port: "+port+" completed");
        return true;
    }

    /**
     *  Wait until WS is in IDLE state.
     *  It has a fixed max wait time.
     *  Useful when WS has been stopped.
     */
    private boolean checkWSIdle() {
        Optional<String> maybeState = makeGetCall (STATE_ENDPOINT, String.class,
                "checking WS Idle state");
        return maybeState.map ("IDLE"::equals).orElse (false);
    }

    private boolean sendInputData (InstanceDataMultiProvider data) {
        boolean returnValue = false;

        if (data != null) {
            Optional<String> maybeState = makePostCall (INPUT_DATA_ENDPOINT, data, "sending input data");
            returnValue = maybeState.map (state -> {
                boolean success;

                if ("CHARGED_INPUTDATA".equals (state)) {
                    success = true;
                } else {
                    logger.error ("Error for experiment: " + data.getId () + " server responded in an unexpected way: " + state);
                    notifyWsErrorState (state);
                    success = false;
                }

                return success;
            }).orElse (false);
        } else {
            logger.error ("Error in one experiment, problem in input data serialization");
        }

        return returnValue;
    }

    private synchronized boolean initialize(InteractiveExperiment experiment) {
        SimulationsManager simManager = experiment.getSimulationsManager();
        List<String> inputFolders = simManager.getInputFolders ();
        boolean success = true;
        Iterator<String> it = EXTENSIONS.iterator ();

        while (success && it.hasNext ()) {
            String ext = it.next ();
            String errorMessage = String.format (
                    "Impossible launching SimulationsManager %s: error with %s files",
                    simManager.getId(), ext);
            success = sendFiles (inputFolders, ext, errorMessage);
        }

        return success;
    }

    private boolean generateInitialSolution() {
        return checkWsTransition (Events.TO_RUNNING_INIT, "RUNNING_INIT", "CHARGED_INITSOLUTION");
    }

    private boolean evaluateInitSolution() {
        return checkWsTransition (Events.TO_EVALUATING_INIT, "EVALUATING_INIT", "EVALUATED_INITSOLUTION");
    }

    private boolean saveInitSolution() {
        Optional<Solution> maybeSolution = retrieveSolution ();

        Optional<Boolean> result = maybeSolution.map (solution -> {
            String solFilePath = RESULT_FOLDER + File.separator + solution.getId () + "-MINLP.json";
            boolean success;
            try {
                String solSerialized = mapper.writeValueAsString (solution);
                Files.write (Paths.get (solFilePath), solSerialized.getBytes ());
                success = true;
            } catch (IOException e) {
                success = false;
            }
            return success;
        });

        return result.orElse (false);
    }

    private boolean executeLocalSearch() {
        return checkWsTransition (Events.TO_RUNNING_LS, "RUNNING_LS", "FINISH");
    }

    private boolean saveFinalSolution(InteractiveExperiment e, String expInfo) {
        Optional<Solution> maybeSolution = retrieveSolution ();

        maybeSolution.ifPresent (solution -> {
            e.setSol(solution);
            e.setExperimentalDuration (solution.getOptimizationTime());
            e.setDone (true);
            String msg = String.format ("%sHill Climbing result  -> %s", expInfo, solution.toStringReduced());
            logger.info (msg);
        });

        return maybeSolution.isPresent ();
    }
}
