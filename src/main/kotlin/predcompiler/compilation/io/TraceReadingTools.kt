package predcompiler.compilation.io

import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.statevariables.AbstractStateToRobustnessMapping
import java.nio.file.Path

private fun csvDataToStateTrace(csvData: CSVResult): List<HashMap<String, Double>> =
    csvData.rows.map { rowData ->
        check(rowData.size == csvData.headers.size) {
            "Mismatch between number of columns and number of values in a row"
        }
        // for each row, generate a hashmap of the row data
        // using the headers as keys
        rowData.indices.associateTo(
            hashMapOf()
        ) { j -> csvData.headers[j] to rowData[j] }
    } // csvToStateTrace

/**
 * Converts a csv data object to a list of real valuations (a trace).
 * @param csvData the csv data object.
 * @return a list of real valuations from the object's rows.
 */
private fun csvDataToTrace(csvData: CSVResult): List<RealValuation> =
    csvData.rows.map { rowData ->
        check(rowData.size == csvData.headers.size) {
            "Mismatch between number of columns and number of values in a row"
        }
        RealValuation().also { valuation ->
            rowData.indices.forEach { j ->
                valuation.set(csvData.headers[j], rowData[j])
            }
        }
    } // csvDataToTrace

data class TracesReadResult(
    val atomicPredicates: HashSet<String>,
    val traces: List<List<RealValuation>>
) // TracesReadResult

fun readTraces(
    tracesPath: Path,
    mappings: List<AbstractStateToRobustnessMapping> = emptyList()
): TracesReadResult {

    val csvResults = readCSVFilesFromPath(tracesPath)
    // ensure that there is at least a CSV file to process
    if (csvResults.isEmpty()) return TracesReadResult(hashSetOf(), emptyList())

    // ensure that all CSV files read have consistent headers
    val headers = csvResults.first().headers
    check(csvResults.all{ it.headers == headers }) {
        "Mismatch between headers in CSV files"
    }

    // case 1: no mappings provided, read traces as is
    if (mappings.isEmpty()) {
        return TracesReadResult(
            headers.toHashSet(),
            csvResults.map { csvDataToTrace(it) }
        )
    }
    // case 2: mappings provided, read traces as states and map them
    else {
        StateTraceToRobustnessMapper(mappings).also { mapper ->
            return TracesReadResult(
                mappings.map { it.predicateRepresentation }.toHashSet(),
                csvResults.map {
                    mapper.mapStateTraceToRobustness(csvDataToStateTrace(it))
                }
            )
        }
    }
} // readRobustnessTraces
