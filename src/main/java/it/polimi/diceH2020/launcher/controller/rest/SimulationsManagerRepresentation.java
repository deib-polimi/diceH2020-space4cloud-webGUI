/*
Copyright 2017 Eugenio Gianniti

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
package it.polimi.diceH2020.launcher.controller.rest;

import it.polimi.diceH2020.launcher.model.SimulationsManager;
import lombok.Getter;
import org.springframework.hateoas.ResourceSupport;

@Getter
class SimulationsManagerRepresentation extends ResourceSupport {

    private String instance;
    private String scenario;
    private String date;
    private String time;

    SimulationsManagerRepresentation(SimulationsManager manager) {
        instance = manager.getInstanceName ();
        scenario = manager.getScenario ().toString ();
        date = manager.getDate ();
        time = manager.getTime ();
    }
}
