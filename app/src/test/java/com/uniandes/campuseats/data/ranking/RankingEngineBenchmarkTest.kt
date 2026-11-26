package com.uniandes.campuseats.data.ranking

import com.uniandes.campuseats.data.Review
import com.uniandes.campuseats.data.ReviewRatings
import com.uniandes.campuseats.data.restaurants
import java.io.File
import java.lang.management.ManagementFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingEngineBenchmarkTest {

    private val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

    private class Measure(val name: String, val millis: Double, val kilobytes: Double)

    private fun inputs(restaurantCount: Int, reviewCount: Int): RankingInputs {
        val template = restaurants.first()
        val list = (0 until restaurantCount).map { template.copy(id = "r$it", name = "Restaurant $it") }
        val reviews = (0 until reviewCount).map { index ->
            val day = 1 + index % 28
            val month = if (index % 3 == 0) "10" else "11"
            Review(
                id = "rev$index",
                restaurantId = "r${index % restaurantCount}",
                author = "author${index % 12}",
                date = "2026-$month-${day.toString().padStart(2, '0')}",
                ratings = ReviewRatings(1 + index % 5, 1 + (index / 5) % 5, 1 + (index / 25) % 5, 1 + (index / 7) % 5),
                dietaryOptions = emptyList(),
                comment = ""
            )
        }
        val crowding = list.associate { it.id to List(8) { level -> 1 + (level + it.id.length) % 5 } }
        return RankingInputs(list, reviews, crowding, windowCutoff = "2026-11-05")
    }

    private fun allocatedBytes(): Long {
        val ids = threads.allThreadIds
        var total = 0L
        val values = threads.getThreadAllocatedBytes(ids)
        for (i in values.indices) if (values[i] > 0) total += values[i]
        return total
    }

    private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

    private fun measure(name: String, warmup: Int = 15, iterations: Int = 25, block: () -> Unit): Measure {
        repeat(warmup) { block() }
        val times = ArrayList<Double>(iterations)
        val bytes = ArrayList<Double>(iterations)
        repeat(iterations) {
            System.gc()
            val bytesBefore = allocatedBytes()
            val started = System.nanoTime()
            block()
            val elapsed = System.nanoTime() - started
            val bytesAfter = allocatedBytes()
            times.add(elapsed / 1_000_000.0)
            bytes.add((bytesAfter - bytesBefore) / 1024.0)
        }
        return Measure(name, median(times), median(bytes))
    }

    @Test
    fun benchmarkRankingPipeline() {
        val data = inputs(restaurantCount = 200, reviewCount = 10_000)
        val ranked = runBlocking { RankingEngine.compute(data, RankingCriterion.OVERALL, RankingWindow.MONTH) }
        val baselineIds = ranked.map { it.restaurant.id }.reversed()
        val stored = StoredRanking(ranked.map { SnapshotRow(it.restaurant.id, it.score, it.reviewCount) }, computedAt = 1L)

        val results = listOf(
            measure("RankingEngine.computeAll (5 criteria)") {
                runBlocking { RankingEngine.computeAll(data, RankingWindow.MONTH) }
            },
            measure("RankingFingerprint.of") {
                RankingFingerprint.of(data, RankingWindow.MONTH)
            },
            measure("RankingMovement.apply") {
                RankingMovement.apply(ranked, baselineIds)
            },
            measure("RankingSnapshots.restore") {
                RankingSnapshots.restore(RankingCriterion.OVERALL, RankingWindow.MONTH, stored, baselineIds, data.restaurants)
            }
        )

        val builder = StringBuilder()
        builder.append("restaurants=200 reviews=10000 median of 25 runs after 15 warmup runs\n")
        builder.append(String.format("%-40s %12s %16s%n", "operation", "time (ms)", "allocated (KB)"))
        results.forEach { builder.append(String.format("%-40s %12.3f %16.1f%n", it.name, it.millis, it.kilobytes)) }
        println(builder)

        val target = File(System.getProperty("ranking.benchmark.out") ?: "build/ranking-benchmark.txt")
        target.parentFile?.mkdirs()
        target.writeText(builder.toString())

        assertTrue(results.all { it.millis >= 0.0 })
    }
}
