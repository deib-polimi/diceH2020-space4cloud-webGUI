package it.polimi.diceH2020.launcher.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.utility.policy.DeletionPolicy;
import it.polimi.diceH2020.launcher.utility.policy.KeepFiles;


@Component
public class FileUtility {

	private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	@Autowired
	private DeletionPolicy policy;

	
	public boolean delete(File file) {
		return policy.delete(file);
	}
	
	public @Nonnull File provideFile(@CheckForNull String fileName) throws IOException {
		File file = new File(LOCAL_DYNAMIC_FOLDER, fileName);
		policy.markForDeletion(file);
		return file;
	}
	

	public @Nonnull File provideTemporaryFile(@CheckForNull String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix, LOCAL_DYNAMIC_FOLDER);
		if (policy == null) {
			policy = new KeepFiles();
		}
		policy.markForDeletion(file);
		return file;
	}

	public void writeContentToFile(@Nonnull String content, @Nonnull File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
	}

	public void createWorkingDir() throws IOException {
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER + " created.");
	}

	public void destroyWorkingDir() throws IOException{
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		if (Files.deleteIfExists(folder)) {
			logger.info(LOCAL_DYNAMIC_FOLDER+ " deleted.");
		}
	}

	public boolean delete(List<File> pFiles) {
		return pFiles.stream().map(f -> delete(f)).allMatch(r -> r);
	}

	public InputStream getFileAsStream(String fileName) {
		
		Path filePath = new File(LOCAL_DYNAMIC_FOLDER, fileName).toPath();
		if (Files.exists(filePath)) {
			try {
				return Files.newInputStream(filePath);
			} catch (IOException e) {
				return null;
			}
		}
		else return null;					
	}
	
	
	
	
}
