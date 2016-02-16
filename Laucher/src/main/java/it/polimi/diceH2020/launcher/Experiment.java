package it.polimi.diceH2020.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

import javax.annotation.PostConstruct;

import org.apache.xbean.blueprint.context.impl.FileEditor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;

@Service
public class Experiment {

	private ObjectMapper mapper;
	@Autowired
	private Settings settings;
	private static String SOLUTION_ENDPOINT;
	private static String INPUTDATA_ENDPOINT;
	private static String EVENT_ENDPOINT;
	private static String STATE_ENDPOINT;
	private static String UPLOAD_ENDPOINT;
	private static Path LOCAL_DYNAMIC_FOLDER;

	public Experiment() throws IOException {
		mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer());
		mapper.registerModule(module);
		LOCAL_DYNAMIC_FOLDER = Paths.get(settings.getResultDir());
		Files.deleteIfExists(LOCAL_DYNAMIC_FOLDER);
		Files.createDirectory(LOCAL_DYNAMIC_FOLDER);

	}

	@PostConstruct
	private void init() {
		INPUTDATA_ENDPOINT = settings.getfullAddress() + "/inputdata";
		EVENT_ENDPOINT = settings.getfullAddress() + "/event";
		STATE_ENDPOINT = settings.getfullAddress() + "/state";
		UPLOAD_ENDPOINT = settings.getfullAddress() + "/upload";
		SOLUTION_ENDPOINT = settings.getfullAddress() + "/solution";

	}

	private RestTemplate restTemplate = new RestTemplate();

	public void launch(Path inputDataPath) {

		String serialized;
		try {
			serialized = new String(Files.readAllBytes(inputDataPath));
			InstanceData data = mapper.readValue(serialized, InstanceData.class);
			String res = restTemplate.postForObject(INPUTDATA_ENDPOINT, data, String.class);
			if (res.equals("CHARGED_INPUTDATA")) {

				res = restTemplate.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_INIT, String.class);
				if (res.equals("RUNNING_INIT")) {
					res = "RUNNING_INIT";
					while (res.equals("RUNNING_INIT")) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
					}
					if (res.equals("CHARGED_INITSOLUTION")) {
						res = restTemplate.postForObject(EVENT_ENDPOINT, Events.TO_RUNNING_LS, String.class);
						if (res.equals("RUNNING_INIT")) {
							res = "RUNNING_LS";
							while (res.equals("RUNNING_LS")) {
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								res = restTemplate.getForObject(STATE_ENDPOINT, String.class);
							}

						}
						if (res.equals("FINISH")) {
							res = restTemplate.postForObject(EVENT_ENDPOINT, Events.RESET, String.class);
							if (!res.equals("IDLE")) {
								System.out.println("ERROR!!");
							}

							Solution sol = restTemplate.getForObject(SOLUTION_ENDPOINT, Solution.class);
							String solSerialized = mapper.writeValueAsString(sol);
							System.out.println(serialized);
							String solFilePath = LOCAL_DYNAMIC_FOLDER+File.separator+sol.getId()+".json";
							Files.write(Paths.get(solFilePath), solSerialized.getBytes());
						}
					}

				}
			}

		} catch (IOException e) {
			e.printStackTrace(); 	
		}

	}

	public void send(Path f) {
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
		String content;
		try {
			content = new String(Files.readAllBytes(f));

			final String filename = f.getFileName().toString();
			map.add("name", filename);
			map.add("filename", filename);
			ByteArrayResource contentsAsResource = new ByteArrayResource(content.getBytes("UTF-8")) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
			map.add("file", contentsAsResource);
			String result = restTemplate.postForObject(UPLOAD_ENDPOINT, map, String.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
