package predcompiler.compilation.experiments

import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.type
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.util.color.Color
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.scales.guide.LegendType
// import org.jetbrains.kotlinx.kandy.letsplot.settings.Symbol
import predcompiler.compilation.evaluation.evaluators.traceset.evaluateTraceSet
import predcompiler.compilation.evaluation.RealValuation
import predcompiler.compilation.evaluation.evaluators.trace.AbstractTraceEvaluator

fun generateDataframe(predicate: String, tracesData: Collection<List<RealValuation>>, evaluators: Collection<AbstractTraceEvaluator>): AnyFrame {
    // evaluate the traces using an automaton trace evaluator and the predicate
    val evaluationResults = evaluateTraceSet(predicate, tracesData, evaluators)
    // convert the evaluation results to a dataframe for plotting
    val metricRecords = evaluationResults.entries.map { Pair(it.key, it.value) }.toTypedArray()
    return dataFrameOf(*metricRecords)
} // generateDataframe

fun plotSamples(
    df: DataFrame<*>,
    category: DataColumn<Int>,
    plotTitle: String
) {
    // check that dataframe contains the necessary columns
    require(df.containsColumn("waste") && df.containsColumn("reward") && df.containsColumn("progress")
    ) { "The dataframe must contain the columns 'waste', 'reward', and 'progress'." }
    // check that dataframe has as many rows as the categories column
    require(df.rowsCount() == category.size()
    ) { "The dataframe must have the same number of rows as the categories column." }
    // check that the waste, reward and progress columns are numeric
    require(
        df["waste"].type.classifier == Double::class &&
                df["reward"].type.classifier == Double::class &&
                df["progress"].type.classifier == Double::class
    ) { "The 'waste', 'reward', and 'progress' columns must be numeric." }

    // val modelled by columnOf(*Array(df.rowsCount()) { (df[it]["progress"] as Double) >= 1.0 })

    val newCat = category.rename("category")
    df
        .add(newCat)
        //.add(modelled)
        .plot {
            layout {
                title = plotTitle
            }
            points {
                x("waste") { scale = continuous(0.0..1.0) }
                y("reward") { scale = continuous(0.0..1.0) }
                size("progress") { scale = continuous(3.0..5.0) }
                color("category") {
                    scale = categorical(
                        domain = listOf(
                            0, 1, 2
                        )
                    )
                    legend.type = LegendType.DiscreteLegend()
                    legend.name = "Category"
                    legend.breaksLabeled(
                        0 to "Modelled",
                        1 to "Pending",
                        2 to "Rejected"
                    )
                }
                /*color("category") {
                    scale = categorical(
                        0 to Color.GREEN,
                        1 to Color.ORANGE,
                        2 to Color.RED
                    )
                    legend.type = LegendType.DiscreteLegend()
                    legend.name = "Category"
                    legend.breaksLabeled(
                        0 to "Modelled",
                        1 to "Pending",
                        2 to "Rejected"
                    )
                }*/
            }
        }
        .save("${plotTitle}.png")
} // plotSamples