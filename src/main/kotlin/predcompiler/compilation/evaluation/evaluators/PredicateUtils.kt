package predcompiler.compilation.evaluation.evaluators

/**
 * Counts the number of occurrences of a given search string in a given string.
 * @note Useful for counting the number of occurrences of a given atomic predicate in a trace.
 */
fun countOccurrences(str: String, searchStr: String): Int {
    var count = 0
    var startIndex = 0

    while (startIndex < str.length) {
        val index = str.indexOf(searchStr, startIndex)
        if (index >= 0) {
            count++
            startIndex = index + searchStr.length
        } else {
            break
        }
    }
    return count
} // countOccurrences