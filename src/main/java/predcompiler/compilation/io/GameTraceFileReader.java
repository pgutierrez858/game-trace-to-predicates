package predcompiler.compilation.io;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import predcompiler.compilation.evaluation.RealValuation;
import predcompiler.compilation.io.CSVReader.CSVResult;
import rabinizer.bdd.BDDForVariables;

public class GameTraceFileReader {

	private Path examplesPath;
	private Path counterExamplesPath;
	private List<RealValuation[]> exampleTraces;
	private List<RealValuation[]> counterExampleTraces;
	private HashSet<String> atomicPredicates;
	private boolean filesRead;

	public GameTraceFileReader(String traceFolderPath) {
		atomicPredicates = new HashSet<>();
		filesRead = false;

		// Define paths for examples and counter-examples folders
		examplesPath = Paths.get(traceFolderPath, "examples");
		counterExamplesPath = Paths.get(traceFolderPath, "counter-examples");
	}

	public List<RealValuation[]> getExampleTraces() {
		return this.exampleTraces;
	}

	public List<RealValuation[]> getCounterExampleTraces() {
		return this.counterExampleTraces;
	}

	public HashSet<String> getAtomicPredicates() {
		return this.atomicPredicates;
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
			RealValuation[] trace = csvToTrace(f);
			if (trace != null)
				exampleTraces.add(trace);
		}

		for (var f : counterExampleFiles) {
			RealValuation[] trace = csvToTrace(f);
			if (trace != null)
				counterExampleTraces.add(trace);
		}
	} // readTraces

	/**
	 * Reads the csv file at given path and attempts to generate a real valuation
	 * from its data.
	 */
	private RealValuation[] csvToTrace(Path csvPath) {
		CSVResult csvData;
		try {
			csvData = CSVReader.readCSV(csvPath.toString());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String[] headers = csvData.headers;
		atomicPredicates.addAll(Arrays.asList(headers));

		RealValuation[] trace = new RealValuation[csvData.rows.length];

		for (int i = 0; i < trace.length; i++) {
			trace[i] = new RealValuation();
			float[] rowData = csvData.rows[i];
			if (rowData.length != headers.length)
				return null;
			for (int j = 0; j < rowData.length; j++) {
				int vId = BDDForVariables.bijectionIdAtom.id(headers[j]);
				trace[i].set(vId, rowData[j]);
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
}
