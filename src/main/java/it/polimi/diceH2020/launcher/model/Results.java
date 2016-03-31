package it.polimi.diceH2020.launcher.model;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import lombok.Data;
@Entity
@Data
public class Results {
	@Id
	private long id;
	private int iteration;
	private String instanceName;
	
	@Column(length = 1000)
	@NotNull
	private String solution="";

	public void setSol(Solution sol){
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.solution = compress(mapper.writeValueAsString(sol));
		} catch ( IOException e) {
			this.solution = "Error";
		}
	}
	public Solution getSol(){
		ObjectMapper mapper = new ObjectMapper();
		try {
			return this.solution.equals("") || this.solution.equals("Error")? null: mapper.readValue(decompress(this.solution), Solution.class);
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
