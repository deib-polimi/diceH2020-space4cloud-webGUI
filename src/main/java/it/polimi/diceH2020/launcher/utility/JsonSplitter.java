/*
Copyright 2017 Eugenio Gianniti
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
package it.polimi.diceH2020.launcher.utility;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.*;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;

import java.util.*;
import java.util.Map.Entry;

public class JsonSplitter {

	public static List<InstanceDataMultiProvider> splitInstanceDataMultiProvider(InstanceDataMultiProvider instanceDataMultiProvider, Scenario scenario){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceDataMultiProvider instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(scenario);
			if(scenario.getCloudType() == CloudType.PRIVATE) {
				instanceData.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
				///CHECK: is this not required for private with no admission control?
				if(scenario.getAdmissionControl()) {
					instanceData.setPrivateCloudParameters(instanceDataMultiProvider.getPrivateCloudParameters());
				}
			} else if(scenario.getLongTermCommitment()) {
				instanceData.setMapPublicCloudParameters(new PublicCloudParametersMap(fromMapPublicCloudParametersToMapTypeVMs(instanceDataMultiProvider.getMapPublicCloudParameters(), provider)));
			}
			instanceDataList.add(instanceData);
		}
		return instanceDataList;

	}

	/**
	 * This method creates the initial instanceData. <br>
	 * It does 2 mandatory mappings from InstanceDataMultiProvider: <br>
	 * &emsp; •maps mapClassParameters to lstClass<br>
	 * &emsp; •maps mapJobProfile to mapProfiles 
	 * @param input InstanveDataMultiProvider to split
	 * @return InstanceData with an initial set of parameters, those ones that are mandatory
	 */
	private static InstanceDataMultiProvider buildPartialInput(InstanceDataMultiProvider input, String provider){

		Map<String, Map<String, Map<String, JobProfile>>> map = fromMapJobProfileToMapProfiles(input.getMapJobProfiles(), provider);
		InstanceDataMultiProvider partialInput = new InstanceDataMultiProvider();
		partialInput.setMapClassParameters(input.getMapClassParameters());
		partialInput.setId(input.getId());
		partialInput.setMapJobProfiles(new JobProfilesMap(map));
		partialInput.setMapJobMLProfiles(input.getMapJobMLProfiles());
		if (input.getMapDags () != null) {
			partialInput.setMapDags (input.getMapDags ());
		}

		return partialInput;
	}

	private static Map<String, Map<String, Map<String, JobProfile>>>  fromMapJobProfileToMapProfiles(JobProfilesMap input, String provider){
		Map<String, Map<String, Map<String, JobProfile>>> map = new HashMap<>(input.getMapJobProfile());
		Set<String> providerToBeRemoved = input.getProviders();
		providerToBeRemoved.remove(provider);

		for (Entry<String, Map<String, Map<String, JobProfile>>> entry : map.entrySet()) {
			entry.getValue().keySet().removeAll(providerToBeRemoved);
		}
		return map;
	}

	private static Map<String, Map<String,Map<String,PublicCloudParameters>>> fromMapPublicCloudParametersToMapTypeVMs(PublicCloudParametersMap input, String provider){

		Map<String, Map<String,Map<String,PublicCloudParameters>>> map = new HashMap<>(input.getMapPublicCloudParameters());
		Set<String> providerToBeRemoved = input.getProviders();
		providerToBeRemoved.remove(provider);

		for (Entry<String, Map<String, Map<String, PublicCloudParameters>>> entry : map.entrySet()) {
			entry.getValue().keySet().removeAll(providerToBeRemoved);
		}

		return map;
	}

}
