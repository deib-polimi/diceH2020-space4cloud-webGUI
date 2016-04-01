package it.polimi.diceH2020.launcher.model;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import lombok.Data;

@Entity
@Data
public class InteractiveExperiment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;           
	@NotNull
	private String instanceName = "";
	
	@ManyToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name = "SIM_MANAGER")//, updatable = false, insertable=false, nullable = false)
	private SimulationsManager simulationsManager;
	
	private String simType;
	
	//@NotNull
	@Min(1)
	private  Integer thinkTime = 0;     	
	//@NotNull
	private  Integer numUsers=0;	
	//@NotNull
	private Integer numVMs=0;
	
	private double responseTime = 0d;
	
	private Integer gamma = 0;
	
	private String provider = "NONE";
	
	private int numSolutions = 0;
	
	@Column(length = 1000)
	@NotNull
	private String finalSolution="";
	
	//@NotNull
	private Integer iter = 1;
	
	private long experimentalDuration = 0;

	private String state = "ready"; //states:  ready to be executed, running, completed, failed
	//@NotNull
	private boolean done; //TODO is it used?
	
	public InteractiveExperiment(){
	}

	public Solution getInputSolution()  {
		if(simulationsManager instanceof SimulationsWIManager){
			SimulationsWIManager wiM = (SimulationsWIManager)simulationsManager;
			Solution sol = wiM.getInputJson();
			SolutionPerJob spj = sol.getLstSolutions().get(0);
			spj.getJob().setThink(thinkTime);
			spj.setNumberVM(numVMs);
			spj.setNumberUsers(numUsers);
			return sol;
		}
		throw new ClassCastException();
	}
	
	public InstanceData getInputData(){
		if(simulationsManager instanceof SimulationsOptManager){
			SimulationsOptManager wiM = (SimulationsOptManager)simulationsManager;
			InstanceData data = wiM.getInputData();
			return data;
		}
		throw new ClassCastException();
	}
	
	public void setSol(Solution sol){
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.finalSolution = compress(mapper.writeValueAsString(sol));
		} catch ( IOException e) {
			this.finalSolution = "Error";
		}
	}
	public Solution getSol(){
		ObjectMapper mapper = new ObjectMapper();
		try {
			return this.finalSolution.equals("") || this.finalSolution.equals("Error")? null: mapper.readValue(decompress(this.finalSolution), Solution.class);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String compress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        System.out.println("String length : " + str.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        String outStr = out.toString("ISO-8859-1");
        System.out.println("Output String lenght : " + outStr.length());
        return outStr;
     }
    
    private static String decompress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        System.out.println("Input String length : " + str.length());
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str.getBytes("ISO-8859-1")));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "ISO-8859-1"));
        String outStr = "";
        String line;
        while ((line=bf.readLine())!=null) {
          outStr += line;
        }
        System.out.println("Output String lenght : " + outStr.length());
        return outStr;
     }
}