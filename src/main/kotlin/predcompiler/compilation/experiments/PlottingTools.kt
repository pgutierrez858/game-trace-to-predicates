package predcompiler.compilation.experiments

import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.type
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.util.color.Color
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.scales.guide.LegendType
import org.jetbrains.kotlinx.kandy.letsplot.settings.Symbol

fun plotSamples(
    df: DataFrame<*>,
    category: DataColumn<Boolean>,
    trueText: String,
    falseText: String,
    plotTitle: String
) {
    // check that dataframe contains the necessary columns
    assert(df.containsColumn("waste") && df.containsColumn("reward") && df.containsColumn("progress")
    ) { "The dataframe must contain the columns 'waste', 'reward', and 'progress'." }
    // check that dataframe has as many rows as the categories column
    assert(df.rowsCount() == category.size()
    ) { "The dataframe must have the same number of rows as the categories column." }
    // check that the waste, reward and progress columns are numeric
    assert(
        df["waste"].type.classifier == Double::class &&
                df["reward"].type.classifier == Double::class &&
                df["progress"].type.classifier == Double::class
    ) { "The 'waste', 'reward', and 'progress' columns must be numeric." }

    val modelled by columnOf(*Array(df.rowsCount()) { (df[it]["progress"] as Double) >= 1.0 })

    val newCat = category.rename("category")
    df
        .add(newCat)
        .add(modelled)
        .plot {
            points {
                x("waste") { scale = continuous(0.0..1.0) }
                y("reward") { scale = continuous(0.0..1.0) }
                size("progress") { scale = continuous(2.0..6.0) }
                color("category") {
                    scale = categorical(
                        true to Color.YELLOW, false to Color.BLUE
                    )
                    legend.type = LegendType.DiscreteLegend()
                    legend.name = "Category"
                    legend.breaksLabeled(true to trueText, false to falseText)
                }
                symbol("modelled") {
                    scale = categorical(
                        true to Symbol.BULLET,
                        false to Symbol.CROSS
                    )
                }
            }
        }
        .save("${plotTitle}.png")
} // plotSamples