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
package it.polimi.diceH2020.launcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class Validator {

    private final Logger logger = Logger.getLogger (getClass ());

    private ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());

    private  <T> Optional<T> objectFromPath (Path pathFile, Class<T> cls) {
        Optional<T> returnValue;
        try {
            String serialized = String.join ("\n", Files.readAllLines (pathFile));
            returnValue = Optional.of(mapper.readValue(serialized, cls));
        } catch (IOException e) {
            logger.debug ("Issue reading the JSON", e);
            returnValue = Optional.empty();
        }
        return returnValue;
    }

    public Optional<InstanceDataMultiProvider> readInstanceDataMultiProvider (Path pathToFile) {
        return objectFromPath (pathToFile, InstanceDataMultiProvider.class);
    }

}
