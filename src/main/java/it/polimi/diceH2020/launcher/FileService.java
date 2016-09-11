/*
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

import com.codepoetics.protonpack.Indexed;
import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class FileService {

	@Autowired
	private Settings settings;

	public List<Solution> getBaseSolutions() {
		Stream<Path> strm = getBaseSolutionsPath();
		return strm == null ? new ArrayList<>() : strm.map(p -> getObjectFromPath(p, Solution.class)).collect(Collectors.toList());
	}

	public Stream<Path> getBaseSolutionsPath() {
		String strDir = settings.getSolInstanceDir();
		Path dir = FileSystems.getDefault().getPath(strDir);
		if (Files.notExists(dir)) {
			Path currentRelativePath = Paths.get("");
			dir = FileSystems.getDefault().getPath(currentRelativePath.toAbsolutePath().toString() + File.pathSeparator + strDir);
		}
		DirectoryStream<Path> stream;
		try {
			stream = Files.newDirectoryStream(dir, "*.{json}");
			return StreamSupport.stream(stream.spliterator(), false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public List<Indexed<Path>> getListFileWithIndex(){
		return StreamUtils.zipWithIndex(getBaseSolutionsPath()).collect(Collectors.toList());

	}

	private <T> T getObjectFromPath(Path Path, Class<T> cls) {
		String serialized;
		try {
			ObjectMapper mapper = new ObjectMapper();
			serialized = new String(Files.readAllBytes(Path));
			T data = mapper.readValue(serialized, cls);
			return data;
		} catch (IOException e) {
			return null;
		}
	}
}
