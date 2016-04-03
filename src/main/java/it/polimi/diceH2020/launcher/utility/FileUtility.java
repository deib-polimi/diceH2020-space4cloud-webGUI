package it.polimi.diceH2020.launcher.utility;

import it.polimi.diceH2020.launcher.utility.policy.DeletionPolicy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


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
		policy.markForDeletion(file);
		return file;
	}

	public void createWorkingDir() throws IOException {
		Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER + " created.");
	}

	public boolean delete(List<File> pFiles) {
		return pFiles.stream().map(this::delete).allMatch(r -> r);
	}

	public @Nonnull File createTempZip(@Nonnull Map<String, String> inputFiles) throws IOException {
		File folder = Files.createTempDirectory(LOCAL_DYNAMIC_FOLDER.toPath(), null).toFile();
		policy.markForDeletion(folder);

		List<File> tempFiles = new LinkedList<>();
		for (Map.Entry<String, String> entry : inputFiles.entrySet()) {
			File tmp = new File(folder, entry.getKey());
			tempFiles.add(tmp);
			Files.write(tmp.toPath(),
					Compressor.originalDecompress(entry.getValue()).getBytes(),
					StandardOpenOption.CREATE_NEW);
		}

		String fileName = String.format("%s.zip", folder.getName());
		File zip = new File(LOCAL_DYNAMIC_FOLDER, fileName);
		policy.markForDeletion(zip);
		zipFolder(folder, zip);

		tempFiles.forEach(policy::delete);
		policy.delete(folder);

		return zip;
	}

	private void zipFolder(File srcFolder, File destZipFile) throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
				new FileOutputStream(destZipFile)))) {
			addFolderToZip(null, srcFolder, zip);
		}
	}

	private void addFolderToZip(File path, File srcFolder, ZipOutputStream zip) throws IOException {
		for (String fileName : srcFolder.list()) {
			addFileToZip(path == null ? srcFolder : new File(path, srcFolder.getName()),
					new File(srcFolder, fileName), zip);
		}
	}

	private void addFileToZip(File path, File srcFile, ZipOutputStream zip) throws IOException {
		if (srcFile.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(srcFile))) {
				zip.putNextEntry(new ZipEntry(makeEntry(path, srcFile)));
				while ((len = in.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
			}
		}
	}

	private ZipEntry makeEntry(File path, File file) {
		String cleanPath = new File(path, file.getName()).toString()
				.replace(LOCAL_DYNAMIC_FOLDER.toString(), "");
		return new ZipEntry(cleanPath);
	}

}
