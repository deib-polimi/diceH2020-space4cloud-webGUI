package it.polimi.diceH2020.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfilesMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PrivateCloudParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PublicCloudParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfigurationsMap;

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
	
	public boolean validateInstanceDataMultiProvider(Path pathToFile){
		Optional<InstanceDataMultiProvider> sol = objectFromPath(pathToFile, InstanceDataMultiProvider.class);
		return (sol.isPresent() && sol.get().validate());
	}
	
//	public boolean validateOptInput(Path pathToFile){
//		
//		Optional<InstanceData> inputData = objectFromPath(pathToFile, InstanceData.class);
//		return (inputData.isPresent());
//	}
	
}
