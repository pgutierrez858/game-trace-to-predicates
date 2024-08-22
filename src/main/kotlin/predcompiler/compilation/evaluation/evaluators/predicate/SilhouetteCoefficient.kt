package predcompiler.compilation.evaluation.evaluators.predicate

import kotlin.math.sqrt

// Function to compute the Euclidean distance between two vectors
fun euclideanDistance(vec1: DoubleArray, vec2: DoubleArray): Double {
    return sqrt(vec1.zip(vec2) { v1, v2 -> (v1 - v2) * (v1 - v2) }.sum())
} // euclideanDistance

// Function to compute the average distance from a point to all other points in the same cluster
fun averageIntraClusterDistance(point: DoubleArray, cluster: List<DoubleArray>): Double {
    val totalDistance = cluster.sumOf { euclideanDistance(point, it) }
    return if (cluster.size > 1) totalDistance / (cluster.size - 1) else 0.0
} // averageIntraClusterDistance

// Function to compute the average distance from a point to all points in another cluster
fun averageInterClusterDistance(point: DoubleArray, otherClusters: List<List<DoubleArray>>): Double {
    return otherClusters.minOfOrNull { cluster ->
        cluster.map { euclideanDistance(point, it) }.average()
    } ?: Double.MAX_VALUE
} // averageInterClusterDistance

// Function to compute the Silhouette Coefficient for a single point
fun silhouetteCoefficientForPoint(
    point: DoubleArray,
    ownCluster: List<DoubleArray>,
    otherClusters: List<List<DoubleArray>>
): Double {
    val a = averageIntraClusterDistance(point, ownCluster)
    val b = averageInterClusterDistance(point, otherClusters)
    return (b - a) / maxOf(a, b)
} // silhouetteCoefficientForPoint

// Function to compute the overall Silhouette Coefficient for the clustering
fun silhouetteCoefficient(
    data: Array<DoubleArray>,
    clusters: IntArray
): Double {
    val clusterMap = data.zip(clusters.asIterable()).groupBy({ it.second }, { it.first })
    if (clusterMap.keys.size <= 1) return -1.0

    val coefficients = data.indices.map { i ->
        val point = data[i]
        val cluster = clusters[i]
        val ownCluster = clusterMap[cluster] ?: emptyList()
        val otherClusters = clusterMap.filterKeys { it != cluster }.values.toList()
        silhouetteCoefficientForPoint(point, ownCluster, otherClusters)
    }
    return coefficients.average()
} // silhouetteCoefficient