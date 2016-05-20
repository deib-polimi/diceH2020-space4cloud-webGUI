package it.polimi.diceH2020.launcher.utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.model.InteractiveExperiment;
import it.polimi.diceH2020.launcher.model.SimulationsWIManager;


/**
 * This class is used for creating V10 model's xls file
 * 
 * This file will be created only when all the simulations are completed,
 * and will be available via web browser or in the machine where the web service is running(in simulations/[current set of simulations folder]).
 * 
 */
@Component
public class ExcelWriter {
	
	@Autowired
	FileUtility fileUtility;

	    public void writeListToExcel(SimulationsWIManager simManager) throws IOException{
	    	   Workbook workbook = createWorkbook(simManager);
	        	FileOutputStream fos = new FileOutputStream(fileUtility.provideTemporaryFile("result", "xls"));	        	
	            workbook.write(fos);
	            fos.close();
	            workbook.close();
	            //System.out.println(FILE_PATH + " is successfully written");
	    }
	    
	    public Workbook createWorkbook(SimulationsWIManager simManager){
	    	List<InteractiveExperiment> simulationList = simManager.getExperimentsList();
	   	 
	        // Using XSSF for xlsx format, for xls use HSSF
	        Workbook workbook = new XSSFWorkbook();

	        Sheet simulationSheet = workbook.createSheet("Simulations");
	        
	        int rowIndex = 0;
	        Row row = simulationSheet.createRow(rowIndex++);
	        int cellIndex = 0;
	        row.createCell(cellIndex++).setCellValue("Total Run Time");
	        row.createCell(cellIndex++).setCellValue(String.valueOf(simManager.getSimDuration()));
	        
	        row = simulationSheet.createRow(rowIndex++);
	        
	        cellIndex = 0;
	        row.createCell(cellIndex++).setCellValue("Accuracy");
	        row.createCell(cellIndex++).setCellValue("Think Time[ms]");	        
	        row.createCell(cellIndex++).setCellValue("Map");
	        row.createCell(cellIndex++).setCellValue("Reduce");
	        row.createCell(cellIndex++).setCellValue("Users");
	        row.createCell(cellIndex++).setCellValue("VMs");
	        row.createCell(cellIndex++).setCellValue("Response Time");
	        row.createCell(cellIndex++).setCellValue("Run Time");
	        
	        for(InteractiveExperiment sim : simulationList){
	        	
	            row = simulationSheet.createRow(rowIndex++);
	            cellIndex = 0;
	            row.createCell(cellIndex++).setCellValue(simManager.getAccuracy());
		        row.createCell(cellIndex++).setCellValue(sim.getThinkTime());
		        row.createCell(cellIndex++).setCellValue(simManager.getDecompressedInputJson().getSolutionPerJob(0).getProfile().getNM());
		        row.createCell(cellIndex++).setCellValue(simManager.getDecompressedInputJson().getSolutionPerJob(0).getProfile().getNR());
		        row.createCell(cellIndex++).setCellValue(sim.getNumUsers());
		        row.createCell(cellIndex++).setCellValue(sim.getNumVMs());
		        row.createCell(cellIndex++).setCellValue(sim.getResponseTime());
		        row.createCell(cellIndex++).setCellValue(sim.getExperimentalDuration());	            
	        }
	        return workbook;
	    }
	}	