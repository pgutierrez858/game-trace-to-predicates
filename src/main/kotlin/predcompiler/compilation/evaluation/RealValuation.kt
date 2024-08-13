package predcompiler.compilation.evaluation
/**
 * An adaptation of the Valuation class from the Rabinizer3 library to enable
 * representations of real-valued atomic predicates (i.e. supporting quantitative
 * semantics instead of purely qualitative ones).
 */
class RealValuation : HashMap<String, Double>() {
    fun set(`var`: String, value: Double): RealValuation {
        this[`var`] = value
        return this
    }

    private var strValuation: String? = null

    override fun toString(): String {
        if (strValuation == null) {
            strValuation = "{"
            var first = true
            for ((key, value) in this) {
                if (first) {
                    strValuation = "$key[$value]"
                    first = false
                } else {
                    strValuation = "$strValuation, $key[$value]"
                }
            }
            strValuation = "$strValuation}"
        }
        return strValuation!!
    }
} // RealValuation
