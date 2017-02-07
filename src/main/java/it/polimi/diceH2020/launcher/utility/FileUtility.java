/*
Copyright 2016-2017 Eugenio Gianniti
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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileUtility {

    private static final File LOCAL_DYNAMIC_FOLDER = new File("TempWorkingDir");
    private static final File LOCAL_INPUT_FOLDER = new File("Input");
    private final Logger logger = Logger.getLogger(getClass ());

    private Random random = new Random();

    @Autowired
    private DeletionPolicy policy;

    public boolean delete (File file) {
        return policy.delete(file);
    }

    public @NotNull File provideFile (@NotNull String fileName) {
        File file = new File(LOCAL_DYNAMIC_FOLDER, fileName);
        policy.markForDeletion(file);
        return file;
    }

    public @NotNull File provideTemporaryFile(@NotNull String prefix, @Nullable String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix, LOCAL_DYNAMIC_FOLDER);
        policy.markForDeletion(file);
        return file;
    }

    public void createWorkingDir() throws IOException {
        Path folder = LOCAL_DYNAMIC_FOLDER.toPath();
        Files.createDirectories(folder);
        logger.info(LOCAL_DYNAMIC_FOLDER + " created.");
    }

    public boolean delete(@NotNull List<File> pFiles) {
        return pFiles.stream().map(this::delete).allMatch(r -> r);
    }

    public @NotNull File createTempZip(@NotNull Map<String, String> inputFiles) throws IOException {
        File folder = Files.createTempDirectory(LOCAL_DYNAMIC_FOLDER.toPath(), null).toFile();
        policy.markForDeletion(folder);

        List<File> tempFiles = new LinkedList<>();
        for (Map.Entry<String, String> entry : inputFiles.entrySet()) {
            File tmp = new File(folder, entry.getKey());
            tmp.getParentFile().mkdirs();
            tempFiles.add(tmp);
            Files.write(tmp.toPath(),
                    Compressor.decompress(entry.getValue()).getBytes(),
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

    private void zipFolder(@NotNull File srcFolder, @NotNull File destZipFile) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(destZipFile)))) {
            addFolderToZip(null, srcFolder, zip);
        }
    }

    private void addFolderToZip(@Nullable File path, @NotNull File srcFolder,
                                @NotNull ZipOutputStream zip) throws IOException {
        String[] files = srcFolder.list ();
        if (files != null) {
            for (String fileName: files) {
                addFileToZip (path == null ? srcFolder : new File (path, srcFolder.getName ()),
                        new File (srcFolder, fileName), zip);
            }
        }
    }

    private void addFileToZip(@NotNull File path, @NotNull File srcFile,
                              @NotNull ZipOutputStream zip) throws IOException {
        if (srcFile.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(srcFile))) {
                zip.putNextEntry(makeEntry(path, srcFile));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }

    private ZipEntry makeEntry(@NotNull File path, @NotNull File file) {
        String cleanPath = new File(path, file.getName()).toString()
                .replace(LOCAL_DYNAMIC_FOLDER.toString(), "");
        return new ZipEntry(cleanPath);
    }

    public File createInputSubFolder() throws FileNameClashException {
        return createNewFolder(LOCAL_INPUT_FOLDER);
    }

    public File createInputFile(@NotNull File folder, @NotNull String fileName) throws FileNameClashException {
        File requestedFile = new File(folder, fileName);
        if (requestedFile.exists()) {
            throw new FileNameClashException(
                    String.format("'%s' already exists in '%s'", fileName, folder.getPath()));
        }
        return requestedFile;
    }

    private synchronized File createNewFolder(@NotNull File parentFolder) throws FileNameClashException {
        final String baseSubFolderName = generateUniqueString();
        File destFolder = new File(parentFolder, baseSubFolderName);

        for (int i = 0; destFolder.exists () && i < 100; ++i) {
            destFolder = new File (parentFolder, String.format ("%s_%d", baseSubFolderName, i));
        }

        if (destFolder.exists()) {
            throw new FileNameClashException (String.format ("Cannot create folder '%s'", baseSubFolderName));
        } else {
            destFolder.mkdirs ();
        }

        return destFolder;
    }

    public @NotNull String generateUniqueString() {
        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat ("Edd-MM-yyyy_HH-mm-ss");
        return String.format ("%s-%d", simpleDateFormat.format(now), random.nextInt (99999));
    }

    public void writeContentToFile(@NotNull String content, @NotNull File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

}
