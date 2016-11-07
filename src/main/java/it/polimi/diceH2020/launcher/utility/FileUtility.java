/*
Copyright 2016 Eugenio Gianniti
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileUtility {

	private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
	private static final File LOCAL_INPUT_FOLDER = new File("Input");
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
			tmp.getParentFile().mkdirs();
			tempFiles.add(tmp);
			Files.write(tmp.toPath(),
					(entry.getValue()).getBytes(),
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
	
	public String createInputSubFolder() throws Exception{
		return createNewFolder(LOCAL_INPUT_FOLDER.getCanonicalPath().toString());
	}
	
	private synchronized String createNewFolder(String inputAbsolutePath) throws Exception{
		String newFolderName = generateUniqueString();
		String newFolderAbsolutePath = inputAbsolutePath+File.separator+newFolderName;
		File destFolder = new File(newFolderAbsolutePath);
		if(!destFolder.exists()){
			destFolder.mkdirs();
			return newFolderAbsolutePath+File.separator;
		}
		for(int i=1;i<1000;i++){
			destFolder = new File(newFolderAbsolutePath+String.valueOf(i));
			if(!destFolder.exists()){
				destFolder.mkdirs();
				return newFolderAbsolutePath+String.valueOf(i)+File.separator;
			}
		}
		throw new Exception();
	}
	
	public String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
		SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
		Random random = new Random();
		String id = ft.format(dNow)+random.nextInt(99999);
		return id;
	}
	
	public void copyFile(String srcPath, String destPath) throws IOException{
		Files.copy(Paths.get(srcPath), Paths.get(destPath));
	}
	
}
