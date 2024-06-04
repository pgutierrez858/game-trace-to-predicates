package predcompiler.compilation.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import predcompiler.compilation.io.CSVReader.CSVResult;

public class GameStateTraceFileReader {
	private Path examplesPath;
	private Path counterExamplesPath;
	private List<List<HashMap<String, Float>>> exampleTraces;
	private List<List<HashMap<String, Float>>> counterExampleTraces;
	private HashSet<String> variableNames;
	private boolean filesRead;

	public GameStateTraceFileReader(String traceFolderPath) {
		variableNames = new HashSet<>();
		filesRead = false;

		// Define paths for examples and counter-examples folders
		examplesPath = Paths.get(traceFolderPath, "examples");
		counterExamplesPath = Paths.get(traceFolderPath, "counter-examples");
	} // GameStateTraceFileReader

	public List<List<HashMap<String, Float>>> getExampleTraces() {
		return this.exampleTraces;
	}

	public List<List<HashMap<String, Float>>> getCounterExampleTraces() {
		return this.counterExampleTraces;
	}

	public HashSet<String> getVariableNames() {
		return this.variableNames;
	}

	public void readTraces() throws IOException {

		if (filesRead)
			return;

		filesRead = true;
		List<Path> exampleFiles = new ArrayList<Path>();
		List<Path> counterExampleFiles = new ArrayList<Path>();

		collectCsvFiles(examplesPath, exampleFiles);
		collectCsvFiles(counterExamplesPath, counterExampleFiles);

		exampleTraces = new ArrayList<>();
		counterExampleTraces = new ArrayList<>();

		for (var f : exampleFiles) {
			List<HashMap<String, Float>> trace = csvToStateTrace(f);
			if (trace != null)
				exampleTraces.add(trace);
		}

		for (var f : counterExampleFiles) {
			List<HashMap<String, Float>> trace = csvToStateTrace(f);
			if (trace != null)
				counterExampleTraces.add(trace);
		}
	} // readTraces

	/**
	 * Reads the csv file at given path and attempts to generate a state trace from
	 * its data.
	 */
	private List<HashMap<String, Float>> csvToStateTrace(Path csvPath) {
		CSVResult csvData;
		try {
			csvData = CSVReader.readCSV(csvPath.toString());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String[] headers = csvData.headers;
		variableNames.addAll(Arrays.asList(headers));

		List<HashMap<String, Float>> trace = new ArrayList<>();

		for (int i = 0; i < csvData.rows.length; i++) {
			trace.add(new HashMap<String, Float>());
			float[] rowData = csvData.rows[i];
			if (rowData.length != headers.length)
				return null;
			for (int j = 0; j < rowData.length; j++) {
				trace.get(i).put(headers[j], rowData[j]);
			}
		}
		return trace;
	} // csvToTrace

	private void collectCsvFiles(Path dir, List<Path> csvFiles) throws IOException {
		if (Files.exists(dir) && Files.isDirectory(dir)) {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".csv")) {
						csvFiles.add(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			System.err.println("Directory does not exist: " + dir);
		}
	} // collectCsvFiles
} // GameStateTraceFileReader
