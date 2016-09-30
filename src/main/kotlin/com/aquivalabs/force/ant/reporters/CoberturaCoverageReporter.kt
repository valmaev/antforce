package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import com.aquivalabs.force.ant.reporters.cobertura.Classes
import com.aquivalabs.force.ant.reporters.cobertura.CoberturaReportRoot
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.DeployResult
import java.io.File

class CoberturaCoverageReporter(
    var outputFile: File,
    var projectRootPath: String? = null) : Reporter<File> {

    override fun createReport(deployResult: DeployResult): File {
        val report = createCoberturaReport(deployResult)
        outputFile.writeText(report.toString())
        return outputFile
    }

    fun createCoberturaReport(deployResult: DeployResult): CoberturaReportRoot {
        val testResult = deployResult.details.runTestResult
        val coverageTypes = testResult.codeCoverage.groupBy { it.type ?: "" }

        val report = CoberturaReportRoot()
        report.coverage(
            `lines-covered` = testResult.totalNumLocationsCovered,
            `lines-valid` = testResult.totalNumLocations,
            `line-rate` = testResult.totalCoverage) {

            sources {
                source { +projectRootPath.orEmpty() }
            }
            packages {
                coverageTypes.forEach { coverageType ->
                    val coverageForType = coverageType.value
                        .map { Pair(it.numLocationsCovered, it.numLocations) }
                        .reduce { x, y -> x + y }
                    val lineRate = coverage(coverageForType.first, coverageForType.second)

                    `package`(name = coverageType.key, `line-rate` = lineRate) {
                        classes {
                            coverageType.value.forEach {
                                createClassTags(it)
                            }
                        }
                    }
                }
            }
        }
        return report
    }

    private fun Classes.createClassTags(result: CodeCoverageResult) {
        `class`(
            name = result.qualifiedName,
            filename = result.classFileName,
            `line-rate` = result.coverage) {

            methods()
            lines {
                val locations = sortedMapOf<Int, Int>()
                result.locationsNotCovered?.forEach { locations[it.line] = it.numExecutions }

                var currentLine = 0
                while (locations.size != result.numLocations) {
                    if (locations.contains(++currentLine))
                        continue
                    locations[currentLine] = 1
                }

                locations.forEach {
                    line(number = it.key, hits = it.value)
                }
            }
        }
    }
}

private infix operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>): Pair<Int, Int> =
    Pair(this.first + other.first, this.second + other.second)