package it.polimi.diceH2020.launcher.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    
    public static String createTempZip(Map<String,String> inputFiles) {
    	System.out.println("asking for a zip");
	    Path folderPath;
	    String uniqueString = generateUniqueString();
		try {
			folderPath = Files.createTempDirectory(LOCAL_DYNAMIC_FOLDER.toPath(), uniqueString);
		} catch (IOException e) {
			logger.info("Error creating folder for zipping files");
			return "error";
		}
	    for (Map.Entry<String, String> entry : inputFiles.entrySet())
	    {
	    	try {
	    		System.out.println("creating file in "+folderPath+"/"+entry.getKey());
				Files.write(Paths.get(folderPath+"/"+entry.getKey()), Compressor.decompress(entry.getValue()).getBytes(), StandardOpenOption.CREATE_NEW);
			} catch (IOException e) {
				logger.info("error creating files in zip folder");
				return null;
			}
	    }
	    try {
			zipFolder(folderPath.toString(), LOCAL_DYNAMIC_FOLDER.toPath().toString()+"/simulations"+uniqueString+".zip");
		} catch (Exception e) {
			logger.info("Error zipping files");
			return null;
		}
	    System.out.println(LOCAL_DYNAMIC_FOLDER.toPath().toString()+"/simulations"+uniqueString+".zip");
	    return LOCAL_DYNAMIC_FOLDER.getAbsolutePath().toString()+"/simulations"+uniqueString+".zip";
   }
    
    private static  String generateUniqueString() {
		//String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Date dNow = new Date( );
	    SimpleDateFormat ft = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
	    Random random = new Random();
	    String id = ft.format(dNow)+random.nextInt(99999);
	    return id;
	}
    
    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
      }

      static private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
          throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
          addFolderToZip(path, srcFile, zip);
        } else {
          byte[] buf = new byte[1024];
          int len;
          FileInputStream in = new FileInputStream(srcFile);
          zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
          while ((len = in.read(buf)) > 0) {
            zip.write(buf, 0, len);
          }
        }
      }

      static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
          throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
          if (path.equals("")) {
            addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
          } else {
            addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
          }
        }
      }
	
	
	
	
}
