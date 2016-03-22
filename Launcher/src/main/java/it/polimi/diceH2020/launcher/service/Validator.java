package it.polimi.diceH2020.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;

@Service
public class Validator {

	private ObjectMapper mapper = new ObjectMapper();

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
	
	public boolean validateWISolution(Path pathToFile){
		Optional<Solution> sol = objectFromPath(pathToFile, Solution.class);
		return (sol.isPresent() && sol.get().validate() && sol.get().getLstSolutions().size() == 1);
	}
	
	public boolean validateOptInput(Path pathToFile){
		
		Optional<InstanceData> inputData = objectFromPath(pathToFile, InstanceData.class);
		return (inputData.isPresent());
	}
	
}
