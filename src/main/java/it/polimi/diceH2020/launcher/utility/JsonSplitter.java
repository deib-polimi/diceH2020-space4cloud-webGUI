/*
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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;

import java.util.*;
import java.util.Map.Entry;

public class JsonSplitter {

	public static List<InstanceDataMultiProvider> splitInstanceDataMultiProvider(InstanceDataMultiProvider instanceDataMultiProvider, Scenarios scenario){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<>();

		switch(scenario){
			case PrivateAdmissionControl:
				instanceDataList = buildInstanceDataPrPeak(instanceDataMultiProvider);
				break;
			case PrivateAdmissionControlWithPhysicalAssignment:
				instanceDataList = buildInstanceDataPrPeakWithPhysicalAssignment(instanceDataMultiProvider);
				break;
			case PrivateNoAdmissionControl:
				instanceDataList = buildInstanceDataPrAvg(instanceDataMultiProvider);
				break;
			case PublicPeakWorkload:
				instanceDataList = buildInstanceDataPuPeak(instanceDataMultiProvider);
				break;
			case PublicAvgWorkLoad:
				instanceDataList = buildInstanceDataPuAvg(instanceDataMultiProvider);
				break;
			default:
				new Exception("Error with the selected scenario");
				break;
		}

		return instanceDataList;
	}

	private static List<InstanceDataMultiProvider> buildInstanceDataPrAvg(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<InstanceDataMultiProvider>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceDataMultiProvider instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Optional.of(Scenarios.PrivateNoAdmissionControl));
			instanceData.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}

	private static List<InstanceDataMultiProvider> buildInstanceDataPrPeak(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceDataMultiProvider> instanceDataMultiProviderList = new ArrayList<InstanceDataMultiProvider>();

		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceDataMultiProvider idmp = buildPartialInput(instanceDataMultiProvider,provider);
			idmp.setScenario(Optional.of(Scenarios.PrivateAdmissionControl)); 
			idmp.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
			idmp.setPrivateCloudParameters(instanceDataMultiProvider.getPrivateCloudParameters());
			instanceDataMultiProviderList.add(idmp);
		}
		return instanceDataMultiProviderList;
	}

	private static List<InstanceDataMultiProvider> buildInstanceDataPrPeakWithPhysicalAssignment(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<InstanceDataMultiProvider>();

		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceDataMultiProvider instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Optional.of(Scenarios.PrivateAdmissionControlWithPhysicalAssignment));
			instanceData.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
			instanceData.setPrivateCloudParameters(instanceDataMultiProvider.getPrivateCloudParameters());
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}

	private static List<InstanceDataMultiProvider> buildInstanceDataPuAvg(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<InstanceDataMultiProvider>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){
			InstanceDataMultiProvider instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Optional.of(Scenarios.PublicAvgWorkLoad));
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}

	private static List<InstanceDataMultiProvider> buildInstanceDataPuPeak(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceDataMultiProvider> instanceDataList = new ArrayList<InstanceDataMultiProvider>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){
			InstanceDataMultiProvider instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Optional.of(Scenarios.PublicPeakWorkload)); 
			instanceData.setMapPublicCloudParameters(new PublicCloudParametersMap(fromMapPublicCloudParametersToMapTypeVMs(instanceDataMultiProvider.getMapPublicCloudParameters(), provider)));
			instanceDataList.add(instanceData);
		}

		return instanceDataList;
	}

	/**
	 * This method creates the initial instanceData. <br>
	 * It does 2 mandatory mappings from InstanceDataMultiProvider: <br>
	 * &emsp; •maps mapClassParameters to lstClass<br>
	 * &emsp; •maps mapJobProfile to mapProfiles 
	 * @param input
	 * @return InstanceData with an initial set of parameters, those ones that are mandatory
	 */
	private static InstanceDataMultiProvider buildPartialInput(InstanceDataMultiProvider input, String provider){

		Map<String, Map<String, Map<String, JobProfile>>> map = fromMapJobProfileToMapProfiles(input.getMapJobProfiles(), provider);
		InstanceDataMultiProvider partialInput = new InstanceDataMultiProvider();
		partialInput.setMapClassParameters(input.getMapClassParameters());
		partialInput.setId(input.getId());
		partialInput.setMapJobProfiles(new JobProfilesMap(map));
		partialInput.setMapJobMLProfiles(input.getMapJobMLProfiles());

		return partialInput;
	}

	private static Map<String, Map<String, Map<String, JobProfile>>>  fromMapJobProfileToMapProfiles(JobProfilesMap input, String provider){
		Map<String, Map<String, Map<String, JobProfile>>> map = new HashMap<String, Map<String, Map<String, JobProfile>>>(input.getMapJobProfile());
		Set<String> providerToBeRemoved = input.getProviders();
		providerToBeRemoved.remove(provider);
		
		for (Entry<String, Map<String, Map<String, JobProfile>>> entry : map.entrySet()) {
			entry.getValue().keySet().removeAll(providerToBeRemoved);
		}
		return map;
	}

	private static Map<String, Map<String,Map<String,PublicCloudParameters>>> fromMapPublicCloudParametersToMapTypeVMs(PublicCloudParametersMap input, String provider){
		
		Map<String, Map<String,Map<String,PublicCloudParameters>>> map = new HashMap<String, Map<String,Map<String,PublicCloudParameters>>>(input.getMapPublicCloudParameters());
		Set<String> providerToBeRemoved = input.getProviders();
		providerToBeRemoved.remove(provider);
		
		for (Entry<String, Map<String, Map<String, PublicCloudParameters>>> entry : map.entrySet()) {
			entry.getValue().keySet().removeAll(providerToBeRemoved);
		}
		
		return map;
	}

}
