package it.polimi.diceH2020.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLoader implements CommandLineRunner{
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private int numFiles=0;
	
	@Autowired
	private Settings settings;
	
	@Autowired
	private Experiment experiment;
	
	@Override
	public void run(String... arg0) throws Exception {
		
		logger.info("working dir ->"+settings.getInstanceDir());
		
		//System.out.println("argomento ->"+arg0[0]);

		DirectoryStream<Path> streamTxt = accessInstanceFolder("txt", settings.getTxtDir());
		streamTxt.forEach(f->experiment.send(f));
		
		
										
		setNumFiles("json", settings.getInstanceDir());		
		DirectoryStream<Path> stream = accessInstanceFolder("json", settings.getInstanceDir());
		stream.forEach(f->experiment.launch(f));

		
	}
	
	private void setNumFiles(String extension, String instanceDir) {
		 try {
			accessInstanceFolder(extension, instanceDir).forEach(f->++numFiles);
			experiment.setTotalAnalysisToExecute(numFiles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private DirectoryStream<Path> accessInstanceFolder(String extension, String strDir) throws IOException{
		
		Path dir = FileSystems.getDefault().getPath(strDir);
 		if (Files.notExists(dir)) {
 			Path currentRelativePath = Paths.get("");	
 			dir = FileSystems.getDefault().getPath(currentRelativePath.toAbsolutePath().toString()+File.pathSeparator+strDir);
		}
 		DirectoryStream<Path> stream = Files.newDirectoryStream( dir, "*.{"+extension+"}" );
 		return stream;
		
	}
	

}
