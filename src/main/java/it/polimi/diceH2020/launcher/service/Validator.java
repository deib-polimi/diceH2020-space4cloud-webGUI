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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class Validator {

	private ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());

	public <T> Optional<T> objectFromPath(Path pathFile, Class<T> cls) {

		String serialized;
		try {
			serialized = new String(Files.readAllBytes(pathFile));
			T data = mapper.readValue(serialized, cls);
			return Optional.of(data);
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public boolean validateJobProfile(Path pathToFile){
		Optional<JobProfilesMap> sol = objectFromPath(pathToFile, JobProfilesMap.class);
		return (sol.isPresent() && sol.get().validate());
	}

	public boolean validateClassParameters(Path pathToFile){
		Optional<ClassParametersMap> sol = objectFromPath(pathToFile, ClassParametersMap.class);
		return (sol.isPresent() && sol.get().validate());
	}

	public boolean validateVMConfigurations(Path pathToFile){
		Optional<VMConfigurationsMap> sol = objectFromPath(pathToFile, VMConfigurationsMap.class);
		return (sol.isPresent() && sol.get().validate());
	}

	public boolean validatePrivateCloudParameters(Path pathToFile){
		Optional<PrivateCloudParameters> sol = objectFromPath(pathToFile, PrivateCloudParameters.class);
		return (sol.isPresent() && sol.get().validate());
	}

	public boolean validatePublicCloudParameters(Path pathToFile){
		Optional<PublicCloudParametersMap> sol = objectFromPath(pathToFile, PublicCloudParametersMap.class);
		return (sol.isPresent() && sol.get().validate());
	}

	public Optional<InstanceDataMultiProvider> readInstanceDataMultiProvider(Path pathToFile){
		Optional<InstanceDataMultiProvider> sol = objectFromPath(pathToFile, InstanceDataMultiProvider.class);
		return sol;
	}
	

	public boolean validateInstanceData(Path pathToFile){
		Optional<InstanceData> inputData = objectFromPath(pathToFile, InstanceData.class);
		return (inputData.isPresent());
	}

}
