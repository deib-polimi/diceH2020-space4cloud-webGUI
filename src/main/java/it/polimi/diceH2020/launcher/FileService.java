package it.polimi.diceH2020.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codepoetics.protonpack.Indexed;
import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;

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
