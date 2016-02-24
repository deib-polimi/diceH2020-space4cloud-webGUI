package it.polimi.diceH2020.launcher.utility;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.model.Simulation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

/**
 * This class is used for creating V7 model's xls file.
 * 
 * This file will be created only when all the simulations are completed,
 * and will be available via web browser or in the machine where the web service is running(in simulations/[current set of simulations folder]).
 * 
 */
@Component
public class ExcelWriter2 {

	    public void writeListToExcel(List<Simulation> simulationList, String string, double totalRuntime) throws IOException{
	    	String FILE_PATH = string+"results.xls";;

	        // Using XSSF for xlsx format, for xls use HSSF
	        Workbook workbook = new XSSFWorkbook();

	        Sheet simulationSheet = workbook.createSheet("Simulations");
	        
	        int rowIndex = 0;
	        Row row = simulationSheet.createRow(rowIndex++);
	        int cellIndex = 0;
	        row.createCell(cellIndex++).setCellValue("Total Run Time");
	        row.createCell(cellIndex++).setCellValue(String.valueOf(totalRuntime));
	        row = simulationSheet.createRow(rowIndex++);
	        
	        row.createCell(cellIndex++).setCellValue("Erlang");
	        row.createCell(cellIndex++).setCellValue(String.valueOf(simulationList.get(0).getErlang()));
	        
	        row.createCell(cellIndex++).setCellValue("Min Batch");
	        row.createCell(cellIndex++).setCellValue(String.valueOf(simulationList.get(0).getMinBatch() ));
	        
	        row.createCell(cellIndex++).setCellValue("Max");
	        row.createCell(cellIndex++).setCellValue(String.valueOf(simulationList.get(0).getMaxBatch() ));
	        
	        cellIndex = 0;
	        row.createCell(cellIndex++).setCellValue("Accuracy");
	        row.createCell(cellIndex++).setCellValue("Map Time[ms]");
	        row.createCell(cellIndex++).setCellValue("Map Time2[ms]");
	        row.createCell(cellIndex++).setCellValue("Reduce Time[ms]");
	        row.createCell(cellIndex++).setCellValue("Reduce Time2[ms]");
	        row.createCell(cellIndex++).setCellValue("Think Time[ms]");	        
	        row.createCell(cellIndex++).setCellValue("Think Time2[ms]");	        
	        row.createCell(cellIndex++).setCellValue("Map");
	        row.createCell(cellIndex++).setCellValue("Map2");
	        row.createCell(cellIndex++).setCellValue("Reduce");
	        row.createCell(cellIndex++).setCellValue("Reduce2");
	        row.createCell(cellIndex++).setCellValue("Users");
	        row.createCell(cellIndex++).setCellValue("Users2");
	        row.createCell(cellIndex++).setCellValue("Cores");
	        row.createCell(cellIndex++).setCellValue("Cores2");
	        row.createCell(cellIndex++).setCellValue("Throughput1");
	        row.createCell(cellIndex++).setCellValue("Throughput2");
	        row.createCell(cellIndex++).setCellValue("ThroughputEND");
	        row.createCell(cellIndex++).setCellValue("Response Time 1");
	        row.createCell(cellIndex++).setCellValue("Response Time 2");
	        row.createCell(cellIndex++).setCellValue("Response Time END");
	        row.createCell(cellIndex++).setCellValue("Run Time");
	        
	        //System.out.println("Creating xls file");

	        for(Simulation sim : simulationList){	        		        	
	            row = simulationSheet.createRow(rowIndex++);
	            //System.out.println("writing excel row");
	            cellIndex = 0;
	            row.createCell(cellIndex++).setCellValue(sim.getAccuracy());
	        	row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getMapTime(), 0));
	        	row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getMapTime(), 1));
		        row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getReduceTime(),0));
		        row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getReduceTime(),1));
		        row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getThinkTime(),0));
		        row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getThinkTime(),1));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getMap(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getMap(),1));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getReduce(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getReduce(),1));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getUsers(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getUsers(),1));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getCores(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getCores(),1));
	            
	            row.createCell(cellIndex++).setCellValue(Array.getDouble(sim.getThroughput(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getDouble(sim.getThroughput(),1));
	            row.createCell(cellIndex++).setCellValue(sim.getThroughputEnd());
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getResponseTime(),0));
	            row.createCell(cellIndex++).setCellValue(Array.getInt(sim.getResponseTime(),1));
	            row.createCell(cellIndex++).setCellValue(sim.getResponseTimeEnd());
	            
	            row.createCell(cellIndex++).setCellValue(sim.getRuntime());	           
	        }

	            FileOutputStream fos = new FileOutputStream(FILE_PATH);
	            workbook.write(fos);
	            fos.close();
	            workbook.close();
	            //System.out.println(FILE_PATH + " is successfully written");
	      
	    }

}
