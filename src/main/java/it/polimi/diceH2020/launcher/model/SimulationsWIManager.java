package it.polimi.diceH2020.launcher.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.utility.Compressor;
import it.polimi.diceH2020.launcher.utility.FileUtility;
import lombok.Data;

/**
 * This class contain informations about client's requested set of simulations
 */
@Entity
@Data
public class SimulationsWIManager extends SimulationsManager{

	@NotNull
	private Integer accuracy;

	@Transient
	private Solution inputJson;

	@Transient
	private Integer maxNumUsers;
	@Transient
	private Integer maxNumVMs;
	@Transient
	private Integer minNumUsers;
	@Transient
	private Integer minNumVMs;

	private Integer numIter;

	private SolverType solver;
	
	@NotNull
	@Transient
	private Integer stepUsers;

	@NotNull
	@Transient
	private Integer stepVMs;

	@Transient
	private Integer thinkTime;
	
	@NotNull
	@Min(60)
	private Integer simDuration;

	public SimulationsWIManager() {
		super();
		setType("WI");
		
		solver = SolverType.QNSolver;
		
		simDuration = 60;
		stepVMs = 1;
		stepUsers = 1;
		numIter = 1;
		accuracy = 5;
		maxNumUsers = 1;
		maxNumVMs = 1;
		minNumUsers = 1;
		minNumVMs = 1;
	}

	public void buildExperiments() {
		getExperimentsList().clear();
		for (int numVMs = minNumVMs; numVMs <= maxNumVMs; numVMs = numVMs + stepVMs)
			for (int numUsers = minNumUsers; numUsers <= maxNumUsers; numUsers = numUsers + stepUsers)
				for (int it = 1; it <= this.numIter; it++) {
					InteractiveExperiment experiment = new InteractiveExperiment();
					experiment.setIter(it);
					experiment.setNumUsers(numUsers);
					experiment.setNumVMs(numVMs);
					experiment.setThinkTime(this.thinkTime);
					experiment.setInstanceName(getInstanceName());
					experiment.setSimulationsManager(this);
					experiment.setSimType("WI");
					getExperimentsList().add(experiment);
				}
	}

	public Solution getInputJson() {
		if (inputJson != null) {
			return inputJson;
		} else if (getInput() != null) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				return getInput().equals("") || getInput().equals("Error") ? null : mapper.readValue(Compressor.decompress(getInput()), Solution.class);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return inputJson;
	}

	public void setInputJson(Solution inputSolution) {
		this.inputJson = inputSolution;

		ObjectMapper mapper = new ObjectMapper();
		try {
			setInput(Compressor.compress(mapper.writeValueAsString(inputSolution)));
		} catch (IOException e) {
			setInput("Error");
		}
		Double tt = inputSolution.getLstSolutions().get(0).getJob().getThink();
		this.thinkTime = tt.intValue();
		setInstanceName(inputSolution.getId());
	}

	public void writeFinalResults(){
		try {
			writeResultOnExcel();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void writeResultOnExcel() throws FileNotFoundException, IOException{
		List<InteractiveExperiment> simulationList = getExperimentsList();
		 
        // Using XSSF for xlsx format, for xls use HSSF
        Workbook workbook = new XSSFWorkbook();

        Sheet simulationSheet = workbook.createSheet("Simulations");
        
        int rowIndex = 0;
        Row row = simulationSheet.createRow(rowIndex++);
        int cellIndex = 0;
        row.createCell(cellIndex++).setCellValue("Total Run Time");
        row.createCell(cellIndex++).setCellValue(String.valueOf(simDuration));
        
        row = simulationSheet.createRow(rowIndex++);
        
        cellIndex = 0;
        row.createCell(cellIndex++).setCellValue("Accuracy");
        row.createCell(cellIndex++).setCellValue("Think Time[ms]");	        
        row.createCell(cellIndex++).setCellValue("Map");
        row.createCell(cellIndex++).setCellValue("Reduce");
        row.createCell(cellIndex++).setCellValue("Users");
        row.createCell(cellIndex++).setCellValue("VMs");
        row.createCell(cellIndex++).setCellValue("Iteration");
        row.createCell(cellIndex++).setCellValue("Response Time");
        row.createCell(cellIndex++).setCellValue("Run Time");
        
        for(InteractiveExperiment sim : simulationList){	    
            row = simulationSheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(accuracy);
	        row.createCell(cellIndex++).setCellValue(thinkTime);
	        row.createCell(cellIndex++).setCellValue(inputJson.getSolutionPerJob(0).getProfile().getNM());
	        row.createCell(cellIndex++).setCellValue(inputJson.getSolutionPerJob(0).getProfile().getNR());
	        row.createCell(cellIndex++).setCellValue(sim.getNumUsers());
	        row.createCell(cellIndex++).setCellValue(sim.getNumVMs());
	        row.createCell(cellIndex++).setCellValue(sim.getIter());
	        row.createCell(cellIndex++).setCellValue(sim.getResponseTime());
	        row.createCell(cellIndex++).setCellValue(sim.getExperimentalDuration());	            
        }
        	FileUtility fileUtility = new FileUtility();
        	
        	File tmpFile = fileUtility.provideTemporaryFile("result", ".xlsxon ti");
        	FileOutputStream fos = new FileOutputStream(tmpFile);	        	
            workbook.write(fos);
            fos.close();
            workbook.close();
            setResultFilePath(tmpFile.getAbsolutePath());
	}
}
