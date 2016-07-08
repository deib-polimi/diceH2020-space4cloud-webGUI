package it.polimi.diceH2020.launcher.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfilesMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PublicCloudParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PublicCloudParametersMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;

public class JsonMapper {
	
	public static List<InstanceData> getInstanceDataList(InstanceDataMultiProvider instanceDataMultiProvider, Scenarios scenario){
		List<InstanceData> instanceDataList = new ArrayList<>();
		
		switch(scenario){
			case PrivateAdmissionControl:
				instanceDataList = buildInstanceDataPrPeak(instanceDataMultiProvider);
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
				new Exception("Error with scenario enumaration");
				break;
		}
		
		return instanceDataList;
	}
	
	private static List<InstanceData> buildInstanceDataPrAvg(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceData> instanceDataList = new ArrayList<InstanceData>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceData instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Scenarios.PrivateNoAdmissionControl);
			instanceData.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}
	
	private static List<InstanceData> buildInstanceDataPrPeak(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceData> instanceDataList = new ArrayList<InstanceData>();
		
		for(String provider : instanceDataMultiProvider.getProvidersList()){ //useless loop, used for compliance 
			InstanceData instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Scenarios.PrivateAdmissionControl);
			instanceData.setMapVMConfigurations(instanceDataMultiProvider.getMapVMConfigurations());
			instanceData.setPrivateCloudParameters(instanceDataMultiProvider.getPrivateCloudParameters());
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}
	
	private static List<InstanceData> buildInstanceDataPuAvg(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceData> instanceDataList = new ArrayList<InstanceData>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){
			InstanceData instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Scenarios.PublicAvgWorkLoad);
			instanceDataList.add(instanceData);
		}
		return instanceDataList;
	}
	
	private static List<InstanceData> buildInstanceDataPuPeak(InstanceDataMultiProvider instanceDataMultiProvider){
		List<InstanceData> instanceDataList = new ArrayList<InstanceData>();
		for(String provider : instanceDataMultiProvider.getProvidersList()){
			InstanceData instanceData = buildPartialInput(instanceDataMultiProvider,provider);
			instanceData.setScenario(Scenarios.PublicPeakWorkload);
			instanceData.setMapTypeVMs(fromMapPublicCloudParametersToMapTypeVMs(instanceDataMultiProvider.getMapPublicCloudParameters(), provider));
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
	private static InstanceData buildPartialInput(InstanceDataMultiProvider input, String provider){
		
		List<JobClass> lstClasses = fromMapClassParametersToLstClass(input.getMapClassParameters());
		Map<TypeVMJobClassKey, Profile> map = fromMapJobProfileToMapProfiles(input.getMapJobProfiles(), provider);
		
		InstanceData partialInput = new InstanceData();
		
		partialInput.setId(input.getId());
		partialInput.setGamma(1500);
		partialInput.setLstClass(lstClasses);
		partialInput.setMapProfiles(map);
		partialInput.setProvider(provider);
		
		return partialInput;
	}
	
	private static List<JobClass> fromMapClassParametersToLstClass(ClassParametersMap input){
		List<JobClass> lstClasses = new ArrayList<>();
		
		for (Map.Entry<String, ClassParameters> entry : input.getMapClassParameters().entrySet()) {
			JobClass jc = new JobClass();
			
			jc.setD(entry.getValue().getD());
			jc.setHlow(entry.getValue().getHlow());
			jc.setHup(entry.getValue().getHup());
			jc.setId(entry.getKey());
			jc.setJob_penalty(entry.getValue().getPenalty());
			jc.setThink(entry.getValue().getThink());
			jc.setM(entry.getValue().getM());
			jc.setV(entry.getValue().getV());
			
			lstClasses.add(jc); 
		}
		
		return lstClasses;
	}
	
	private static Map<TypeVMJobClassKey, Profile> fromMapJobProfileToMapProfiles(JobProfilesMap input, String provider){
		Map<TypeVMJobClassKey, Profile> map = new HashMap<TypeVMJobClassKey, Profile>();
		
		for (Entry<String, Map<String, JobProfile>> jobIDs : input.get_IdVMTypes_Map(provider).entrySet()) {
	    	for (Map.Entry<String, JobProfile> typeVMs : jobIDs.getValue().entrySet()) {
	    		TypeVMJobClassKey key = new TypeVMJobClassKey();
	    		Profile p = new Profile();
	    		
	    		key.setJob(jobIDs.getKey());
	    		key.setTypeVM(typeVMs.getKey());
	    		
	    		p.setNM(typeVMs.getValue().getNM());
	    		p.setNR(typeVMs.getValue().getNR());
	    		p.setCM(typeVMs.getValue().getCM());
	    		p.setCR(typeVMs.getValue().getCR());
	    		p.setMavg(typeVMs.getValue().getMavg());
	    		p.setMmax(typeVMs.getValue().getMmax());
	    		p.setRavg(typeVMs.getValue().getRavg());
	    		p.setRmax(typeVMs.getValue().getRmax());
	    		p.setSH1max(typeVMs.getValue().getSH1max());
	    		p.setSHtypavg(typeVMs.getValue().getSHtypavg());
	    		p.setSHtypmax(typeVMs.getValue().getSHtypmax());
	    		
	    		map.put(key, p);
	    	}
		}
		return map;
	}
	
	private static Map<String,List<TypeVM>> fromMapPublicCloudParametersToMapTypeVMs(PublicCloudParametersMap input, String provider){
		Map<String,List<TypeVM>> map = new HashMap<String,List<TypeVM>>();
		for (Entry<String, Map<String, PublicCloudParameters>> jobIDs : input.get_IdVMTypes_Map(provider).entrySet()) {
			List<TypeVM> vmList = new ArrayList<TypeVM>();
	    	for (Entry<String, PublicCloudParameters> typeVMs : jobIDs.getValue().entrySet()) {
	    		
	    		TypeVM vm = new TypeVM();
	    		vm.setEta(typeVMs.getValue().getEta());
	    		vm.setId(typeVMs.getKey()); //id of vmType
	    		vm.setR(typeVMs.getValue().getR());
	    		vmList.add(vm);
	    		
		    }
	    	map.put(jobIDs.getKey(),vmList);
		}
		return map;
	}
	
	
}
