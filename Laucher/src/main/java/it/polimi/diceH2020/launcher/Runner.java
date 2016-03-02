package it.polimi.diceH2020.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.model.ExperimentRecord;
import it.polimi.diceH2020.launcher.repository.ExperimentRepository;

@Component
public class Runner {

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	@Autowired
	private ExperimentRepository expRecordRepo;

	private int numFiles = 0;

	@Autowired
	private Settings settings;

	@Autowired
	private Experiment experiment;
	
	
	@Async
	public void run(){
		try {
			exec();
		} catch (IOException | MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void exec() throws IOException, MessagingException {

		experiment.waitForWS();
		sendSideFiles();

		List<ExperimentRecord> lstExp = retriveListUndoneExperiments();
		printDirFiles();
		
		if (lstExp.size() > 0) {
			logger.info("Some experiment pending in the database");
			experiment.setTotalAnalysisToExecute(lstExp.size());
			logger.info("The launcher is in running mode");
			lstExp.forEach(e -> experiment.launch(e));
			
		} else {
			setNumFiles("json", settings.getInstanceDir());
			saveExperimentInDB().stream().forEachOrdered(e -> experiment.launch(e));
		}
		logger.info("The launcher is in waiting mode");
		

	}

	public String stop() {
		
		if (experiment.stop()) {
				//mailSender.send("michele.ciavotta@gmail.com", "Experiment ended", "Sir, the experiment ended");
			return "WS will soon be reset";
		}
		else return "Error resetting the WS";
			
	}
	
	
	private List<ExperimentRecord> retriveListUndoneExperiments() {
		return expRecordRepo.findByDone(false);
	}

	private void sendSideFiles() throws IOException {
		Stream<Path> streamTxt = accessInstanceFolder("txt", settings.getTxtDir());
		streamTxt.forEach(f -> experiment.send(f));
	}

	private void setNumFiles(String extension, String instanceDir) {
		try {
			accessInstanceFolder(extension, instanceDir).forEach(f -> ++numFiles);
			experiment.setTotalAnalysisToExecute(numFiles * settings.getNumIterations());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	private void printDirFiles(){
		try {
			accessInstanceFolder("json", settings.getInstanceDir()).forEach(p -> System.out.println(p.toString()));;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private List<ExperimentRecord> saveExperimentInDB() {
		try {
			return accessInstanceFolder("json", settings.getInstanceDir()).flatMap(f -> createExperimentInDB(f)).collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Stream<ExperimentRecord> createExperimentInDB(Path f) {
		IntStream.rangeClosed(1, settings.getNumIterations()).forEach(i -> saveExperimentRecord(f, i));
		expRecordRepo.flush();
		return expRecordRepo.findByDone(false).stream();
	}

	private ExperimentRecord saveExperimentRecord(Path name, int i) {
		return expRecordRepo.save(new ExperimentRecord(name, i));
	}

	private Stream<Path> accessInstanceFolder(String extension, String strDir) throws IOException {
		Path dir = FileSystems.getDefault().getPath(strDir);
		if (Files.notExists(dir)) {
			Path currentRelativePath = Paths.get("");
			dir = FileSystems.getDefault().getPath(currentRelativePath.toAbsolutePath().toString() + File.pathSeparator + strDir);
		}
		DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{" + extension + "}");
		return StreamSupport.stream(stream.spliterator(), false);
	}

}
