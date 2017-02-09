/*
Copyright 2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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
package it.polimi.diceH2020.launcher;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileService {

	public List<Map<String, String>> getFiles (List<String> folders, String extension) throws IOException {
		List<Map<String, String>> fileList = new ArrayList<>();

		for (String folder: folders) {
			for (String file: listFile(folder, extension)) {
				Map<String, String> fileInfo = new HashMap<> ();
				fileInfo.put ("name", Paths.get(file).getFileName().toString());
				fileInfo.put ("content", new String(Files.readAllBytes(Paths.get(folder, file))));
				fileList.add(fileInfo);
			}
		}

		return fileList;
	}

	private String[] listFile(String folder, String ext) {
		GenericExtFilter filter = new GenericExtFilter(ext);
		File dirInput = new File(folder);
		return dirInput.list(filter);
	}

	public static class GenericExtFilter implements FilenameFilter {

		private String ext;

		GenericExtFilter(String ext) {
			this.ext = ext;
		}

		public boolean accept(File dir, String name) {
			return (name.endsWith(ext));
		}
	}

}
