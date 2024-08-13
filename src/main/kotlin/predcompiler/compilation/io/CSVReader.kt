package predcompiler.compilation.io

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.pathString

data class CSVResult(
    val headers: List<String>,
    val rows: List<List<Double>>
)

/**
 * Reads a CSV file and returns the headers and rows as a [CSVResult] object.
 * @param filePath the path to the CSV file
 */
fun readCSV(filePath: String): CSVResult {

    File(filePath).bufferedReader().use { br ->

        // Read the header line
        val headerLine = br.readLine()
        require(!headerLine.isNullOrEmpty()) { "The file is empty" }

        // Split the header line to get column names
        val headers = headerLine.split(",")

        // Read the rest of the file line by line
        val rowList = mutableListOf<List<Double>>()
        var line: String
        while ((br.readLine().also { line = it }) != null) {
            val values = line.split(",").toTypedArray()
            check(values.size == headers.size) {
                "Mismatch between number of columns (${headers.size}) and number of values in a row (${values.size})"
            }

            // Parse and store the float values in an array
            rowList.add(values.map { it.toDouble() })
        }
        return CSVResult(headers, rowList)
    } // File(filePath).bufferedReader().use
} // readCSV

/**
 * Collects all CSV files from the specified directory and returns a list of their paths.
 * Provided path must be a valid directory.
 */
fun collectCsvFilesFromPath(dir: Path): List<Path> {
    require(Files.exists(dir) && Files.isDirectory(dir)) { "Directory does not exist: $dir" }

    val csvFiles = mutableListOf<Path>()
    Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (file.toString().endsWith(".csv")) {
                csvFiles.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    })
    return csvFiles
} // collectCsvFilesFromPath

fun readCSVFilesFromPath(path: Path): List<CSVResult> {
    return collectCsvFilesFromPath(path).map { readCSV(it.pathString) }
} // readCSVFilesFromPath

private fun <T> arrayToString(array: List<T>): String {
    val sb = StringBuilder()
    sb.append("[")
    for (i in array.indices) {
        sb.append(array[i])
        if (i < array.size - 1) {
            sb.append(", ")
        }
    }
    sb.append("]")
    return sb.toString()
} // arrayToString

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: java CSVReader <csv-file-path>")
        return
    }

    val filePath = args[0]
    readCSV(filePath).also { (headers, rows) ->
        println("Headers: " + arrayToString(headers))
        rows.forEach { row ->
            println("Row: " + arrayToString(row))
        }
    }
} // main
